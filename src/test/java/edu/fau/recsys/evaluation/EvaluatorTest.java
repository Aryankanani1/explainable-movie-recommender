package edu.fau.recsys.evaluation;

import java.util.List;

import edu.fau.recsys.dataset.MovieLensRating;
import edu.fau.recsys.engine.LectureFixture;
import edu.fau.recsys.engine.PopularityRecommender;
import org.junit.jupiter.api.Test;

import static edu.fau.recsys.engine.LectureFixture.BATMAN_BEGINS;
import static edu.fau.recsys.engine.LectureFixture.GUARDIANS;
import static edu.fau.recsys.engine.LectureFixture.HITCHHIKERS;
import static edu.fau.recsys.engine.LectureFixture.LADY_BIRD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** End to end on a hand-checkable case: popularity on the 7-movie fixture, K = 2. */
class EvaluatorTest {

	private static final double TOLERANCE = 1e-4;

	// User 1 rated Guardians; the hidden Batman Begins (4.5) is relevant, Lady Bird (2.0) is not.
	private static final UserSplit USER_1 = new UserSplit(1, List.of(rating(1, GUARDIANS, 5.0)),
			List.of(rating(1, BATMAN_BEGINS, 4.5), rating(1, LADY_BIRD, 2.0)));

	// User 2 has no relevant hidden movie (3.0 < 4.0), so is skipped for the accuracy metrics only.
	private static final UserSplit USER_2 = new UserSplit(2, List.of(rating(2, LADY_BIRD, 4.0)),
			List.of(rating(2, HITCHHIKERS, 3.0)));

	@Test
	void popularityOnTheFixture() {
		Evaluator evaluator = new Evaluator(LectureFixture.catalog(), List.of(USER_1, USER_2), 2, 4.0);

		MethodResult result = evaluator.run(new EvaluatedMethod("popularity", "PopularityRecommender",
				userId -> new PopularityRecommender()));

		// User 1 gets [Batman Begins 380, Captain America 350]: a hit at rank 1 of 1 relevant.
		assertThat(result.method()).isEqualTo("popularity");
		assertThat(result.usersEvaluated()).isEqualTo(1);
		assertThat(result.usersSkipped()).isEqualTo(1);
		assertThat(result.precision()).isCloseTo(0.5, within(TOLERANCE));
		assertThat(result.recall()).isCloseTo(1.0, within(TOLERANCE));
		assertThat(result.ndcg()).isCloseTo(1.0, within(TOLERANCE));
		assertThat(result.hitRate()).isCloseTo(1.0, within(TOLERANCE));
		assertThat(result.ndcgPerUser()).containsExactly(1.0);
		// User 2 gets [Guardians, Batman Begins]; together 3 distinct movies of 7.
		assertThat(result.coverage()).isCloseTo(3.0 / 7, within(TOLERANCE));
		// ILD over both users: (1 − 0.7071) and (1 − 0.5).
		assertThat(result.ild()).isCloseTo((0.2929 + 0.5) / 2, within(TOLERANCE));
	}

	private static MovieLensRating rating(int userId, int movieId, double rating) {
		return new MovieLensRating(userId, movieId, rating, 0);
	}
}
