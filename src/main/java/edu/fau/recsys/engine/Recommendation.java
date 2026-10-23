package edu.fau.recsys.engine;

/** One ranked item. Ranks start at 1; {@code explanation} is null when it was not requested. */
public record Recommendation(int movieId, int rank, double score, Explanation explanation) {
}
