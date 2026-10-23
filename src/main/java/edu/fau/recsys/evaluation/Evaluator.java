package edu.fau.recsys.evaluation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.fau.recsys.dataset.MovieLensRating;
import edu.fau.recsys.engine.CatalogSnapshot;
import edu.fau.recsys.engine.MmrReranker;
import edu.fau.recsys.engine.RatingScale;
import edu.fau.recsys.engine.RecommendOptions;
import edu.fau.recsys.engine.Recommendation;
import edu.fau.recsys.engine.UserRatings;

/**
 * Runs one method over every user (EVALUATION.md, protocol steps 5–7): the profile is built from train ratings only
 * (scale 0.5–5.0), candidates are all movies not in train, and a hidden movie is relevant when rated at least
 * {@code relevanceThreshold}.
 */
final class Evaluator {

	/** Candidates MMR re-ranks (ALGORITHMS.md §6, M), the same default as the app. */
	static final int MMR_POOL = 100;

	private final MmrReranker mmr = new MmrReranker();

	private final CatalogSnapshot catalog;

	private final List<UserSplit> splits;

	private final int k;

	private final double relevanceThreshold;

	Evaluator(CatalogSnapshot catalog, List<UserSplit> splits, int k, double relevanceThreshold) {
		this.catalog = catalog;
		this.splits = List.copyOf(splits);
		this.k = k;
		this.relevanceThreshold = relevanceThreshold;
	}

	MethodResult run(EvaluatedMethod method) {
		long start = System.nanoTime();
		RecommendOptions options = new RecommendOptions(k, 0, method.mmrLambda(), false);
		int users = splits.size();

		double precision = 0;
		double recall = 0;
		double ndcg = 0;
		double hitRate = 0;
		double ild = 0;
		double novelty = 0;
		int evaluated = 0;
		List<Double> ndcgPerUser = new ArrayList<>();
		List<List<Integer>> lists = new ArrayList<>();

		for (UserSplit split : splits) {
			List<Integer> top = new ArrayList<>();
			for (Recommendation item : mmr
				.diversify(method.forUser().apply(split.userId()), catalog, userRatings(split.train()), options, MMR_POOL)
				.items()) {
				top.add(item.movieId());
			}
			lists.add(top);
			ild += Metrics.ild(top, catalog);
			novelty += Metrics.novelty(top, catalog, users);

			Set<Integer> relevant = relevant(split.test());
			if (relevant.isEmpty()) {
				continue;
			}
			evaluated++;
			precision += Metrics.precision(top, relevant, k);
			recall += Metrics.recall(top, relevant, k);
			double userNdcg = Metrics.ndcg(top, relevant, k);
			ndcg += userNdcg;
			ndcgPerUser.add(userNdcg);
			hitRate += Metrics.hitRate(top, relevant, k);
		}

		double[] perUser = new double[ndcgPerUser.size()];
		for (int n = 0; n < perUser.length; n++) {
			perUser[n] = ndcgPerUser.get(n);
		}
		long runtimeMs = (System.nanoTime() - start) / 1_000_000;
		return new MethodResult(method.id(), evaluated, users - evaluated, mean(precision, evaluated),
				mean(recall, evaluated), mean(ndcg, evaluated), mean(hitRate, evaluated),
				Metrics.coverage(lists, catalog.size()), mean(ild, users), mean(novelty, users), runtimeMs, perUser);
	}

	private Set<Integer> relevant(List<MovieLensRating> hidden) {
		Set<Integer> relevant = new HashSet<>();
		for (MovieLensRating rating : hidden) {
			if (rating.rating() >= relevanceThreshold) {
				relevant.add(rating.movieId());
			}
		}
		return relevant;
	}

	private static UserRatings userRatings(List<MovieLensRating> train) {
		Map<Integer, Double> byMovieId = new TreeMap<>();
		for (MovieLensRating rating : train) {
			byMovieId.put(rating.movieId(), rating.rating());
		}
		return new UserRatings(byMovieId, RatingScale.MOVIELENS_STARS);
	}

	private static double mean(double sum, int count) {
		return count == 0 ? 0 : sum / count;
	}
}
