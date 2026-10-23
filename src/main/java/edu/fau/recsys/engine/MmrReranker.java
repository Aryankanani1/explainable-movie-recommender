package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Diversity re-ranking with Maximal Marginal Relevance (ALGORITHMS.md §6). Builds the list one pick at a time:
 * <pre>
 *   rel_i    = (score_i − min_pool) / (max_pool − min_pool)        (all 1 if max = min)
 *   sim(i,j) = ĝ_i · ĝ_j          (IDF-weighted, L2-normalized genre block, whatever the recommender used)
 *   pick c maximizing   λ · rel_c − (1 − λ) · max_{s ∈ picked} sim(c, s)       (the max is 0 before any pick)
 * </pre>
 * Ties use the shared order on the original score. The reported score stays the original relevance score; the rank is
 * the MMR position. λ = 1 returns the input order unchanged.
 */
public final class MmrReranker {

	/** Re-ranks {@code pool} (sorted by the shared order, as every recommender returns it) down to {@code k} items. */
	public List<Recommendation> rerank(CatalogSnapshot catalog, List<Recommendation> pool, int k, double lambda) {
		List<Pick> picks = picks(catalog, pool, k, lambda);
		List<Recommendation> reranked = new ArrayList<>();
		for (int n = 0; n < picks.size(); n++) {
			Recommendation item = picks.get(n).item();
			reranked.add(new Recommendation(item.movieId(), n + 1, item.score(), item.explanation()));
		}
		return List.copyOf(reranked);
	}

	/**
	 * Asks {@code recommender} for its top {@code poolSize}, then re-ranks to {@code options.k()} with
	 * {@code options.mmrLambda()}. With λ = 1 it is exactly {@code recommender.recommend(...)}.
	 */
	public RecommendationResult diversify(Recommender recommender, CatalogSnapshot catalog, UserRatings user,
			RecommendOptions options, int poolSize) {
		if (options.mmrLambda() >= 1.0) {
			return recommender.recommend(catalog, user, options);
		}
		RecommendOptions poolOptions = new RecommendOptions(Math.max(poolSize, options.k()), options.minRatingCount(),
				1.0, options.explain());
		RecommendationResult pool = recommender.recommend(catalog, user, poolOptions);
		return new RecommendationResult(pool.method(), rerank(catalog, pool.items(), options.k(), options.mmrLambda()),
				pool.fallback());
	}

	/** The greedy selection, with each pick's MMR value (for tests and traces). */
	List<Pick> picks(CatalogSnapshot catalog, List<Recommendation> pool, int k, double lambda) {
		int M = pool.size();
		double[] rel = relevance(pool);
		ItemVectors gHat = catalog.itemVectors(true, 0);
		int[] idx = new int[M];
		Ranking.Scored[] order = new Ranking.Scored[M];
		for (int c = 0; c < M; c++) {
			idx[c] = catalog.indexOf(pool.get(c).movieId());
			order[c] = new Ranking.Scored(idx[c], pool.get(c).movieId(), catalog.ratingCount(idx[c]), pool.get(c).score());
		}

		boolean[] picked = new boolean[M];
		// maxSim[c] = max over picked s of sim(c, s); 0 before the first pick.
		double[] maxSim = new double[M];
		List<Pick> picks = new ArrayList<>();
		for (int step = 0; step < Math.min(k, M); step++) {
			int best = -1;
			double bestMmr = 0;
			for (int c = 0; c < M; c++) {
				if (picked[c]) {
					continue;
				}
				double mmr = lambda * rel[c] - (1 - lambda) * maxSim[c];
				if (best < 0 || mmr > bestMmr || (mmr == bestMmr && Ranking.ORDER.compare(order[c], order[best]) < 0)) {
					best = c;
					bestMmr = mmr;
				}
			}
			picked[best] = true;
			picks.add(new Pick(pool.get(best), bestMmr));
			for (int c = 0; c < M; c++) {
				if (!picked[c]) {
					maxSim[c] = Math.max(maxSim[c], Vectors.dot(gHat.genreRow(idx[c]), gHat.genreRow(idx[best])));
				}
			}
		}
		return picks;
	}

	/** rel_i = (score_i − min) / (max − min) over the pool; all 1 when every score is equal. */
	static double[] relevance(List<Recommendation> pool) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (Recommendation item : pool) {
			min = Math.min(min, item.score());
			max = Math.max(max, item.score());
		}
		double[] rel = new double[pool.size()];
		for (int c = 0; c < rel.length; c++) {
			rel[c] = max == min ? 1.0 : (pool.get(c).score() - min) / (max - min);
		}
		return rel;
	}

	/** sim(a, b) = ĝ_a · ĝ_b on the IDF-weighted, L2-normalized genre block. */
	static double similarity(CatalogSnapshot catalog, int movieA, int movieB) {
		ItemVectors gHat = catalog.itemVectors(true, 0);
		return Vectors.dot(gHat.genreRow(catalog.indexOf(movieA)), gHat.genreRow(catalog.indexOf(movieB)));
	}

	record Pick(Recommendation item, double mmr) {
	}
}
