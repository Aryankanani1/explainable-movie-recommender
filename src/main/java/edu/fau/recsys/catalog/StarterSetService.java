package edu.fau.recsys.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.fau.recsys.engine.CatalogSnapshot;
import edu.fau.recsys.engine.StarterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The onboarding starter set: candidates (mean rating ≥ 3.5) come from the database, genres and rating counts from the
 * in-memory snapshot, and the greedy pick from {@link StarterSet}. The catalog never changes at run time, so each size
 * is computed once.
 */
@Service
public class StarterSetService {

	private static final Logger log = LoggerFactory.getLogger(StarterSetService.class);

	private final JdbcTemplate jdbcTemplate;

	private final MovieRepository movieRepository;

	private final CatalogSnapshotHolder catalogHolder;

	private final Map<Integer, List<MovieSummary>> bySize = new ConcurrentHashMap<>();

	StarterSetService(JdbcTemplate jdbcTemplate, MovieRepository movieRepository, CatalogSnapshotHolder catalogHolder) {
		this.jdbcTemplate = jdbcTemplate;
		this.movieRepository = movieRepository;
		this.catalogHolder = catalogHolder;
	}

	@Transactional(readOnly = true)
	public List<MovieSummary> starterSet(int size) {
		List<MovieSummary> cached = bySize.get(size);
		if (cached != null) {
			return cached;
		}
		CatalogSnapshot catalog = catalogHolder.get();
		List<StarterSet.Candidate> candidates = new ArrayList<>();
		for (int movieId : jdbcTemplate.queryForList("SELECT id FROM movie WHERE rating_mean >= ? ORDER BY id",
				Integer.class, StarterSet.MIN_MEAN)) {
			int idx = catalog.indexOf(movieId);
			candidates.add(new StarterSet.Candidate(movieId, catalog.genres(idx), catalog.ratingCount(idx)));
		}
		List<Integer> picked = StarterSet.select(StarterSet.mostRated(candidates, StarterSet.POOL_SIZE), size);

		Map<Integer, MovieSummary> summaries = new HashMap<>();
		for (Movie movie : movieRepository.findAllById(picked)) {
			summaries.put(movie.getId(), MovieSummary.from(movie));
		}
		List<MovieSummary> starter = new ArrayList<>();
		Set<String> covered = new HashSet<>();
		for (int movieId : picked) {
			starter.add(summaries.get(movieId));
			covered.addAll(summaries.get(movieId).genres());
		}
		log.info("Starter set: {} films covering {} of {} genres", starter.size(), covered.size(),
				catalog.genreNames().size());
		List<MovieSummary> result = List.copyOf(starter);
		bySize.put(size, result);
		return result;
	}
}
