package edu.fau.recsys.web;

import edu.fau.recsys.catalog.MovieSearch;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
class MovieController {

	private final MovieSearch movieSearch;

	MovieController(MovieSearch movieSearch) {
		this.movieSearch = movieSearch;
	}

	/** Searches display titles (case-insensitive contains), most-rated first. Invalid paging → 400 ProblemDetail. */
	@GetMapping
	PagedModel<MovieSummaryDto> search(@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "0") @PositiveOrZero int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return new PagedModel<>(movieSearch.search(q, page, size).map(MovieSummaryDto::from));
	}
}
