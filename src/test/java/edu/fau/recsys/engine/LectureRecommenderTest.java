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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Golden values G1, G2 and G7 from ALGORITHMS.md §10. */
class LectureRecommenderTest {

	private static final double TOLERANCE = 1e-4;

	private final CatalogSnapshot catalog = LectureFixture.catalog();

	private final LectureRecommender lecture = new LectureRecommender();

	@Test
	void g1ProfileMatchesTheSlides() {
		LectureProfile profile = LectureProfile.of(catalog, LectureFixture.defaultGrades());

		assertThat(profile.P()).containsExactly(new double[] { 18, 12, 20, 10, 0 }, within(TOLERANCE));
		assertThat(profile.T()).isCloseTo(60, within(TOLERANCE));
		assertThat(profile.pHat()).containsExactly(new double[] { 0.3000, 0.2000, 0.3333, 0.1667, 0 }, within(TOLERANCE));
	}

	@Test
	void g1RankingMatchesTheSlides() {
		RecommendationResult result = lecture.recommend(catalog, LectureFixture.defaultGrades(),
				RecommendOptions.defaults());

		assertThat(result.method()).isEqualTo("lecture");
		assertThat(result.fallback()).isEqualTo(Fallback.NONE);
		assertRanking(result, HITCHHIKERS, 0.6667, SPIDER_MAN, 0.6333, BATMAN_BEGINS, 0.3333, LADY_BIRD, 0.0);
	}

	@Test
	void g2SwappedGradesChangeProfileAndRanking() {
		UserRatings swapped = grades(Map.of(BATMAN_V_SUPERMAN, 10.0, GUARDIANS, 2.0, CAPTAIN_AMERICA, 8.0));

		LectureProfile profile = LectureProfile.of(catalog, swapped);
		RecommendationResult result = lecture.recommend(catalog, swapped, RecommendOptions.defaults());

		assertThat(profile.P()).containsExactly(new double[] { 10, 12, 20, 2, 0 }, within(TOLERANCE));
		assertThat(profile.T()).isCloseTo(44, within(TOLERANCE));
		assertRanking(result, SPIDER_MAN, 0.6818, HITCHHIKERS, 0.5455, BATMAN_BEGINS, 0.4545, LADY_BIRD, 0.0);
	}

	@Test
	void g7TiesBreakByRatingCountThenMovieId() {
		UserRatings onlyGuardians = grades(Map.of(GUARDIANS, 10.0));

		LectureProfile profile = LectureProfile.of(catalog, onlyGuardians);
		RecommendationResult result = lecture.recommend(catalog, onlyGuardians, RecommendOptions.defaults());

		assertThat(profile.pHat()).containsExactly(new double[] { 0.25, 0.25, 0.25, 0.25, 0 }, within(TOLERANCE));
		assertRanking(result, HITCHHIKERS, 0.75, CAPTAIN_AMERICA, 0.5, BATMAN_V_SUPERMAN, 0.5, SPIDER_MAN, 0.5,
				BATMAN_BEGINS, 0.25, LADY_BIRD, 0.0);
	}

	@Test
	void neverRecommendsAGradedMovie() {
		RecommendationResult result = lecture.recommend(catalog, LectureFixture.defaultGrades(),
				RecommendOptions.defaults());

		assertThat(result.items()).extracting(Recommendation::movieId)
			.doesNotContain(BATMAN_V_SUPERMAN, GUARDIANS, CAPTAIN_AMERICA);
	}

	@Test
	void kLimitsTheList() {
		RecommendationResult result = lecture.recommend(catalog, LectureFixture.defaultGrades(),
				new RecommendOptions(2, 0, 1.0, true));

		assertRanking(result, HITCHHIKERS, 0.6667, SPIDER_MAN, 0.6333);
	}

	@Test
	void minRatingCountDropsRarelyRatedCandidates() {
		RecommendationResult result = lecture.recommend(catalog, LectureFixture.defaultGrades(),
				new RecommendOptions(10, 200, 1.0, true));

		// Hitchhiker's (120 ratings) and Lady Bird (80) are below the threshold.
		assertRanking(result, SPIDER_MAN, 0.6333, BATMAN_BEGINS, 0.3333);
	}

	@Test
	void unknownMovieIdsAreIgnored() {
		RecommendationResult withUnknown = lecture.recommend(catalog, grades(Map.of(GUARDIANS, 10.0, 999, 1.0)),
				RecommendOptions.defaults());

		assertRanking(withUnknown, HITCHHIKERS, 0.75, CAPTAIN_AMERICA, 0.5, BATMAN_V_SUPERMAN, 0.5, SPIDER_MAN, 0.5,
				BATMAN_BEGINS, 0.25, LADY_BIRD, 0.0);
	}

	@Test
	void noGradesFallsBackToPopularity() {
		RecommendationResult result = lecture.recommend(catalog, grades(Map.of()), RecommendOptions.defaults());

		assertThat(result.method()).isEqualTo("lecture");
		assertThat(result.fallback()).isEqualTo(Fallback.POPULARITY);
		assertThat(result.items()).extracting(Recommendation::movieId)
			.containsExactly(GUARDIANS, BATMAN_BEGINS, CAPTAIN_AMERICA, BATMAN_V_SUPERMAN, SPIDER_MAN, HITCHHIKERS,
					LADY_BIRD);
	}

	@Test
	void onlyUnknownMovieIdsFallsBackToPopularity() {
		RecommendationResult result = lecture.recommend(catalog, grades(Map.of(999, 7.0)), RecommendOptions.defaults());

		assertThat(result.fallback()).isEqualTo(Fallback.POPULARITY);
	}

	@Test
	void zeroProfileFallsBackToPopularity() {
		CatalogSnapshot withGenrelessMovie = CatalogSnapshot.builder()
			.addMovie(1, "No Genres Listed", null, List.of(), Map.of(), 10)
			.addMovie(2, "A Drama", null, List.of("Drama"), Map.of(), 5)
			.build();

		RecommendationResult result = lecture.recommend(withGenrelessMovie, grades(Map.of(1, 9.0)),
				RecommendOptions.defaults());

		assertThat(result.fallback()).isEqualTo(Fallback.POPULARITY);
		assertThat(result.items()).extracting(Recommendation::movieId).containsExactly(2);
	}

	@Test
	void explanationsAreOmittedWhenNotRequested() {
		RecommendationResult result = lecture.recommend(catalog, LectureFixture.defaultGrades(),
				new RecommendOptions(10, 0, 1.0, false));

		assertThat(result.items()).extracting(Recommendation::explanation).containsOnlyNulls();
	}

	private static UserRatings grades(Map<Integer, Double> byMovieId) {
		return new UserRatings(byMovieId, LectureFixture.SCALE);
	}

	/** {@code expected} alternates movieId and score, in rank order. */
	static void assertRanking(RecommendationResult result, Object... expected) {
		assertThat(result.items()).hasSize(expected.length / 2);
		for (int n = 0; n < result.items().size(); n++) {
			Recommendation item = result.items().get(n);
			assertThat(item.rank()).as("rank of item %d", n).isEqualTo(n + 1);
			assertThat(item.movieId()).as("movie at rank %d", n + 1).isEqualTo(expected[2 * n]);
			assertThat(item.score()).as("score at rank %d", n + 1)
				.isCloseTo(((Number) expected[2 * n + 1]).doubleValue(), within(TOLERANCE));
		}
	}
}
