package edu.fau.recsys.grading;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface GradeRepository extends JpaRepository<Grade, Long> {

	List<Grade> findByUserIdOrderByMovieId(long userId);

	/** Insert or update in one statement, so two concurrent PUTs for the same movie can't collide. */
	@Modifying
	@Query(value = """
			INSERT INTO grade (user_id, movie_id, score, updated_at)
			VALUES (:userId, :movieId, :score, now())
			ON CONFLICT (user_id, movie_id) DO UPDATE SET score = EXCLUDED.score, updated_at = EXCLUDED.updated_at
			""", nativeQuery = true)
	void upsert(long userId, int movieId, int score);

	@Modifying
	@Query("DELETE FROM Grade g WHERE g.userId = :userId AND g.movieId = :movieId")
	void deleteGrade(long userId, int movieId);
}
