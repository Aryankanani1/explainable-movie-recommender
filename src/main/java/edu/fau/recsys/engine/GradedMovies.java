package edu.fau.recsys.engine;

import java.util.Arrays;
import java.util.Map;

/**
 * The user's graded movies that exist in the catalog, in movieId order. Grades for unknown IDs are dropped here,
 * so they never reach a profile (or, for the content-based method, the mean r̄).
 *
 * @param idx catalog indices
 * @param r grades, aligned with {@code idx}
 */
record GradedMovies(int[] idx, double[] r) {

	static GradedMovies of(CatalogSnapshot catalog, UserRatings user) {
		int count = 0;
		int[] idx = new int[user.byMovieId().size()];
		double[] r = new double[idx.length];
		for (Map.Entry<Integer, Double> rating : user.byMovieId().entrySet()) {
			int i = catalog.indexOf(rating.getKey());
			if (i >= 0) {
				idx[count] = i;
				r[count] = rating.getValue();
				count++;
			}
		}
		return new GradedMovies(Arrays.copyOf(idx, count), Arrays.copyOf(r, count));
	}
}
