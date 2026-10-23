package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Candidate selection and the one ordering rule every recommender uses (ALGORITHMS.md §1). */
final class Ranking {

	/** Score desc, then ratingCount desc, then movieId asc. */
	static final Comparator<Scored> ORDER = (a, b) -> {
		int byScore = Double.compare(b.score(), a.score());
		if (byScore != 0) {
			return byScore;
		}
		int byRatingCount = Integer.compare(b.ratingCount(), a.ratingCount());
		if (byRatingCount != 0) {
			return byRatingCount;
		}
		return Integer.compare(a.movieId(), b.movieId());
	};

	private Ranking() {
	}

	/** A movie not rated by the user, with at least {@code minRatingCount} ratings. Unknown rated IDs are ignored. */
	static boolean[] candidates(CatalogSnapshot catalog, UserRatings user, int minRatingCount) {
		boolean[] candidate = new boolean[catalog.size()];
		for (int i = 0; i < catalog.size(); i++) {
			candidate[i] = catalog.ratingCount(i) >= minRatingCount;
		}
		for (int movieId : user.byMovieId().keySet()) {
			int i = catalog.indexOf(movieId);
			if (i >= 0) {
				candidate[i] = false;
			}
		}
		return candidate;
	}

	/** The first {@code k} candidates by {@link #ORDER}, given a score for every catalog index. */
	static List<Scored> top(CatalogSnapshot catalog, boolean[] candidate, double[] score, int k) {
		List<Scored> scored = new ArrayList<>();
		for (int i = 0; i < catalog.size(); i++) {
			if (candidate[i]) {
				scored.add(new Scored(i, catalog.movieId(i), catalog.ratingCount(i), score[i]));
			}
		}
		scored.sort(ORDER);
		return scored.size() > k ? List.copyOf(scored.subList(0, k)) : List.copyOf(scored);
	}

	record Scored(int idx, int movieId, int ratingCount, double score) {

		Scored {
			if (Double.isNaN(score)) {
				throw new IllegalStateException("NaN score for movie " + movieId);
			}
			// -0.0 + 0.0 is +0.0, so a "negative zero" can never break a tie differently from zero.
			score = score + 0.0;
		}
	}
}
