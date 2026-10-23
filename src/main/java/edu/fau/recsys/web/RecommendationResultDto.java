package edu.fau.recsys.web;

import java.util.ArrayList;
import java.util.List;

import edu.fau.recsys.engine.CatalogSnapshot;
import edu.fau.recsys.engine.Fallback;
import edu.fau.recsys.engine.Recommendation;
import edu.fau.recsys.engine.RecommendationResult;
import edu.fau.recsys.infra.LatencySummary;

/**
 * {@code fallback: "POPULARITY"} tells the UI to show the "grade a few movies first" banner; {@code gradeCount} is
 * how many grades the list was built from.
 */
record RecommendationResultDto(String method, Fallback fallback, int gradeCount, List<RecommendationDto> items)
		implements LatencySummary {

	@Override
	public String latencySummary() {
		return "grades=" + gradeCount + " fallback=" + fallback + " items=" + items.size();
	}

	static RecommendationResultDto from(RecommendationResult result, CatalogSnapshot catalog, int gradeCount) {
		List<RecommendationDto> items = new ArrayList<>();
		for (Recommendation item : result.items()) {
			int idx = catalog.indexOf(item.movieId());
			items.add(new RecommendationDto(item.movieId(), item.rank(), catalog.title(idx), catalog.year(idx),
					catalog.genres(idx), item.score(), item.explanation()));
		}
		return new RecommendationResultDto(result.method(), result.fallback(), gradeCount, items);
	}
}
