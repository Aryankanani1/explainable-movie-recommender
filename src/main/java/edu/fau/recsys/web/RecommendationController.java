package edu.fau.recsys.web;

import edu.fau.recsys.engine.MathTrace;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

	private static final String K_RANGE = "must be between 1 and 100";

	private static final String MATH_K_RANGE = "must be between 1 and 20";

	private static final String LAMBDA_RANGE = "must be between 0 and 1";

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
			@RequestParam(required = false) @Min(value = 1, message = K_RANGE) @Max(value = 100,
					message = K_RANGE) Integer k,
			@RequestParam(defaultValue = "0") @PositiveOrZero(message = "must be 0 or more") int minRatings,
			@RequestParam(defaultValue = "1.0") @DecimalMin(value = "0.0", message = LAMBDA_RANGE) @DecimalMax(
					value = "1.0", message = LAMBDA_RANGE) double lambda) {
		return recommendationService.recommend(method, k, minRatings, lambda);
	}

	/** The slides' matrices for the current grades and the top {@code k} candidates. */
	@GetMapping("/math")
	MathTrace math(@RequestParam(defaultValue = "lecture") String method,
			@RequestParam(defaultValue = "5") @Min(value = 1, message = MATH_K_RANGE) @Max(value = 20,
					message = MATH_K_RANGE) int k) {
		return recommendationService.math(method, k);
	}
}
