package edu.fau.recsys.engine;

/**
 * Ranks every catalog movie the user has not rated (and that has at least {@code minRatingCount} ratings).
 * Implementations are stateless and deterministic.
 */
public interface Recommender {

	/** "lecture", "content-based", "popularity" or "random". */
	String id();

	RecommendationResult recommend(CatalogSnapshot catalog, UserRatings user, RecommendOptions options);

}
