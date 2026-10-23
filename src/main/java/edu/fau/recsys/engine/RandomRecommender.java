package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Baseline (ALGORITHMS.md §5): shuffle the candidates with {@code new Random(seed)}, then score = 1 − rank / size
 * (ranks from 1). A sanity floor for the evaluation, which seeds it with 42 + userId.
 */
public final class RandomRecommender implements Recommender {

	public static final String ID = "random";

	private final long seed;

	public RandomRecommender(long seed) {
		this.seed = seed;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public RecommendationResult recommend(CatalogSnapshot catalog, UserRatings user, RecommendOptions options) {
		boolean[] candidate = Ranking.candidates(catalog, user, options.minRatingCount());
		List<Integer> shuffled = new ArrayList<>();
		for (int i = 0; i < catalog.size(); i++) {
			if (candidate[i]) {
				shuffled.add(i);
			}
		}
		Collections.shuffle(shuffled, new Random(seed));

		double[] score = new double[catalog.size()];
		int size = shuffled.size();
		for (int rank = 1; rank <= size; rank++) {
			score[shuffled.get(rank - 1)] = 1.0 - (double) rank / size;
		}

		List<Ranking.Scored> top = Ranking.top(catalog, candidate, score, options.k());
		List<Recommendation> items = new ArrayList<>();
		for (int n = 0; n < top.size(); n++) {
			Ranking.Scored scored = top.get(n);
			Explanation explanation = options.explain()
					? new Explanation(List.of(), List.of(), "Picked at random (a baseline for comparison).") : null;
			items.add(new Recommendation(scored.movieId(), n + 1, scored.score(), explanation));
		}
		return new RecommendationResult(ID, items, Fallback.NONE);
	}
}
