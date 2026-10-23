package edu.fau.recsys.catalog;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.fau.recsys.dataset.MovieLensFiles;
import edu.fau.recsys.dataset.MovieLensMovie;
import edu.fau.recsys.dataset.MovieLensRating;
import edu.fau.recsys.dataset.TagCounts;
import edu.fau.recsys.infra.RecsysProperties;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Loads MovieLens into PostgreSQL on first start. Skips when {@code movie} already has rows.
 * <ul>
 *   <li>Movies, genres and links: JPA, flushed and cleared every {@value #JPA_BATCH_SIZE} movies.</li>
 *   <li>Tags and ratings: {@link JdbcTemplate} batch inserts.</li>
 *   <li>One transaction per file; the ratings transaction ends by filling {@code rating_count}/{@code rating_mean}.</li>
 * </ul>
 */
@Component
class MovieLensImporter implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MovieLensImporter.class);

	/** Matches {@code hibernate.jdbc.batch_size}. */
	private static final int JPA_BATCH_SIZE = 500;

	private static final int JDBC_BATCH_SIZE = 1000;

	private final RecsysProperties properties;

	private final MovieRepository movieRepository;

	private final EntityManager entityManager;

	private final JdbcTemplate jdbcTemplate;

	private final TransactionTemplate transactionTemplate;

	MovieLensImporter(RecsysProperties properties, MovieRepository movieRepository, EntityManager entityManager,
			JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
		this.properties = properties;
		this.movieRepository = movieRepository;
		this.entityManager = entityManager;
		this.jdbcTemplate = jdbcTemplate;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.importSettings().enabled()) {
			log.info("MovieLens import disabled (recsys.import.enabled=false)");
			return;
		}
		long existing = movieRepository.count();
		if (existing > 0) {
			log.info("MovieLens import skipped: catalog already has {} movies", existing);
			return;
		}
		Path dir = properties.dataDir();
		if (!Files.isDirectory(dir)) {
			throw new IllegalStateException("MovieLens data not found at " + dir.toAbsolutePath()
					+ "; download ml-latest-small as described in docs/DATASET.md");
		}

		MovieLensFiles files = new MovieLensFiles(dir);
		long start = System.nanoTime();
		MovieImport movies = transactionTemplate.execute(status -> importMovies(files));
		TagCounts tags = transactionTemplate.execute(status -> importTags(files));
		Integer ratings = transactionTemplate.execute(status -> importRatings(files));
		double seconds = (System.nanoTime() - start) / 1e9;

		log.info("Imported {} movies, {} genres, {} tag applications ({} movie-tag pairs), {} ratings in {} s",
				movies.movies(), movies.genres(), tags.applications(), tags.pairs(), ratings,
				String.format(Locale.ROOT, "%.1f", seconds));
	}

	private MovieImport importMovies(MovieLensFiles files) {
		Map<Integer, Integer> tmdbIds = new HashMap<>();
		files.forEachLink(link -> {
			if (link.tmdbId() != null) {
				tmdbIds.put(link.movieId(), link.tmdbId());
			}
		});
		List<MovieLensMovie> rows = new ArrayList<>();
		files.forEachMovie(rows::add);

		// Built from the data, not the README's list (the file has IMAX, for one).
		SortedSet<String> genreNames = new TreeSet<>();
		for (MovieLensMovie row : rows) {
			genreNames.addAll(row.genres());
		}
		Map<String, Integer> genreIds = new HashMap<>();
		int nextGenreId = 1;
		for (String name : genreNames) {
			entityManager.persist(new Genre(nextGenreId, name));
			genreIds.put(name, nextGenreId);
			nextGenreId++;
		}

		int persisted = 0;
		for (MovieLensMovie row : rows) {
			Movie movie = new Movie(row.movieId(), row.rawTitle(), row.displayTitle(), row.year(),
					tmdbIds.get(row.movieId()));
			for (String genre : row.genres()) {
				// After clear() the Genre instances are detached; a reference proxy needs no SELECT.
				movie.addGenre(entityManager.getReference(Genre.class, genreIds.get(genre)));
			}
			entityManager.persist(movie);
			persisted++;
			if (persisted % JPA_BATCH_SIZE == 0) {
				entityManager.flush();
				entityManager.clear();
			}
		}
		entityManager.flush();
		entityManager.clear();
		return new MovieImport(rows.size(), genreNames.size());
	}

	private TagCounts importTags(MovieLensFiles files) {
		TagCounts tags = files.tagCounts();
		List<Object[]> rows = new ArrayList<>(tags.pairs());
		for (Map.Entry<Integer, SortedMap<String, Integer>> movie : tags.byMovie().entrySet()) {
			for (Map.Entry<String, Integer> tag : movie.getValue().entrySet()) {
				rows.add(new Object[] { movie.getKey(), tag.getKey(), tag.getValue() });
			}
		}
		jdbcTemplate.batchUpdate("INSERT INTO movie_tag (movie_id, tag, count) VALUES (?, ?, ?)", rows);
		return tags;
	}

	private int importRatings(MovieLensFiles files) {
		List<MovieLensRating> ratings = new ArrayList<>();
		files.forEachRating(ratings::add);
		jdbcTemplate.batchUpdate("INSERT INTO ml_rating (user_id, movie_id, rating, rated_at) VALUES (?, ?, ?, ?)",
				ratings, JDBC_BATCH_SIZE, (ps, rating) -> {
					ps.setInt(1, rating.userId());
					ps.setInt(2, rating.movieId());
					ps.setBigDecimal(3, BigDecimal.valueOf(rating.rating()));
					ps.setObject(4, OffsetDateTime.ofInstant(Instant.ofEpochSecond(rating.timestamp()), ZoneOffset.UTC));
				});
		jdbcTemplate.update("""
				UPDATE movie m
				   SET rating_count = s.rating_count,
				       rating_mean  = s.rating_mean
				  FROM (SELECT movie_id, count(*) AS rating_count, round(avg(rating), 2) AS rating_mean
				          FROM ml_rating
				         GROUP BY movie_id) s
				 WHERE m.id = s.movie_id
				""");
		return ratings.size();
	}

	private record MovieImport(int movies, int genres) {
	}
}
