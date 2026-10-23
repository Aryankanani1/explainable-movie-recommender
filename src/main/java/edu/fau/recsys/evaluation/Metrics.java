package edu.fau.recsys.evaluation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.fau.recsys.engine.CatalogSnapshot;

/** The ranking metrics of EVALUATION.md. Positions are 1-based; only the first {@code k} items of a list count. */
final class Metrics {

	private Metrics() {
	}

	static int hits(List<Integer> recommended, Set<Integer> relevant, int k) {
		int hits = 0;
		for (int i = 0; i < Math.min(k, recommended.size()); i++) {
			if (relevant.contains(recommended.get(i))) {
				hits++;
			}
		}
		return hits;
	}

	/** Hits in top K / K. */
	static double precision(List<Integer> recommended, Set<Integer> relevant, int k) {
		return (double) hits(recommended, relevant, k) / k;
	}

	/** Hits in top K / number of relevant items. */
	static double recall(List<Integer> recommended, Set<Integer> relevant, int k) {
		return relevant.isEmpty() ? 0 : (double) hits(recommended, relevant, k) / relevant.size();
	}

	/** 1 if there is at least one hit in the top K. */
	static double hitRate(List<Integer> recommended, Set<Integer> relevant, int k) {
		return hits(recommended, relevant, k) > 0 ? 1 : 0;
	}

	/** DCG = Σ_{i≤K} rel_i / log₂(i + 1), binary relevance. */
	static double dcg(List<Integer> recommended, Set<Integer> relevant, int k) {
		double dcg = 0;
		for (int i = 1; i <= Math.min(k, recommended.size()); i++) {
			if (relevant.contains(recommended.get(i - 1))) {
				dcg += 1 / log2(i + 1);
			}
		}
		return dcg;
	}

	/** The DCG of the ideal list: min(K, |relevant|) hits on top. */
	static double idealDcg(int relevantCount, int k) {
		double idcg = 0;
		for (int i = 1; i <= Math.min(k, relevantCount); i++) {
			idcg += 1 / log2(i + 1);
		}
		return idcg;
	}

	static double ndcg(List<Integer> recommended, Set<Integer> relevant, int k) {
		double idcg = idealDcg(relevant.size(), k);
		return idcg == 0 ? 0 : dcg(recommended, relevant, k) / idcg;
	}

	/** |∪ of the lists| / N. */
	static double coverage(Collection<? extends Collection<Integer>> lists, int catalogSize) {
		Set<Integer> union = new HashSet<>();
		for (Collection<Integer> list : lists) {
			union.addAll(list);
		}
		return (double) union.size() / catalogSize;
	}

	/**
	 * Intra-list diversity: 1 − mean pairwise cosine of the raw 0/1 genre rows, the same for every method. For 0/1
	 * rows the cosine is |shared genres| / √(|A|·|B|); a movie without genres has similarity 0. Fewer than two items
	 * have no pairs, so their diversity is 0.
	 */
	static double ild(List<Integer> list, CatalogSnapshot catalog) {
		if (list.size() < 2) {
			return 0;
		}
		double sum = 0;
		int pairs = 0;
		for (int a = 0; a < list.size(); a++) {
			List<String> genresA = catalog.genres(catalog.indexOf(list.get(a)));
			for (int b = a + 1; b < list.size(); b++) {
				List<String> genresB = catalog.genres(catalog.indexOf(list.get(b)));
				sum += rawCosine(genresA, genresB);
				pairs++;
			}
		}
		return 1 - sum / pairs;
	}

	/** Mean over the list of −log₂((ratingCount_train + 1) / (users + 1)): higher means less obvious picks. */
	static double novelty(List<Integer> list, CatalogSnapshot catalog, int users) {
		if (list.isEmpty()) {
			return 0;
		}
		double sum = 0;
		for (int movieId : list) {
			int ratingCount = catalog.ratingCount(catalog.indexOf(movieId));
			sum += -log2((ratingCount + 1.0) / (users + 1.0));
		}
		return sum / list.size();
	}

	private static double rawCosine(List<String> a, List<String> b) {
		if (a.isEmpty() || b.isEmpty()) {
			return 0;
		}
		int shared = 0;
		for (String genre : a) {
			if (b.contains(genre)) {
				shared++;
			}
		}
		return shared / Math.sqrt((double) a.size() * b.size());
	}

	private static double log2(double x) {
		return Math.log(x) / Math.log(2);
	}
}
