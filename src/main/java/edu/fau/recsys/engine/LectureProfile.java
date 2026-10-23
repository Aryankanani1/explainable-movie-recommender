package edu.fau.recsys.engine;

/**
 * The lecture's user profile (ALGORITHMS.md §2), on raw grades and raw 0/1 genre rows:
 * <pre>
 *   P_g  = Σ_i r_i · x_ig        column sums of the weighted genre matrix
 *   T    = Σ_g P_g
 *   p̂_g  = P_g / T               sums to 1
 * </pre>
 *
 * @param graded catalog indices of the graded movies that exist in the catalog, in movieId order
 * @param r their grades, aligned with {@code graded}
 */
record LectureProfile(int[] graded, double[] r, double[] P, double T, double[] pHat) {

	static LectureProfile of(CatalogSnapshot catalog, UserRatings user) {
		GradedMovies gradedMovies = GradedMovies.of(catalog, user);
		int[] graded = gradedMovies.idx();
		double[] r = gradedMovies.r();

		double[][] x = catalog.genreRows();
		int G = catalog.genreNames().size();
		double[] P = new double[G];
		for (int n = 0; n < graded.length; n++) {
			int i = graded[n];
			for (int g = 0; g < G; g++) {
				P[g] += r[n] * x[i][g];
			}
		}

		double T = 0;
		for (int g = 0; g < G; g++) {
			T += P[g];
		}

		double[] pHat = new double[G];
		if (T != 0) {
			for (int g = 0; g < G; g++) {
				pHat[g] = P[g] / T;
			}
		}
		return new LectureProfile(graded, r, P, T, pHat);
	}

	/** No graded movie in the catalog, or none of them has a genre: the method has nothing to score with. */
	boolean isEmpty() {
		return graded.length == 0 || T == 0;
	}
}
