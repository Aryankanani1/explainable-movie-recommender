package edu.fau.recsys.engine;

/**
 * Switches for the content-based method (ALGORITHMS.md §3–4). Each one fixes a weakness of the lecture method.
 *
 * @param centerRatings w_i = r_i − r̄, so a grade below the user's own average pushes its genres down
 * @param useIdf weight genres by idf_g = ln(N / n_g), so common genres count less
 * @param tagWeight β, the weight of the tag block (ticket 04); 0 means genres only
 * @param popularityPrior γ in γ · ln(1 + ratingCount) / ln(1 + maxRatingCount); evaluation-only extension
 */
public record ScoringConfig(boolean centerRatings, boolean useIdf, double tagWeight, double popularityPrior) {

	public ScoringConfig {
		if (tagWeight < 0 || popularityPrior < 0) {
			throw new IllegalArgumentException("tagWeight and popularityPrior must not be negative");
		}
	}

	/** The app's "improved" method: centered, IDF-weighted, no popularity prior. */
	public static ScoringConfig improved(double tagWeight) {
		return new ScoringConfig(true, true, tagWeight, 0.0);
	}
}
