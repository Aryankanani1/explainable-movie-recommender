package edu.fau.recsys.evaluation;

/**
 * The β search for cb-tags on the validation split (EVALUATION.md, "Don't tune on test").
 *
 * @param grid β values tried
 * @param validationNdcg NDCG@K on validation for each β
 * @param validationUsers users with at least one relevant validation movie
 * @param chosen the β with the highest validation NDCG (ties go to the smaller β)
 */
record BetaSearch(double[] grid, double[] validationNdcg, int validationUsers, double chosen) {
}
