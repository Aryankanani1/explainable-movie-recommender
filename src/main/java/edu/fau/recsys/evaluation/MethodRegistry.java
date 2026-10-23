package edu.fau.recsys.evaluation;

import java.util.List;
import java.util.Locale;

import edu.fau.recsys.engine.ContentBasedRecommender;
import edu.fau.recsys.engine.LectureRecommender;
import edu.fau.recsys.engine.PopularityRecommender;
import edu.fau.recsys.engine.RandomRecommender;
import edu.fau.recsys.engine.ScoringConfig;

/**
 * The ablation ladder (EVALUATION.md): each row adds one change. Beyond the spec: cb-idf-mmr is the app's actual
 * improved method (genres only since ticket 05) with MMR, and cb-tags-tuned (added by the runner) is cb-tags with the
 * validation-chosen β.
 */
final class MethodRegistry {

	/** β values tried on the validation split. */
	static final double[] BETA_GRID = { 0, 0.1, 0.25, 0.5, 0.75, 1.0 };

	/** λ for the MMR rows (EVALUATION.md, row 8). */
	static final double MMR_LAMBDA = 0.7;

	private MethodRegistry() {
	}

	static List<EvaluatedMethod> ladder() {
		PopularityRecommender popularity = new PopularityRecommender();
		LectureRecommender lecture = new LectureRecommender();
		return List.of(
				new EvaluatedMethod("random", "RandomRecommender(42 + userId)",
						userId -> new RandomRecommender(UserSplit.SEED + userId)),
				new EvaluatedMethod("popularity", "PopularityRecommender (train counts)", userId -> popularity),
				new EvaluatedMethod("lecture", "LectureRecommender", userId -> lecture),
				contentBased("cb-cosine", new ScoringConfig(false, false, 0, 0)),
				contentBased("cb-centered", new ScoringConfig(true, false, 0, 0)),
				contentBased("cb-idf", new ScoringConfig(true, true, 0, 0)),
				diversified(contentBased("cb-idf-mmr", new ScoringConfig(true, true, 0, 0))),
				contentBased("cb-tags", new ScoringConfig(true, true, 0.5, 0)),
				diversified(contentBased("cb-tags-mmr", new ScoringConfig(true, true, 0.5, 0))),
				contentBased("cb-tags-pop", new ScoringConfig(true, true, 0.5, 0.1)));
	}

	/** cb-tags with β chosen on the validation split. */
	static EvaluatedMethod tunedTags(double beta) {
		return contentBased("cb-tags-tuned", new ScoringConfig(true, true, beta, 0));
	}

	/** The same method, re-ranked by MMR with λ = {@value #MMR_LAMBDA} over the top {@value Evaluator#MMR_POOL}. */
	static EvaluatedMethod diversified(EvaluatedMethod method) {
		return new EvaluatedMethod(method.id(),
				method.definition() + String.format(Locale.ROOT, " + MMR(lambda=%s, pool=%d)", MMR_LAMBDA, Evaluator.MMR_POOL),
				method.forUser(), MMR_LAMBDA);
	}

	static EvaluatedMethod contentBased(String id, ScoringConfig config) {
		ContentBasedRecommender recommender = new ContentBasedRecommender(config);
		String definition = String.format(Locale.ROOT, "ScoringConfig(center=%s, idf=%s, tagWeight=%s, prior=%s)",
				config.centerRatings(), config.useIdf(), config.tagWeight(), config.popularityPrior());
		return new EvaluatedMethod(id, definition, userId -> recommender);
	}
}
