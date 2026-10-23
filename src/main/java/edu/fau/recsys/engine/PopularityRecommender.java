package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.List;

/** Baseline (ALGORITHMS.md §5): score = ratingCount / maxRatingCount, ties broken by the shared order. */
public final class PopularityRecommender implements Recommender {

	public static final String ID = "popularity";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public RecommendationResult recommend(CatalogSnapshot catalog, UserRatings user, RecommendOptions options) {
		int maxRatingCount = catalog.maxRatingCount();
		double[] score = new double[catalog.size()];
		for (int c = 0; c < catalog.size(); c++) {
			score[c] = maxRatingCount == 0 ? 0 : (double) catalog.ratingCount(c) / maxRatingCount;
		}

		boolean[] candidate = Ranking.candidates(catalog, user, options.minRatingCount());
		List<Ranking.Scored> top = Ranking.top(catalog, candidate, score, options.k());
		List<Recommendation> items = new ArrayList<>();
		for (int n = 0; n < top.size(); n++) {
			Ranking.Scored scored = top.get(n);
			Explanation explanation = options.explain() ? explain(scored.ratingCount()) : null;
			items.add(new Recommendation(scored.movieId(), n + 1, scored.score(), explanation));
		}
		return new RecommendationResult(ID, items, Fallback.NONE);
	}

	private static Explanation explain(int ratingCount) {
		String ratings = ratingCount == 1 ? "1 rating" : ratingCount + " ratings";
		return new Explanation(List.of(), List.of(), "Popular with other viewers: " + ratings + ".");
	}
}
