package edu.fau.recsys.grading;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.fau.recsys.catalog.MovieCatalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A user's grades: upsert, list with titles, delete, and the plain movieId → score map the engine needs. */
@Service
public class GradeService {

	public static final int MIN_SCORE = 1;

	public static final int MAX_SCORE = 10;

	private static final String VIEW_SQL = """
			SELECT g.movie_id, m.display_title, m.year, g.score, g.updated_at
			  FROM grade g JOIN movie m ON m.id = g.movie_id
			 WHERE g.user_id = ?
			""";

	private static final RowMapper<GradeView> VIEW_MAPPER = (rs, rowNum) -> new GradeView(rs.getInt("movie_id"),
			rs.getString("display_title"), rs.getObject("year", Integer.class), rs.getInt("score"),
			rs.getObject("updated_at", OffsetDateTime.class).toInstant());

	private final GradeRepository gradeRepository;

	private final MovieCatalog movieCatalog;

	private final JdbcTemplate jdbcTemplate;

	GradeService(GradeRepository gradeRepository, MovieCatalog movieCatalog, JdbcTemplate jdbcTemplate) {
		this.gradeRepository = gradeRepository;
		this.movieCatalog = movieCatalog;
		this.jdbcTemplate = jdbcTemplate;
	}

	/** @throws UnknownMovieException if the movie is not in the catalog */
	@Transactional
	public GradeView upsert(long userId, int movieId, int score) {
		if (score < MIN_SCORE || score > MAX_SCORE) {
			throw new IllegalArgumentException("Score must be between 1 and 10: " + score);
		}
		if (!movieCatalog.exists(movieId)) {
			throw new UnknownMovieException(movieId);
		}
		gradeRepository.upsert(userId, movieId, score);
		return jdbcTemplate.queryForObject(VIEW_SQL + " AND g.movie_id = ?", VIEW_MAPPER, userId, movieId);
	}

	/** Most recently graded first. */
	@Transactional(readOnly = true)
	public List<GradeView> list(long userId) {
		return jdbcTemplate.query(VIEW_SQL + " ORDER BY g.updated_at DESC, g.movie_id", VIEW_MAPPER, userId);
	}

	/** Deleting a grade that does not exist is not an error. */
	@Transactional
	public void delete(long userId, int movieId) {
		gradeRepository.deleteGrade(userId, movieId);
	}

	/** movieId → score, in movieId order: the input for {@code UserRatings}. */
	@Transactional(readOnly = true)
	public Map<Integer, Double> scores(long userId) {
		Map<Integer, Double> scores = new TreeMap<>();
		for (Grade grade : gradeRepository.findByUserIdOrderByMovieId(userId)) {
			scores.put(grade.getMovieId(), (double) grade.getScore());
		}
		return scores;
	}
}
