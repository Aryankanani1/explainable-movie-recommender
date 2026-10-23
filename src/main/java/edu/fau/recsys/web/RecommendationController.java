package edu.fau.recsys.web;

import edu.fau.recsys.engine.MathTrace;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/recommendations")
class RecommendationController {

	private final RecommendationService recommendationService;

	RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	/**
	 * Ranked unseen movies; {@code method} is "lecture" or "improved", {@code k} defaults to
	 * {@code recsys.recommend.default-k}. {@code fallback: "POPULARITY"} when there is nothing to go on.
	 */
	@GetMapping
	RecommendationResultDto recommend(@RequestParam(defaultValue = "lecture") String method,
			@RequestParam(required = false) @Min(1) @Max(100) Integer k,
			@RequestParam(defaultValue = "0") @PositiveOrZero int minRatings) {
		return recommendationService.recommend(method, k, minRatings);
	}

	/** The slides' matrices for the current grades and the top {@code k} candidates. */
	@GetMapping("/math")
	MathTrace math(@RequestParam(defaultValue = "lecture") String method,
			@RequestParam(defaultValue = "5") @Min(1) @Max(20) int k) {
		return recommendationService.math(method, k);
	}
}
