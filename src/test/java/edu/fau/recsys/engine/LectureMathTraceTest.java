package edu.fau.recsys.engine;

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

/** The slides' matrices for G1 (ALGORITHMS.md §8). */
class LectureMathTraceTest {

	private static final double TOLERANCE = 1e-4;

	private final LectureRecommender lecture = new LectureRecommender();

	@Test
	void g1TopThreeShowsTheFourSlideGenres() {
		MathTrace trace = lecture.trace(LectureFixture.catalog(), LectureFixture.defaultGrades(), 3);

		assertThat(trace.method()).isEqualTo("lecture");
		assertThat(trace.fallback()).isEqualTo(Fallback.NONE);
		assertThat(trace.columns()).containsExactly("Comedy", "Adventure", "Super Hero", "Sci-Fi");
	}

	@Test
	void g1WeightedGenreMatrix() {
		MathTrace trace = lecture.trace(LectureFixture.catalog(), LectureFixture.defaultGrades(), 3);

		assertThat(trace.rows()).extracting(MathTrace.GradedRow::movieId)
			.containsExactly(BATMAN_V_SUPERMAN, GUARDIANS, CAPTAIN_AMERICA);
		assertThat(trace.rows()).extracting(MathTrace.GradedRow::grade).containsExactly(2.0, 10.0, 8.0);
		assertThat(trace.rows().get(0).x()).containsExactly(0, 1, 1, 0);
		assertThat(trace.rows().get(0).weighted()).containsExactly(0, 2, 2, 0);
		assertThat(trace.rows().get(1).weighted()).containsExactly(10, 10, 10, 10);
		assertThat(trace.rows().get(2).weighted()).containsExactly(8, 0, 8, 0);
	}

	@Test
	void g1Profile() {
		MathTrace.Profile profile = lecture.trace(LectureFixture.catalog(), LectureFixture.defaultGrades(), 3)
			.profile();

		assertThat(profile.P()).containsExactly(new double[] { 18, 12, 20, 10 }, within(TOLERANCE));
		assertThat(profile.T()).isCloseTo(60, within(TOLERANCE));
		assertThat(profile.pHat()).containsExactly(new double[] { 0.3000, 0.2000, 0.3333, 0.1667 }, within(TOLERANCE));
	}

	@Test
	void g1CandidateScoresWithTimesTenAndContributions() {
		MathTrace trace = lecture.trace(LectureFixture.catalog(), LectureFixture.defaultGrades(), 3);

		assertThat(trace.candidates()).extracting(MathTrace.CandidateRow::movieId)
			.containsExactly(HITCHHIKERS, SPIDER_MAN, BATMAN_BEGINS);
		MathTrace.CandidateRow hitchhikers = trace.candidates().get(0);
		assertThat(hitchhikers.x()).containsExactly(1, 1, 0, 1);
		assertThat(hitchhikers.score()).isCloseTo(0.6667, within(TOLERANCE));
		assertThat(hitchhikers.scoreTimes10()).isCloseTo(6.6667, within(TOLERANCE));
		assertThat(hitchhikers.contrib()).containsExactly(new double[] { 0.3, 0.2, 0, 0.1667 }, within(TOLERANCE));
		assertThat(trace.candidates().get(1).scoreTimes10()).isCloseTo(6.3333, within(TOLERANCE));
		assertThat(trace.candidates().get(2).scoreTimes10()).isCloseTo(3.3333, within(TOLERANCE));
	}

	@Test
	void showingLadyBirdAddsTheDramaColumn() {
		MathTrace trace = lecture.trace(LectureFixture.catalog(), LectureFixture.defaultGrades(), 4);

		assertThat(trace.columns()).containsExactly("Comedy", "Adventure", "Super Hero", "Sci-Fi", "Drama");
		assertThat(trace.candidates()).extracting(MathTrace.CandidateRow::movieId).last().isEqualTo(LADY_BIRD);
		assertThat(trace.candidates().get(3).score()).isZero();
	}

	@Test
	void noGradesGivesAnEmptyTraceFlaggedAsFallback() {
		MathTrace trace = lecture.trace(LectureFixture.catalog(), new UserRatings(Map.of(), LectureFixture.SCALE), 5);

		assertThat(trace.fallback()).isEqualTo(Fallback.POPULARITY);
		assertThat(trace.columns()).isEmpty();
		assertThat(trace.rows()).isEmpty();
		assertThat(trace.candidates()).isEmpty();
	}
}
