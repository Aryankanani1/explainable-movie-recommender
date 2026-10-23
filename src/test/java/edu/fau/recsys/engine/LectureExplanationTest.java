package edu.fau.recsys.engine;

import org.junit.jupiter.api.Test;

import static edu.fau.recsys.engine.LectureFixture.CAPTAIN_AMERICA;
import static edu.fau.recsys.engine.LectureFixture.GUARDIANS;
import static edu.fau.recsys.engine.LectureFixture.HITCHHIKERS;
import static edu.fau.recsys.engine.LectureFixture.LADY_BIRD;
import static edu.fau.recsys.engine.LectureFixture.SPIDER_MAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Golden value G8 from ALGORITHMS.md §10, plus the §7 rules for the lecture method. */
class LectureExplanationTest {

	private static final double TOLERANCE = 1e-4;

	private final RecommendationResult g1 = new LectureRecommender().recommend(LectureFixture.catalog(),
			LectureFixture.defaultGrades(), RecommendOptions.defaults());

	@Test
	void g8SpiderManContributionsAreTheProfileWeightsOfItsGenres() {
		Explanation explanation = explanationOf(SPIDER_MAN);

		assertThat(explanation.topFeatures()).extracting(FeatureContribution::feature)
			.containsExactly("Super Hero", "Comedy");
		assertThat(explanation.topFeatures()).extracting(FeatureContribution::kind).containsOnly("genre");
		assertThat(explanation.topFeatures().get(0).contribution()).isCloseTo(0.3333, within(TOLERANCE));
		assertThat(explanation.topFeatures().get(1).contribution()).isCloseTo(0.3000, within(TOLERANCE));
	}

	@Test
	void g8SpiderManBecauseYouRatedRanksByGradeTimesCosine() {
		Explanation explanation = explanationOf(SPIDER_MAN);

		assertThat(explanation.becauseYouRated()).extracting(RatedInfluence::movieId)
			.containsExactly(CAPTAIN_AMERICA, GUARDIANS);
		RatedInfluence first = explanation.becauseYouRated().get(0);
		assertThat(first.title()).isEqualTo("Captain America: Civil War");
		assertThat(first.grade()).isEqualTo(8.0);
		assertThat(first.influence()).isCloseTo(8.0, within(TOLERANCE));
		assertThat(explanation.becauseYouRated().get(1).influence()).isCloseTo(7.0711, within(TOLERANCE));
	}

	@Test
	void g8SentenceNamesTopGenresAndStrongestGradedMovie() {
		assertThat(explanationOf(SPIDER_MAN).sentence())
			.isEqualTo("Matches your taste for Super Hero and Comedy; you rated Captain America: Civil War 8/10.");
	}

	@Test
	void contributionsAddUpToTheScore() {
		Recommendation hitchhikers = itemOf(HITCHHIKERS);

		double sum = 0;
		for (FeatureContribution contribution : hitchhikers.explanation().topFeatures()) {
			sum += contribution.contribution();
		}
		assertThat(hitchhikers.explanation().topFeatures()).extracting(FeatureContribution::feature)
			.containsExactly("Comedy", "Adventure", "Sci-Fi");
		assertThat(sum).isCloseTo(hitchhikers.score(), within(TOLERANCE));
	}

	@Test
	void ladyBirdHasNothingInCommonWithTheGrades() {
		Explanation explanation = explanationOf(LADY_BIRD);

		assertThat(explanation.topFeatures()).isEmpty();
		assertThat(explanation.becauseYouRated()).isEmpty();
		assertThat(explanation.sentence()).isEqualTo("No overlap with what you graded.");
	}

	private Explanation explanationOf(int movieId) {
		return itemOf(movieId).explanation();
	}

	private Recommendation itemOf(int movieId) {
		return g1.items().stream().filter(item -> item.movieId() == movieId).findFirst().orElseThrow();
	}
}
