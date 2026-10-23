package edu.fau.recsys.evaluation;

import java.util.Arrays;
import java.util.Random;

/**
 * Paired bootstrap over users (EVALUATION.md, optional rigor): resample users with replacement, recompute the mean
 * difference a − b each time, and take the 2.5th and 97.5th percentiles as a 95% confidence interval. If the interval
 * excludes 0, the difference is unlikely to be luck.
 */
final class Bootstrap {

	private Bootstrap() {
	}

	/** @param a, b per-user values for the same users in the same order */
	static Interval pairedDifference(double[] a, double[] b, int resamples, long seed) {
		if (a.length != b.length || a.length == 0) {
			throw new IllegalArgumentException("Need two non-empty arrays of equal length");
		}
		int n = a.length;
		double observed = 0;
		for (int u = 0; u < n; u++) {
			observed += a[u] - b[u];
		}
		observed = observed / n;

		Random random = new Random(seed);
		double[] means = new double[resamples];
		for (int r = 0; r < resamples; r++) {
			double sum = 0;
			for (int draw = 0; draw < n; draw++) {
				int u = random.nextInt(n);
				sum += a[u] - b[u];
			}
			means[r] = sum / n;
		}
		Arrays.sort(means);
		double low = means[(int) Math.floor(0.025 * resamples)];
		double high = means[(int) Math.ceil(0.975 * resamples) - 1];
		return new Interval(observed, low, high);
	}

	record Interval(double mean, double low, double high) {

		boolean excludesZero() {
			return low > 0 || high < 0;
		}
	}
}
