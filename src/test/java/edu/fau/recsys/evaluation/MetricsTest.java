package edu.fau.recsys.evaluation;

import java.util.List;
import java.util.Set;

import edu.fau.recsys.engine.CatalogSnapshot;
import edu.fau.recsys.engine.LectureFixture;
import org.junit.jupiter.api.Test;

import static edu.fau.recsys.engine.LectureFixture.BATMAN_BEGINS;
import static edu.fau.recsys.engine.LectureFixture.HITCHHIKERS;
import static edu.fau.recsys.engine.LectureFixture.LADY_BIRD;
import static edu.fau.recsys.engine.LectureFixture.SPIDER_MAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Golden values from EVALUATION.md. */
class MetricsTest {

	private static final double TOLERANCE = 1e-4;

	/** Hits at positions 1 and 3 of a 5-item list, with 2 relevant items. */
	private static final List<Integer> FIVE = List.of(10, 20, 30, 40, 50);

	private static final Set<Integer> TWO_RELEVANT = Set.of(10, 30);

	@Test
	void precisionRecallAndHitRateAtFive() {
		assertThat(Metrics.precision(FIVE, TWO_RELEVANT, 5)).isCloseTo(0.4, within(TOLERANCE));
		assertThat(Metrics.recall(FIVE, TWO_RELEVANT, 5)).isCloseTo(1.0, within(TOLERANCE));
		assertThat(Metrics.hitRate(FIVE, TWO_RELEVANT, 5)).isEqualTo(1.0);
	}

	@Test
	void ndcgAtFive() {
		assertThat(Metrics.dcg(FIVE, TWO_RELEVANT, 5)).isCloseTo(1.5, within(TOLERANCE));
		assertThat(Metrics.idealDcg(TWO_RELEVANT.size(), 5)).isCloseTo(1.6309, within(TOLERANCE));
		assertThat(Metrics.ndcg(FIVE, TWO_RELEVANT, 5)).isCloseTo(0.9197, within(TOLERANCE));
	}

	@Test
	void threeHitsInTopTenWithSixRelevant() {
		List<Integer> ten = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		Set<Integer> six = Set.of(2, 5, 9, 100, 200, 300);

		assertThat(Metrics.precision(ten, six, 10)).isCloseTo(0.3, within(TOLERANCE));
		assertThat(Metrics.recall(ten, six, 10)).isCloseTo(0.5, within(TOLERANCE));
	}

	@Test
	void missesEverywhere() {
		assertThat(Metrics.hitRate(FIVE, Set.of(99), 5)).isZero();
		assertThat(Metrics.ndcg(FIVE, Set.of(99), 5)).isZero();
	}

	@Test
	void onlyTheFirstKItemsCount() {
		// The hit at position 3 is outside the top 2.
		assertThat(Metrics.precision(FIVE, Set.of(30), 2)).isZero();
	}

	@Test
	void intraListDiversityOnRawGenreRows() {
		CatalogSnapshot catalog = LectureFixture.catalog();

		// Pair similarities 0, 0.4082 and 0.
		assertThat(Metrics.ild(List.of(SPIDER_MAN, LADY_BIRD, HITCHHIKERS), catalog)).isCloseTo(0.8639, within(TOLERANCE));
		// Pair similarities 0.4082, 0.7071 and 0.
		assertThat(Metrics.ild(List.of(SPIDER_MAN, HITCHHIKERS, BATMAN_BEGINS), catalog)).isCloseTo(0.6282,
				within(TOLERANCE));
		assertThat(Metrics.ild(List.of(SPIDER_MAN), catalog)).isZero();
	}

	@Test
	void coverageIsTheShareOfTheCatalogInAnyList() {
		assertThat(Metrics.coverage(List.of(List.of(4, 6), List.of(6, 5)), 7)).isCloseTo(0.4286, within(TOLERANCE));
	}

	@Test
	void noveltyIsMeanSelfInformationOfTrainPopularity() {
		CatalogSnapshot catalog = LectureFixture.catalog();

		// Guardians has 400 ratings in a world of 999 users: −log2(401 / 1000).
		double expected = -Math.log(401.0 / 1000) / Math.log(2);
		assertThat(Metrics.novelty(List.of(LectureFixture.GUARDIANS), catalog, 999)).isCloseTo(expected, within(1e-9));
	}
}
