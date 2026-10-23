package edu.fau.recsys.engine;

/**
 * The content-based user profile (ALGORITHMS.md §4):
 * <pre>
 *   r̄   = mean of the grades
 *   w_i = centerRatings ? r_i − r̄ : r_i        (midpoint fallback when the centered weights are all zero)
 *   p   = Σ_i w_i · v_i
 * </pre>
 *
 * @param graded catalog indices of the graded movies that exist in the catalog, in movieId order
 * @param r their grades, aligned with {@code graded}
 */
record ContentProfile(int[] graded, double[] r, double rBar, double[] w, double[] p) {

	/** Σ|w_i| below this means centering removed everything (one grade, or all grades equal). */
	static final double ZERO_WEIGHTS = 1e-9;

	/** ‖p‖ below this means there is no direction to score against. */
	static final double ZERO_PROFILE = 1e-12;

	static ContentProfile of(CatalogSnapshot catalog, UserRatings user, ScoringConfig config, double[][] v) {
		GradedMovies gradedMovies = GradedMovies.of(catalog, user);
		int[] graded = gradedMovies.idx();
		double[] r = gradedMovies.r();

		double rBar = 0;
		for (int n = 0; n < graded.length; n++) {
			rBar += r[n];
		}
		if (graded.length > 0) {
			rBar = rBar / graded.length;
		}

		double[] w = new double[graded.length];
		double sumAbsW = 0;
		for (int n = 0; n < graded.length; n++) {
			w[n] = config.centerRatings() ? r[n] - rBar : r[n];
			sumAbsW += Math.abs(w[n]);
		}
		if (config.centerRatings() && sumAbsW < ZERO_WEIGHTS) {
			// r − r̄ is 0 for every grade, so judge each grade against the middle of the scale instead.
			for (int n = 0; n < graded.length; n++) {
				w[n] = r[n] - user.scale().midpoint();
			}
		}

		int dimensions = v.length == 0 ? 0 : v[0].length;
		double[] p = new double[dimensions];
		for (int n = 0; n < graded.length; n++) {
			int i = graded[n];
			for (int d = 0; d < dimensions; d++) {
				p[d] += w[n] * v[i][d];
			}
		}
		return new ContentProfile(graded, r, rBar, w, p);
	}

	/** No graded movie in the catalog, or the weighted vectors cancel out: fall back to popularity. */
	boolean isEmpty() {
		return graded.length == 0 || Vectors.norm(p) < ZERO_PROFILE;
	}
}
