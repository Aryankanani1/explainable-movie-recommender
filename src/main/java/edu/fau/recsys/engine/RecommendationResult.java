package edu.fau.recsys.engine;

import java.util.List;

/**
 * A ranked list. {@code method} is the recommender that was asked; when it had no usable profile the items come
 * from popularity and {@code fallback} says so.
 */
public record RecommendationResult(String method, List<Recommendation> items, Fallback fallback) {

	public RecommendationResult {
		items = List.copyOf(items);
	}
}
