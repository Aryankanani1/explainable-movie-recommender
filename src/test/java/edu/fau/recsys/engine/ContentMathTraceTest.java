package edu.fau.recsys.engine;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static edu.fau.recsys.engine.LectureFixture.BATMAN_V_SUPERMAN;
import static edu.fau.recsys.engine.LectureFixture.CAPTAIN_AMERICA;
import static edu.fau.recsys.engine.LectureFixture.GUARDIANS;
import static edu.fau.recsys.engine.LectureFixture.HITCHHIKERS;
import static edu.fau.recsys.engine.LectureFixture.LADY_BIRD;
import static edu.fau.recsys.engine.LectureFixture.SPIDER_MAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** "Show the math" for the improved genre method (G5): centered weights, IDF row, normalized rows, cosine scores. */
class ContentMathTraceTest {

	private static final double TOLERANCE = 1e-4;

	private final ContentBasedRecommender improved = new ContentBasedRecommender(new ScoringConfig(true, true, 0, 0));

	private final MathTrace trace = improved.trace(LectureFixture.catalog(), LectureFixture.defaultGrades(), 3);

	@Test
	void columnsAreGradedGenresPlusShownCandidateGenres() {
		// Lady Bird is 3rd under G5, so Drama is shown.
		assertThat(trace.method()).isEqualTo("content-based");
		assertThat(trace.fallback()).isEqualTo(Fallback.NONE);
		assertThat(trace.lecture()).isNull();
		assertThat(trace.columns()).containsExactly("Comedy", "Adventure", "Super Hero", "Sci-Fi", "Drama");
	}

	@Test
	void centeredWeightsAndIdfRow() {
		MathTrace.ContentBased content = trace.contentBased();

		assertThat(content.config()).isEqualTo(new ScoringConfig(true, true, 0, 0));
		assertThat(content.rBar()).isCloseTo(6.6667, within(TOLERANCE));
		assertThat(content.idf()).containsExactly(new double[] { 0.5596, 0.8473, 0.3365, 1.2528, 1.9459 },
				within(TOLERANCE));
		assertThat(content.rows()).extracting(MathTrace.ContentRow::movieId)
			.containsExactly(BATMAN_V_SUPERMAN, GUARDIANS, CAPTAIN_AMERICA);
		assertThat(content.rows()).extracting(MathTrace.ContentRow::grade).containsExactly(2.0, 10.0, 8.0);
		assertThat(content.rows().get(0).w()).isCloseTo(-4.6667, within(TOLERANCE));
		assertThat(content.rows().get(1).w()).isCloseTo(3.3333, within(TOLERANCE));
	}

	@Test
	void normalizedRowsAndTheirWeightedSumIsTheProfile() {
		MathTrace.ContentBased content = trace.contentBased();
		MathTrace.ContentRow guardians = content.rows().get(1);

		assertThat(guardians.x()).containsExactly(1, 1, 1, 1, 0);
		assertThat(guardians.gHat()).containsExactly(new double[] { 0.3397, 0.5143, 0.2043, 0.7605, 0 }, within(TOLERANCE));
		assertThat(guardians.wGHat()).containsExactly(new double[] { 1.1324, 1.7145, 0.6808, 2.5349, 0 },
				within(TOLERANCE));
		assertThat(content.rows().get(0).wGHat()).containsExactly(new double[] { 0, -4.3372, -1.7224, 0, 0 },
				within(TOLERANCE));
		assertThat(content.p()).containsExactly(new double[] { 2.2751, -2.6227, -0.3545, 2.5349, 0 }, within(TOLERANCE));
	}

	@Test
	void candidatesShowCosineScoreAndPerGenreContribution() {
		MathTrace.ContentBased content = trace.contentBased();

		assertThat(content.candidates()).extracting(MathTrace.ContentCandidate::movieId)
			.containsExactly(SPIDER_MAN, HITCHHIKERS, LADY_BIRD);
		MathTrace.ContentCandidate hitchhikers = content.candidates().get(1);
		assertThat(hitchhikers.score()).isCloseTo(0.3201, within(TOLERANCE));
		assertThat(hitchhikers.contrib()).containsExactly(new double[] { 0.1830, -0.3195, 0, 0.4565, 0 },
				within(TOLERANCE));
		assertThat(hitchhikers.genrePart()).isCloseTo(0.3201, within(TOLERANCE));
		assertThat(hitchhikers.tagPart()).isZero();
		assertThat(content.candidates().get(0).contrib()).containsExactly(new double[] { 0.4520, 0, -0.0423, 0, 0 },
				within(TOLERANCE));
	}

	@Test
	void noGradesGivesAnEmptyTraceFlaggedAsFallback() {
		MathTrace empty = improved.trace(LectureFixture.catalog(), new UserRatings(Map.of(), LectureFixture.SCALE), 5);

		assertThat(empty.fallback()).isEqualTo(Fallback.POPULARITY);
		assertThat(empty.columns()).isEmpty();
		assertThat(empty.contentBased()).isNull();
	}
}
