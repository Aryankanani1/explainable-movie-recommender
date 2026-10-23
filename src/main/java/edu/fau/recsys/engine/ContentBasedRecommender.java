package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * The improved method (ALGORITHMS.md §3–4): centered weights, IDF-weighted and L2-normalized item vectors, cosine.
 * <pre>
 *   score(c) = cos(p, v_c) + popularityPrior · ln(1 + ratingCount_c) / ln(1 + maxRatingCount)
 * </pre>
 * with p from {@link ContentProfile}; cos is 0 when v_c is the zero vector. Scores can be negative: a movie can point
 * away from the user's taste. Falls back to popularity when ‖p‖ is (near) zero.
 */
public final class ContentBasedRecommender implements Recommender {

	public static final String ID = "content-based";

	private final ScoringConfig config;

	private final PopularityRecommender popularity = new PopularityRecommender();

	public ContentBasedRecommender(ScoringConfig config) {
		if (config.tagWeight() > 0) {
			throw new IllegalArgumentException("Tag features (tagWeight > 0) arrive in ticket 04");
		}
		this.config = config;
	}

	public ScoringConfig config() {
		return config;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public RecommendationResult recommend(CatalogSnapshot catalog, UserRatings user, RecommendOptions options) {
		double[][] v = catalog.itemVectors(config.useIdf(), config.tagWeight());
		ContentProfile profile = ContentProfile.of(catalog, user, config, v);
		if (profile.isEmpty()) {
			return new RecommendationResult(ID, popularity.recommend(catalog, user, options).items(),
					Fallback.POPULARITY);
		}

		List<Ranking.Scored> top = rank(catalog, user, profile, v, options.k(), options.minRatingCount());
		List<Recommendation> items = new ArrayList<>();
		for (int n = 0; n < top.size(); n++) {
			Ranking.Scored scored = top.get(n);
			// Content-based explanations (§7) arrive with the tag features in ticket 04.
			items.add(new Recommendation(scored.movieId(), n + 1, scored.score(), null));
		}
		return new RecommendationResult(ID, items, Fallback.NONE);
	}

	/** The improved method's tables for these grades and the top {@code k} candidates (ALGORITHMS.md §8). */
	public MathTrace trace(CatalogSnapshot catalog, UserRatings user, int k) {
		double[][] v = catalog.itemVectors(config.useIdf(), config.tagWeight());
		ContentProfile profile = ContentProfile.of(catalog, user, config, v);
		if (profile.isEmpty()) {
			return MathTrace.empty(ID);
		}
		List<Ranking.Scored> top = rank(catalog, user, profile, v, k, 0);
		double[][] x = catalog.genreRows();
		double[] p = profile.p();
		double pNorm = Vectors.norm(p);
		int G = catalog.genreNames().size();

		// Columns: genres of the graded movies plus every genre of a shown candidate, in catalog order.
		boolean[] graded = new boolean[G];
		for (int i : profile.graded()) {
			for (int g = 0; g < G; g++) {
				graded[g] = graded[g] || x[i][g] != 0;
			}
		}
		int[] columns = TraceColumns.select(catalog, graded, top);

		List<MathTrace.ContentRow> rows = new ArrayList<>();
		for (int n = 0; n < profile.graded().length; n++) {
			int i = profile.graded()[n];
			double w = profile.w()[n];
			double[] gHat = TraceColumns.pick(v[i], columns);
			double[] wGHat = new double[columns.length];
			for (int col = 0; col < columns.length; col++) {
				wGHat[col] = w * gHat[col];
			}
			rows.add(new MathTrace.ContentRow(catalog.movieId(i), catalog.title(i), profile.r()[n], w,
					TraceColumns.pick(x[i], columns), gHat, wGHat));
		}

		List<MathTrace.ContentCandidate> candidates = new ArrayList<>();
		for (Ranking.Scored candidate : top) {
			int c = candidate.idx();
			double vNorm = Vectors.norm(v[c]);
			// contrib_g(c) = p_g · v_cg / (‖p‖ · ‖v_c‖); summed over the genre block it is the genre part of the cosine.
			double[] contrib = new double[G];
			double genrePart = 0;
			for (int g = 0; g < G; g++) {
				contrib[g] = vNorm == 0 ? 0 : p[g] * v[c][g] / (pNorm * vNorm);
				genrePart += contrib[g];
			}
			candidates.add(new MathTrace.ContentCandidate(candidate.movieId(), catalog.title(c),
					TraceColumns.pick(v[c], columns), candidate.score(), genrePart, 0,
					TraceColumns.pick(contrib, columns)));
		}

		double[] idf = config.useIdf() ? TraceColumns.pick(catalog.genreIdf(), columns) : null;
		MathTrace.ContentBased content = new MathTrace.ContentBased(config, profile.rBar(), idf, rows,
				TraceColumns.pick(p, columns), candidates);
		return new MathTrace(ID, Fallback.NONE, TraceColumns.names(catalog, columns), null, content);
	}

	/** Scores every movie, then applies the shared candidate filter and order. */
	private List<Ranking.Scored> rank(CatalogSnapshot catalog, UserRatings user, ContentProfile profile, double[][] v,
			int k, int minRatingCount) {
		double[] p = profile.p();
		double pNorm = Vectors.norm(p);
		double logMaxRatingCount = Math.log(1 + catalog.maxRatingCount());
		double[] score = new double[catalog.size()];
		for (int c = 0; c < catalog.size(); c++) {
			double vNorm = Vectors.norm(v[c]);
			double cosine = vNorm == 0 ? 0 : Vectors.dot(p, v[c]) / (pNorm * vNorm);
			double prior = 0;
			if (config.popularityPrior() > 0 && logMaxRatingCount > 0) {
				prior = config.popularityPrior() * Math.log(1 + catalog.ratingCount(c)) / logMaxRatingCount;
			}
			score[c] = cosine + prior;
		}
		return Ranking.top(catalog, Ranking.candidates(catalog, user, minRatingCount), score, k);
	}
}
