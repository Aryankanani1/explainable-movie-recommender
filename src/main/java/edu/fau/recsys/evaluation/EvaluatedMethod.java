package edu.fau.recsys.evaluation;

import java.util.function.IntFunction;

import edu.fau.recsys.engine.Recommender;

/**
 * One row of the ablation ladder.
 *
 * @param id short name used in the results, e.g. "cb-idf"
 * @param definition how the row is configured, for run-info.json
 * @param forUser the recommender to use for a user (only "random" needs a per-user seed)
 */
record EvaluatedMethod(String id, String definition, IntFunction<Recommender> forUser) {
}
