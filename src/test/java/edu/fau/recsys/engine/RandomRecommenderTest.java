package edu.fau.recsys.engine;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static edu.fau.recsys.engine.LectureFixture.GUARDIANS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Baseline (ALGORITHMS.md §5): a seeded shuffle of the candidates, score = 1 − rank / size. */
class RandomRecommenderTest {

	private final CatalogSnapshot catalog = LectureFixture.catalog();

	private final UserRatings onlyGuardians = new UserRatings(Map.of(GUARDIANS, 10.0), LectureFixture.SCALE);

	@Test
	void sameSeedSameListAndNeverARatedMovie() {
		RecommendationResult first = new RandomRecommender(42).recommend(catalog, onlyGuardians,
				RecommendOptions.defaults());
		RecommendationResult again = new RandomRecommender(42).recommend(catalog, onlyGuardians,
				RecommendOptions.defaults());

		assertThat(first.method()).isEqualTo("random");
		assertThat(first.items()).hasSize(6).isEqualTo(again.items());
		assertThat(first.items()).extracting(Recommendation::movieId).doesNotContain(GUARDIANS);
	}

	@Test
	void scoresAreOneMinusRankOverSize() {
		RecommendationResult result = new RandomRecommender(7).recommend(catalog, onlyGuardians,
				RecommendOptions.defaults());

		for (Recommendation item : result.items()) {
			assertThat(item.score()).isCloseTo(1.0 - item.rank() / 6.0, within(1e-12));
		}
	}

	@Test
	void differentSeedsGiveDifferentOrders() {
		var orderA = new RandomRecommender(1).recommend(catalog, onlyGuardians, RecommendOptions.defaults()).items();
		var orderB = new RandomRecommender(2).recommend(catalog, onlyGuardians, RecommendOptions.defaults()).items();

		assertThat(orderA.stream().map(Recommendation::movieId).toList())
			.isNotEqualTo(orderB.stream().map(Recommendation::movieId).toList());
	}
}
