package edu.fau.recsys.engine;

/**
 * @param k list length
 * @param minRatingCount candidates need at least this many ratings
 * @param mmrLambda diversity trade-off for MMR re-ranking (ticket 07); 1.0 means no re-ranking
 * @param explain attach an {@link Explanation} to each item
 */
public record RecommendOptions(int k, int minRatingCount, double mmrLambda, boolean explain) {

	public RecommendOptions {
		if (k < 1) {
			throw new IllegalArgumentException("k must be at least 1: " + k);
		}
		if (minRatingCount < 0) {
			throw new IllegalArgumentException("minRatingCount must not be negative: " + minRatingCount);
		}
		if (mmrLambda < 0 || mmrLambda > 1) {
			throw new IllegalArgumentException("mmrLambda must be in [0, 1]: " + mmrLambda);
		}
	}

	public static RecommendOptions defaults() {
		return new RecommendOptions(10, 0, 1.0, true);
	}
}
