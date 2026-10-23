package edu.fau.recsys.web;

import java.util.List;

import edu.fau.recsys.engine.Explanation;

/** One recommended movie, enriched with what the UI shows. {@code explanation} is the engine's record as-is. */
record RecommendationDto(int movieId, int rank, String title, Integer year, List<String> genres, double score,
		Explanation explanation) {
}
