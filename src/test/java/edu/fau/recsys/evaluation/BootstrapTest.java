package edu.fau.recsys.evaluation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BootstrapTest {

	@Test
	void aConstantDifferenceHasAZeroWidthInterval() {
		double[] a = { 0.5, 0.7, 0.2, 0.9 };
		double[] b = { 0.4, 0.6, 0.1, 0.8 };

		Bootstrap.Interval interval = Bootstrap.pairedDifference(a, b, 1000, 42);

		assertThat(interval.mean()).isCloseTo(0.1, within(1e-12));
		assertThat(interval.low()).isCloseTo(0.1, within(1e-12));
		assertThat(interval.high()).isCloseTo(0.1, within(1e-12));
	}

	@Test
	void theIntervalContainsTheMeanAndIsReproducible() {
		double[] a = { 0.9, 0.1, 0.5, 0.3, 0.8, 0.0, 0.6 };
		double[] b = { 0.2, 0.4, 0.5, 0.1, 0.3, 0.2, 0.6 };

		Bootstrap.Interval first = Bootstrap.pairedDifference(a, b, 1000, 42);

		assertThat(first.low()).isLessThanOrEqualTo(first.mean());
		assertThat(first.high()).isGreaterThanOrEqualTo(first.mean());
		assertThat(Bootstrap.pairedDifference(a, b, 1000, 42)).isEqualTo(first);
	}
}
