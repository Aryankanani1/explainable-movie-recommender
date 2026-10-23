package edu.fau.recsys.engine;

import java.util.List;

/**
 * Why a movie was recommended (ALGORITHMS.md §7).
 *
 * @param topFeatures up to 3 genres or tags with a positive contribution, largest first
 * @param becauseYouRated up to 2 graded movies with a positive influence, largest first
 * @param sentence the same, as one readable sentence
 */
public record Explanation(List<FeatureContribution> topFeatures, List<RatedInfluence> becauseYouRated,
		String sentence) {

	public Explanation {
		topFeatures = List.copyOf(topFeatures);
		becauseYouRated = List.copyOf(becauseYouRated);
	}
}
