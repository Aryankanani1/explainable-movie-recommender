package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The improved method (ALGORITHMS.md §3–4): centered weights, IDF-weighted genre and tag blocks, cosine.
 * <pre>
 *   score(c) = cos(p, v_c) + popularityPrior · ln(1 + ratingCount_c) / ln(1 + maxRatingCount)
 * </pre>
 * with p from {@link ContentProfile}; cos is 0 when v_c is the zero vector. Scores can be negative: a movie can point
 * away from the user's taste. Falls back to popularity when ‖p‖ is (near) zero.
 */
public final class ContentBasedRecommender implements Recommender {

	public static final String ID = "content-based";

	/** The trace summarizes the tag part of p by this many tags instead of drawing every column. */
	static final int TRACE_TOP_TAGS = 5;

	private final ScoringConfig config;

	private final PopularityRecommender popularity = new PopularityRecommender();

	public ContentBasedRecommender(ScoringConfig config) {
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
		ItemVectors v = catalog.itemVectors(config.useIdf(), config.tagWeight());
		ContentProfile profile = ContentProfile.of(catalog, user, config, v);
		if (profile.isEmpty()) {
			return new RecommendationResult(ID, popularity.recommend(catalog, user, options).items(),
					Fallback.POPULARITY);
		}

		List<Ranking.Scored> top = rank(catalog, user, profile, v, options.k(), options.minRatingCount());
		List<Recommendation> items = new ArrayList<>();
		for (int n = 0; n < top.size(); n++) {
			Ranking.Scored scored = top.get(n);
			Explanation explanation = options.explain() ? explain(catalog, user, profile, v, scored.idx()) : null;
			items.add(new Recommendation(scored.movieId(), n + 1, scored.score(), explanation));
		}
		return new RecommendationResult(ID, items, Fallback.NONE);
	}

	/** The improved method's tables for these grades and the top {@code k} candidates (ALGORITHMS.md §8). */
	public MathTrace trace(CatalogSnapshot catalog, UserRatings user, int k) {
		ItemVectors v = catalog.itemVectors(config.useIdf(), config.tagWeight());
		ContentProfile profile = ContentProfile.of(catalog, user, config, v);
		if (profile.isEmpty()) {
			return MathTrace.empty(ID);
		}
		List<Ranking.Scored> top = rank(catalog, user, profile, v, k, 0);
		double[][] x = catalog.genreRows();
		double[] p = profile.p();
		double pNorm = Vectors.norm(p);
		int G = v.genreDimensions();

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
			double[] gHat = TraceColumns.pick(v.genreRow(i), columns);
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
			double denominator = pNorm * v.norm(c);
			// contrib_f(c) = p_f · v_cf / (‖p‖ · ‖v_c‖); summed over a block it is that block's part of the cosine.
			double[] contrib = new double[G];
			double genrePart = 0;
			for (int g = 0; g < G; g++) {
				contrib[g] = denominator == 0 ? 0 : p[g] * v.genreRow(c)[g] / denominator;
				genrePart += contrib[g];
			}
			double tagPart = 0;
			for (int j = 0; j < v.tagIndices(c).length; j++) {
				tagPart += p[G + v.tagIndices(c)[j]] * v.tagValues(c)[j] / denominator;
			}
			candidates.add(new MathTrace.ContentCandidate(candidate.movieId(), catalog.title(c),
					TraceColumns.pick(v.genreRow(c), columns), candidate.score(), genrePart, tagPart,
					TraceColumns.pick(contrib, columns)));
		}

		double[] idf = config.useIdf() ? TraceColumns.pick(catalog.genreIdf(), columns) : null;
		MathTrace.ContentBased content = new MathTrace.ContentBased(config, profile.rBar(), idf, rows,
				TraceColumns.pick(p, columns), topTags(catalog, p, G), candidates);
		return new MathTrace(ID, Fallback.NONE, TraceColumns.names(catalog, columns), null, content);
	}

	/** Scores every movie, then applies the shared candidate filter and order. */
	private List<Ranking.Scored> rank(CatalogSnapshot catalog, UserRatings user, ContentProfile profile,
			ItemVectors v, int k, int minRatingCount) {
		double[] p = profile.p();
		double pNorm = Vectors.norm(p);
		double logMaxRatingCount = Math.log(1 + catalog.maxRatingCount());
		double[] score = new double[catalog.size()];
		for (int c = 0; c < catalog.size(); c++) {
			double vNorm = v.norm(c);
			double cosine = vNorm == 0 ? 0 : v.dot(p, c) / (pNorm * vNorm);
			double prior = 0;
			if (config.popularityPrior() > 0 && logMaxRatingCount > 0) {
				prior = config.popularityPrior() * Math.log(1 + catalog.ratingCount(c)) / logMaxRatingCount;
			}
			score[c] = cosine + prior;
		}
		return Ranking.top(catalog, Ranking.candidates(catalog, user, minRatingCount), score, k);
	}

	/**
	 * §7: contrib_f(c) = p_f · v_cf / (‖p‖ · ‖v_c‖) for every genre and tag of c, and
	 * influence_i = w_i · cos(v_i, v_c) for graded movies with w_i > 0 (movies the user liked more than usual).
	 */
	private static Explanation explain(CatalogSnapshot catalog, UserRatings user, ContentProfile profile,
			ItemVectors v, int c) {
		double[] p = profile.p();
		double denominator = Vectors.norm(p) * v.norm(c);
		int G = v.genreDimensions();

		List<FeatureContribution> contributions = new ArrayList<>();
		for (int g = 0; g < G; g++) {
			double contrib = denominator == 0 ? 0 : p[g] * v.genreRow(c)[g] / denominator;
			contributions.add(new FeatureContribution(catalog.genreNames().get(g), "genre", contrib));
		}
		for (int k = 0; k < v.tagIndices(c).length; k++) {
			int t = v.tagIndices(c)[k];
			double contrib = p[G + t] * v.tagValues(c)[k] / denominator;
			contributions.add(new FeatureContribution(catalog.tagVocabulary().get(t), "tag", contrib));
		}

		List<RatedInfluence> influences = new ArrayList<>();
		for (int n = 0; n < profile.graded().length; n++) {
			double w = profile.w()[n];
			if (w > 0) {
				int i = profile.graded()[n];
				double influence = w * v.cosine(i, c);
				influences.add(new RatedInfluence(catalog.movieId(i), catalog.title(i), profile.r()[n], influence));
			}
		}
		return Explanations.of(contributions, influences, user.scale());
	}

	/** The tag weights p_t with the largest |p_t|, largest first; ties keep vocabulary order. */
	private static List<MathTrace.TagWeight> topTags(CatalogSnapshot catalog, double[] p, int G) {
		List<MathTrace.TagWeight> tags = new ArrayList<>();
		for (int t = 0; G + t < p.length; t++) {
			if (p[G + t] != 0) {
				tags.add(new MathTrace.TagWeight(catalog.tagVocabulary().get(t), p[G + t]));
			}
		}
		tags.sort(Comparator.comparingDouble((MathTrace.TagWeight tag) -> Math.abs(tag.weight())).reversed());
		return tags.size() > TRACE_TOP_TAGS ? List.copyOf(tags.subList(0, TRACE_TOP_TAGS)) : List.copyOf(tags);
	}
}
