package edu.fau.recsys.web;

import java.util.List;

import edu.fau.recsys.catalog.StarterSetService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OnboardingController {

	private static final String SIZE_RANGE = "must be between 1 and 30";

	private final StarterSetService starterSet;

	OnboardingController(StarterSetService starterSet) {
		this.starterSet = starterSet;
	}

	/** Popular, well-liked films spread across genres, for users who have not graded anything yet. */
	@GetMapping("/api/onboarding/starter-set")
	List<MovieSummaryDto> starterSet(@RequestParam(defaultValue = "15") @Min(value = 1, message = SIZE_RANGE) @Max(
			value = 30, message = SIZE_RANGE) int size) {
		return starterSet.starterSet(size).stream().map(MovieSummaryDto::from).toList();
	}
}
