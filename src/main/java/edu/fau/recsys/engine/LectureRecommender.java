package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * The professor's method, exactly as on the slides (ALGORITHMS.md §2):
 * <pre>
 *   score(c) = Σ_g p̂_g · x_cg      the "weighted average"; the slides also show it × 10
 * </pre>
 * with p̂ from {@link LectureProfile}. Falls back to popularity when the profile is empty or zero.
 */
public final class LectureRecommender implements Recommender {

	public static final String ID = "lecture";

	private final PopularityRecommender popularity = new PopularityRecommender();

	@Override
	public String id() {
		return ID;
	}

	@Override
	public RecommendationResult recommend(CatalogSnapshot catalog, UserRatings user, RecommendOptions options) {
		LectureProfile profile = LectureProfile.of(catalog, user);
		if (profile.isEmpty()) {
			return new RecommendationResult(ID, popularity.recommend(catalog, user, options).items(),
					Fallback.POPULARITY);
		}

		List<Ranking.Scored> top = rank(catalog, user, profile, options.k(), options.minRatingCount());
		List<Recommendation> items = new ArrayList<>();
		for (int n = 0; n < top.size(); n++) {
			Ranking.Scored scored = top.get(n);
			Explanation explanation = options.explain() ? explain(catalog, user, profile, scored.idx()) : null;
			items.add(new Recommendation(scored.movieId(), n + 1, scored.score(), explanation));
		}
		return new RecommendationResult(ID, items, Fallback.NONE);
	}

	/** The slides' matrices for these grades and the top {@code k} candidates (ALGORITHMS.md §8). */
	public MathTrace trace(CatalogSnapshot catalog, UserRatings user, int k) {
		LectureProfile profile = LectureProfile.of(catalog, user);
		if (profile.isEmpty()) {
			return MathTrace.empty(ID);
		}
		List<Ranking.Scored> top = rank(catalog, user, profile, k, 0);
		double[][] x = catalog.genreRows();
		double[] P = profile.P();
		double[] pHat = profile.pHat();

		// Columns: genres in the profile (P_g ≠ 0) plus every genre of a shown candidate, in catalog order.
		int G = P.length;
		boolean[] shown = new boolean[G];
		for (int g = 0; g < G; g++) {
			shown[g] = P[g] != 0;
		}
		for (Ranking.Scored candidate : top) {
			for (int g = 0; g < G; g++) {
				shown[g] = shown[g] || x[candidate.idx()][g] != 0;
			}
		}
		int shownCount = 0;
		for (int g = 0; g < G; g++) {
			if (shown[g]) {
				shownCount++;
			}
		}
		int[] columns = new int[shownCount];
		List<String> columnNames = new ArrayList<>();
		int j = 0;
		for (int g = 0; g < G; g++) {
			if (shown[g]) {
				columns[j++] = g;
				columnNames.add(catalog.genreNames().get(g));
			}
		}

		List<MathTrace.GradedRow> rows = new ArrayList<>();
		for (int n = 0; n < profile.graded().length; n++) {
			int i = profile.graded()[n];
			double r = profile.r()[n];
			double[] xRow = pick(x[i], columns);
			double[] weighted = new double[columns.length];
			for (int col = 0; col < columns.length; col++) {
				weighted[col] = r * xRow[col];
			}
			rows.add(new MathTrace.GradedRow(catalog.movieId(i), catalog.title(i), r, xRow, weighted));
		}

		List<MathTrace.CandidateRow> candidates = new ArrayList<>();
		for (Ranking.Scored candidate : top) {
			int c = candidate.idx();
			double[] contrib = new double[columns.length];
			for (int col = 0; col < columns.length; col++) {
				contrib[col] = pHat[columns[col]] * x[c][columns[col]];
			}
			candidates.add(new MathTrace.CandidateRow(candidate.movieId(), catalog.title(c), pick(x[c], columns),
					candidate.score(), candidate.score() * 10, contrib));
		}

		MathTrace.Profile traceProfile = new MathTrace.Profile(pick(P, columns), profile.T(), pick(pHat, columns));
		return new MathTrace(ID, Fallback.NONE, columnNames, rows, traceProfile, candidates);
	}

	/** score(c) = Σ_g p̂_g · x_cg for every movie, then the shared candidate filter and order. */
	private static List<Ranking.Scored> rank(CatalogSnapshot catalog, UserRatings user, LectureProfile profile, int k,
			int minRatingCount) {
		double[][] x = catalog.genreRows();
		double[] pHat = profile.pHat();
		double[] score = new double[catalog.size()];
		for (int c = 0; c < catalog.size(); c++) {
			double s = 0;
			for (int g = 0; g < pHat.length; g++) {
				s += pHat[g] * x[c][g];
			}
			score[c] = s;
		}
		return Ranking.top(catalog, Ranking.candidates(catalog, user, minRatingCount), score, k);
	}

	/**
	 * §7 for the lecture method: contrib_g(c) = p̂_g · x_cg, and influence_i = r_i · cos(x_i, x_c) on raw 0/1 rows.
	 */
	private static Explanation explain(CatalogSnapshot catalog, UserRatings user, LectureProfile profile, int c) {
		double[][] x = catalog.genreRows();
		List<String> genreNames = catalog.genreNames();

		List<FeatureContribution> contributions = new ArrayList<>();
		for (int g = 0; g < genreNames.size(); g++) {
			double contrib = profile.pHat()[g] * x[c][g];
			contributions.add(new FeatureContribution(genreNames.get(g), "genre", contrib));
		}

		List<RatedInfluence> influences = new ArrayList<>();
		for (int n = 0; n < profile.graded().length; n++) {
			int i = profile.graded()[n];
			double r = profile.r()[n];
			double influence = r * Vectors.cosine(x[i], x[c]);
			influences.add(new RatedInfluence(catalog.movieId(i), catalog.title(i), r, influence));
		}
		return Explanations.of(contributions, influences, user.scale());
	}

	private static double[] pick(double[] full, int[] columns) {
		double[] picked = new double[columns.length];
		for (int j = 0; j < columns.length; j++) {
			picked[j] = full[columns[j]];
		}
		return picked;
	}
}
