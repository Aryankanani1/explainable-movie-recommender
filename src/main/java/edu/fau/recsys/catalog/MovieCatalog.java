package edu.fau.recsys.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Database reads of the catalog for other packages. Entities stay inside these transactions. */
@Service
public class MovieCatalog {

	/** Most-rated first; the ID makes the order total, so pages never overlap or skip rows. */
	private static final Sort ORDER = Sort.by(Sort.Order.desc("ratingCount"), Sort.Order.asc("id"));

	static final int DETAIL_TOP_TAGS = 5;

	private final MovieRepository movieRepository;

	private final JdbcTemplate jdbcTemplate;

	MovieCatalog(MovieRepository movieRepository, JdbcTemplate jdbcTemplate) {
		this.movieRepository = movieRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public Page<MovieSummary> search(String query, int page, int size) {
		return movieRepository.findByDisplayTitleContainingIgnoreCase(query.strip(), PageRequest.of(page, size, ORDER))
			.map(MovieSummary::from);
	}

	@Transactional(readOnly = true)
	public boolean exists(int movieId) {
		return movieRepository.existsById(movieId);
	}

	/** The movie with its genres and its most-applied tags (most applications first, then alphabetical). */
	@Transactional(readOnly = true)
	public Optional<MovieDetail> detail(int movieId) {
		return movieRepository.findById(movieId).map(movie -> {
			MovieSummary summary = MovieSummary.from(movie);
			List<MovieDetail.TagCount> topTags = jdbcTemplate.query(
					"SELECT tag, count FROM movie_tag WHERE movie_id = ? ORDER BY count DESC, tag LIMIT ?",
					(rs, rowNum) -> new MovieDetail.TagCount(rs.getString("tag"), rs.getInt("count")), movieId,
					DETAIL_TOP_TAGS);
			return new MovieDetail(summary.id(), summary.title(), summary.year(), summary.genres(), topTags,
					summary.ratingCount(), summary.ratingMean());
		});
	}

	/** Every genre with its movie count, in genre ID (catalog) order. */
	@Transactional(readOnly = true)
	public List<GenreCount> genres() {
		return jdbcTemplate.query("""
				SELECT g.name, count(mg.movie_id) AS movies
				  FROM genre g LEFT JOIN movie_genre mg ON mg.genre_id = g.id
				 GROUP BY g.id, g.name
				 ORDER BY g.id
				""", (rs, rowNum) -> new GenreCount(rs.getString("name"), rs.getInt("movies")));
	}
}
