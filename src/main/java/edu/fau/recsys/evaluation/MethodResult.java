package edu.fau.recsys.evaluation;

/**
 * Macro-averaged metrics for one method. Accuracy metrics (precision, recall, NDCG, hit rate) average over the users
 * with at least one relevant hidden movie; ILD and novelty over all users; coverage is over all users' lists.
 *
 * @param ndcgPerUser NDCG@K of each evaluated user, in userId order (for the paired bootstrap)
 */
record MethodResult(String method, int usersEvaluated, int usersSkipped, double precision, double recall,
		double ndcg, double hitRate, double coverage, double ild, double novelty, long runtimeMs,
		double[] ndcgPerUser) {
}
