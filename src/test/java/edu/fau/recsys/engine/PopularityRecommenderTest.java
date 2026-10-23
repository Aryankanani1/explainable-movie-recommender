package edu.fau.recsys.engine;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static edu.fau.recsys.engine.LectureFixture.BATMAN_BEGINS;
import static edu.fau.recsys.engine.LectureFixture.BATMAN_V_SUPERMAN;
import static edu.fau.recsys.engine.LectureFixture.CAPTAIN_AMERICA;
import static edu.fau.recsys.engine.LectureFixture.GUARDIANS;
import static edu.fau.recsys.engine.LectureFixture.HITCHHIKERS;
import static edu.fau.recsys.engine.LectureFixture.LADY_BIRD;
import static edu.fau.recsys.engine.LectureFixture.SPIDER_MAN;
import static edu.fau.recsys.engine.LectureRecommenderTest.assertRanking;
import static org.assertj.core.api.Assertions.assertThat;

class PopularityRecommenderTest {

	private final PopularityRecommender popularity = new PopularityRecommender();

	@Test
	void scoresAreRatingCountOverTheCatalogMaximum() {
		UserRatings onlyGuardians = new UserRatings(Map.of(GUARDIANS, 10.0), LectureFixture.SCALE);

		RecommendationResult result = popularity.recommend(LectureFixture.catalog(), onlyGuardians,
				RecommendOptions.defaults());

		// Guardians (400 ratings) is graded, so it is not a candidate, but it still sets the maximum.
		assertThat(result.method()).isEqualTo("popularity");
		assertThat(result.fallback()).isEqualTo(Fallback.NONE);
		assertRanking(result, BATMAN_BEGINS, 0.95, CAPTAIN_AMERICA, 0.875, BATMAN_V_SUPERMAN, 0.75, SPIDER_MAN, 0.625,
				HITCHHIKERS, 0.3, LADY_BIRD, 0.2);
	}

	@Test
	void equalCountsBreakByMovieId() {
		CatalogSnapshot catalog = CatalogSnapshot.builder()
			.addMovie(30, "C", null, List.of("Drama"), Map.of(), 50)
			.addMovie(10, "A", null, List.of("Drama"), Map.of(), 50)
			.addMovie(20, "B", null, List.of("Drama"), Map.of(), 50)
			.build();

		RecommendationResult result = popularity.recommend(catalog, new UserRatings(Map.of(), LectureFixture.SCALE),
				RecommendOptions.defaults());

		assertThat(result.items()).extracting(Recommendation::movieId).containsExactly(10, 20, 30);
	}

	@Test
	void explanationStatesTheRatingCount() {
		RecommendationResult result = popularity.recommend(LectureFixture.catalog(),
				new UserRatings(Map.of(), LectureFixture.SCALE), new RecommendOptions(1, 0, 1.0, true));

		Explanation explanation = result.items().get(0).explanation();
		assertThat(explanation.topFeatures()).isEmpty();
		assertThat(explanation.becauseYouRated()).isEmpty();
		assertThat(explanation.sentence()).isEqualTo("Popular with other viewers: 400 ratings.");
	}
}
