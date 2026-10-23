package edu.fau.recsys.web;

import edu.fau.recsys.engine.CatalogSnapshot;
import edu.fau.recsys.engine.LectureFixture;
import edu.fau.recsys.engine.LectureRecommender;
import edu.fau.recsys.engine.MathTrace;
import edu.fau.recsys.engine.RecommendOptions;
import edu.fau.recsys.engine.RecommendationResult;
import edu.fau.recsys.engine.UserRatings;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The lecture's worked example, computed live by the engine: 0.667 / 0.633 / 0.333 / Lady Bird 0. */
@RestController
class DemoController {

	private final LectureRecommender lecture = new LectureRecommender();

	private final CatalogSnapshot fixture = LectureFixture.catalog();

	@GetMapping("/api/demo/lecture-example")
	LectureExampleDto lectureExample() {
		UserRatings grades = LectureFixture.defaultGrades();
		int allCandidates = fixture.size() - grades.byMovieId().size();
		RecommendationResult result = lecture.recommend(fixture, grades,
				new RecommendOptions(allCandidates, 0, 1.0, true));
		return new LectureExampleDto(RecommendationResultDto.from(result, fixture, grades.byMovieId().size()),
				lecture.trace(fixture, grades, allCandidates));
	}

	record LectureExampleDto(RecommendationResultDto recommendations, MathTrace math) {
	}
}
