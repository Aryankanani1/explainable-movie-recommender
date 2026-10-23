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
import static org.assertj.core.api.Assertions.within;

/** Golden values G3, G4 and G5 from ALGORITHMS.md §10, plus the §4 edge cases. */
class ContentBasedRecommenderTest {

	private static final double TOLERANCE = 1e-4;

	private static final ScoringConfig G3 = new ScoringConfig(false, false, 0, 0);

	private static final ScoringConfig G4 = new ScoringConfig(true, false, 0, 0);

	private static final ScoringConfig G5 = new ScoringConfig(true, true, 0, 0);

	private final CatalogSnapshot catalog = LectureFixture.catalog();

	@Test
	void genreIdfIsLogOfCatalogSizeOverGenreCount() {
		assertThat(catalog.genreIdf()).containsExactly(new double[] { 0.5596, 0.8473, 0.3365, 1.2528, 1.9459 },
				within(TOLERANCE));
	}

	@Test
	void g3RawGradesOnNormalizedRows() {
		ContentProfile profile = profile(G3, LectureFixture.defaultGrades());

		assertThat(profile.p()).containsExactly(new double[] { 10.6569, 6.4142, 12.0711, 5.0000, 0 }, within(TOLERANCE));
		assertRanking(recommend(G3, LectureFixture.defaultGrades()), SPIDER_MAN, 0.8909, HITCHHIKERS, 0.7064,
				BATMAN_BEGINS, 0.6691, LADY_BIRD, 0.0);
	}

	@Test
	void g4CenteringTurnsTheLowGradeNegative() {
		ContentProfile profile = profile(G4, LectureFixture.defaultGrades());

		assertThat(profile.rBar()).isCloseTo(6.6667, within(TOLERANCE));
		assertThat(profile.w()).containsExactly(new double[] { -4.6667, 3.3333, 1.3333 }, within(TOLERANCE));
		assertThat(profile.p()).containsExactly(new double[] { 2.6095, -1.6332, -0.6904, 1.6667, 0 }, within(TOLERANCE));
		assertRanking(recommend(G4, LectureFixture.defaultGrades()), HITCHHIKERS, 0.4277, SPIDER_MAN, 0.3803,
				LADY_BIRD, 0.0, BATMAN_BEGINS, -0.1935);
	}

	@Test
	void g5CenteringPlusIdfIsTheImprovedGenreMethod() {
		ContentProfile profile = profile(G5, LectureFixture.defaultGrades());
		RecommendationResult result = recommend(G5, LectureFixture.defaultGrades());

		assertThat(profile.p()).containsExactly(new double[] { 2.2751, -2.6227, -0.3545, 2.5349, 0 }, within(TOLERANCE));
		assertThat(result.method()).isEqualTo("content-based");
		assertThat(result.fallback()).isEqualTo(Fallback.NONE);
		assertRanking(result, SPIDER_MAN, 0.4097, HITCHHIKERS, 0.3201, LADY_BIRD, 0.0, BATMAN_BEGINS, -0.0822);
	}

	@Test
	void improvedConfigIsCenteredIdfWithoutPrior() {
		assertThat(ScoringConfig.improved(0.5)).isEqualTo(new ScoringConfig(true, true, 0.5, 0.0));
	}

	@Test
	void singleGradeFallsBackToTheScaleMidpoint() {
		ContentProfile high = profile(G5, grades(Map.of(GUARDIANS, 8.0)));
		ContentProfile low = profile(G5, grades(Map.of(GUARDIANS, 3.0)));

		// r̄ = r, so r − r̄ = 0; the midpoint 5.5 decides whether the one grade is a like or a dislike.
		assertThat(high.w()).containsExactly(new double[] { 2.5 }, within(TOLERANCE));
		assertThat(low.w()).containsExactly(new double[] { -2.5 }, within(TOLERANCE));
		assertRanking(recommend(G5, grades(Map.of(GUARDIANS, 8.0))), HITCHHIKERS, 0.9789, BATMAN_V_SUPERMAN, 0.5534,
				CAPTAIN_AMERICA, 0.3964, SPIDER_MAN, 0.3964, BATMAN_BEGINS, 0.2043, LADY_BIRD, 0.0);
	}

	@Test
	void allEqualGradesFallBackToTheScaleMidpoint() {
		ContentProfile profile = profile(G5,
				grades(Map.of(BATMAN_V_SUPERMAN, 8.0, GUARDIANS, 8.0, CAPTAIN_AMERICA, 8.0)));

		assertThat(profile.w()).containsExactly(new double[] { 2.5, 2.5, 2.5 }, within(TOLERANCE));
		assertThat(profile.p()).containsExactly(new double[] { 2.9918, 3.6094, 2.7215, 1.9012, 0 }, within(TOLERANCE));
	}

	@Test
	void noGradesFallsBackToPopularity() {
		RecommendationResult result = recommend(G5, grades(Map.of()));

		assertThat(result.method()).isEqualTo("content-based");
		assertThat(result.fallback()).isEqualTo(Fallback.POPULARITY);
		assertThat(result.items()).extracting(Recommendation::movieId).startsWith(GUARDIANS, BATMAN_BEGINS);
	}

	@Test
	void unknownMovieIdsAreIgnoredIncludingInTheMean() {
		RecommendationResult withUnknown = recommend(G5,
				grades(Map.of(BATMAN_V_SUPERMAN, 2.0, GUARDIANS, 10.0, CAPTAIN_AMERICA, 8.0, 999, 1.0)));

		assertRanking(withUnknown, SPIDER_MAN, 0.4097, HITCHHIKERS, 0.3201, LADY_BIRD, 0.0, BATMAN_BEGINS, -0.0822);
	}

	@Test
	void movieWithoutGenresHasAZeroVectorAndScoresZero() {
		CatalogSnapshot withGenreless = CatalogSnapshot.builder()
			.addMovie(1, "A Drama", null, List.of("Drama"), Map.of(), 5)
			.addMovie(2, "No Genres Listed", null, List.of(), Map.of(), 9)
			.addMovie(3, "Another Drama", null, List.of("Drama"), Map.of(), 1)
			.build();
		ContentBasedRecommender recommender = new ContentBasedRecommender(G5);

		RecommendationResult result = recommender.recommend(withGenreless, grades(Map.of(1, 9.0)),
				RecommendOptions.defaults());
		RecommendationResult onlyGenreless = recommender.recommend(withGenreless, grades(Map.of(2, 9.0)),
				RecommendOptions.defaults());

		assertThat(withGenreless.itemVectors(true, 0).dense(1)).containsOnly(0.0);
		assertRanking(result, 3, 1.0, 2, 0.0);
		assertThat(onlyGenreless.fallback()).isEqualTo(Fallback.POPULARITY);
	}

	@Test
	void itemVectorsAreBuiltOncePerConfig() {
		ItemVectors first = catalog.itemVectors(true, 0);

		assertThat(catalog.itemVectors(true, 0)).isSameAs(first);
		assertThat(catalog.itemVectors(false, 0)).isNotSameAs(first);
		assertThat(catalog.itemVectors(true, 0.5)).isNotSameAs(first);
		assertThat(first.dense(catalog.indexOf(GUARDIANS))).containsExactly(new double[] { 0.3397, 0.5143, 0.2043, 0.7605, 0 },
				within(TOLERANCE));
	}

	@Test
	void popularityPriorAddsLogScaledRatingCount() {
		ScoringConfig withPrior = new ScoringConfig(true, true, 0, 0.1);
		RecommendationResult plain = recommend(G5, LectureFixture.defaultGrades());
		RecommendationResult boosted = recommend(withPrior, LectureFixture.defaultGrades());

		for (Recommendation item : plain.items()) {
			int ratingCount = catalog.ratingCount(catalog.indexOf(item.movieId()));
			double prior = 0.1 * Math.log(1 + ratingCount) / Math.log(1 + 400);
			Recommendation sameMovie = boosted.items()
				.stream()
				.filter(other -> other.movieId() == item.movieId())
				.findFirst()
				.orElseThrow();
			assertThat(sameMovie.score()).isCloseTo(item.score() + prior, within(1e-9));
		}
	}

	private ContentProfile profile(ScoringConfig config, UserRatings user) {
		return ContentProfile.of(catalog, user, config, catalog.itemVectors(config.useIdf(), config.tagWeight()));
	}

	private RecommendationResult recommend(ScoringConfig config, UserRatings user) {
		return new ContentBasedRecommender(config).recommend(catalog, user, RecommendOptions.defaults());
	}

	private static UserRatings grades(Map<Integer, Double> byMovieId) {
		return new UserRatings(byMovieId, LectureFixture.SCALE);
	}
}
