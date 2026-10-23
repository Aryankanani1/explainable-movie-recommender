package edu.fau.recsys.engine;

import java.util.Arrays;
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

/** Golden value G5t (ALGORITHMS.md §10): the improved method with tags, β = 0.5, and its Spider-Man explanation. */
class TagFeaturesTest {

	private static final double TOLERANCE = 1e-4;

	private static final ScoringConfig G5T = ScoringConfig.improved(0.5);

	private final CatalogSnapshot catalog = LectureFixture.catalog();

	@Test
	void vocabularyKeepsTagsOnAtLeastTwoMovies() {
		// "british" is only on Hitchhiker's, so it cannot link two movies.
		assertThat(catalog.tagVocabulary()).containsExactly("dc", "marvel", "space");
		assertThat(catalog.tagIdf()).containsExactly(new double[] { 1.2528, 0.8473, 1.2528 }, within(TOLERANCE));
		assertThat(catalog.moviesWithVocabularyTag()).isEqualTo(6);
	}

	@Test
	void tagBlockIsSublinearTfTimesIdfNormalizedThenScaledByBeta() {
		ItemVectors v = catalog.itemVectors(true, 0.5);

		assertThat(tagBlock(v, GUARDIANS)).containsExactly(new double[] { 0, 0.3212, 0.3832 }, within(TOLERANCE));
		assertThat(tagBlock(v, HITCHHIKERS)).containsExactly(new double[] { 0, 0, 0.5 }, within(TOLERANCE));
		assertThat(tagBlock(v, LADY_BIRD)).containsOnly(0.0);
	}

	@Test
	void g5tTagPartOfTheProfile() {
		ContentProfile profile = ContentProfile.of(catalog, LectureFixture.defaultGrades(), G5T,
				catalog.itemVectors(true, 0.5));

		assertThat(profile.p()).containsExactly(
				new double[] { 2.2751, -2.6227, -0.3545, 2.5349, 0, -2.3333, 1.7374, 1.2772 }, within(TOLERANCE));
	}

	@Test
	void g5tRanking() {
		assertRanking(recommend(G5T), SPIDER_MAN, 0.4401, HITCHHIKERS, 0.3371, LADY_BIRD, 0.0, BATMAN_BEGINS, -0.2540);
	}

	@Test
	void betaZeroReproducesG5Exactly() {
		ItemVectors genresOnly = catalog.itemVectors(true, 0);

		assertThat(genresOnly.dimensions()).isEqualTo(5);
		assertRanking(recommend(ScoringConfig.improved(0)), SPIDER_MAN, 0.4097, HITCHHIKERS, 0.3201, LADY_BIRD, 0.0,
				BATMAN_BEGINS, -0.0822);
	}

	@Test
	void g5tSpiderManContributionsSumToTheCosine() {
		Recommendation spiderMan = itemOf(recommend(G5T), SPIDER_MAN);
		Explanation explanation = spiderMan.explanation();

		assertThat(explanation.topFeatures()).extracting(FeatureContribution::feature).containsExactly("Comedy", "marvel");
		assertThat(explanation.topFeatures()).extracting(FeatureContribution::kind).containsExactly("genre", "tag");
		assertThat(explanation.topFeatures().get(0).contribution()).isCloseTo(0.3255, within(TOLERANCE));
		assertThat(explanation.topFeatures().get(1).contribution()).isCloseTo(0.1450, within(TOLERANCE));
		// Super Hero (−0.0305) is negative, so it is not a top feature; with it the three sum to the score.
		assertThat(0.3255 + 0.1450 - 0.0305).isCloseTo(spiderMan.score(), within(TOLERANCE));
	}

	@Test
	void g5tSpiderManBecauseYouRatedSkipsTheDislikedMovie() {
		Explanation explanation = itemOf(recommend(G5T), SPIDER_MAN).explanation();

		assertThat(explanation.becauseYouRated()).extracting(RatedInfluence::movieId)
			.containsExactly(GUARDIANS, CAPTAIN_AMERICA)
			.doesNotContain(BATMAN_V_SUPERMAN);
		assertThat(explanation.becauseYouRated().get(0).influence()).isCloseTo(1.4853, within(TOLERANCE));
		assertThat(explanation.becauseYouRated().get(0).grade()).isEqualTo(10.0);
		assertThat(explanation.becauseYouRated().get(1).influence()).isCloseTo(1.3333, within(TOLERANCE));
	}

	@Test
	void g5tSpiderManSentenceIsTheSpecExample() {
		assertThat(itemOf(recommend(G5T), SPIDER_MAN).explanation().sentence())
			.isEqualTo("Matches your taste for Comedy and \"marvel\"; you rated Guardians of the Galaxy 10/10.");
	}

	@Test
	void hitchhikersAndBatmanBeginsExplanations() {
		RecommendationResult result = recommend(G5T);
		Explanation hitchhikers = itemOf(result, HITCHHIKERS).explanation();
		Explanation batmanBegins = itemOf(result, BATMAN_BEGINS).explanation();

		assertThat(hitchhikers.topFeatures()).extracting(FeatureContribution::feature)
			.containsExactly("Sci-Fi", "Comedy", "space");
		assertThat(hitchhikers.becauseYouRated().get(0).influence()).isCloseTo(3.1213, within(TOLERANCE));
		// Every feature of Batman Begins counts against it, yet two liked movies still share Super Hero with it.
		assertThat(batmanBegins.topFeatures()).isEmpty();
		assertThat(batmanBegins.becauseYouRated()).extracting(RatedInfluence::movieId)
			.containsExactly(CAPTAIN_AMERICA, GUARDIANS);
		assertThat(batmanBegins.sentence()).isEqualTo("Similar to Captain America: Civil War, which you rated 8/10.");
	}

	@Test
	void movieWithTagsButNoGenresHasOnlyATagBlock() {
		CatalogSnapshot catalog = CatalogSnapshot.builder()
			.addMovie(1, "Space Doc", null, List.of(), Map.of("space", 1), 3)
			.addMovie(2, "Space Opera", null, List.of("Sci-Fi"), Map.of("space", 2), 5)
			.addMovie(3, "A Drama", null, List.of("Drama"), Map.of(), 1)
			.build();

		ItemVectors v = catalog.itemVectors(true, 0.5);

		// Columns: Sci-Fi, Drama, then the tag "space".
		assertThat(v.dense(0)).containsExactly(new double[] { 0, 0, 0.5 }, within(TOLERANCE));
		assertThat(v.norm(0)).isCloseTo(0.5, within(TOLERANCE));
		assertThat(v.norm(1)).isCloseTo(Math.sqrt(1.25), within(TOLERANCE));
	}

	private double[] tagBlock(ItemVectors v, int movieId) {
		double[] dense = v.dense(catalog.indexOf(movieId));
		return Arrays.copyOfRange(dense, catalog.genreNames().size(), dense.length);
	}

	private RecommendationResult recommend(ScoringConfig config) {
		return new ContentBasedRecommender(config).recommend(catalog, LectureFixture.defaultGrades(),
				RecommendOptions.defaults());
	}

	private static Recommendation itemOf(RecommendationResult result, int movieId) {
		return result.items().stream().filter(item -> item.movieId() == movieId).findFirst().orElseThrow();
	}
}
