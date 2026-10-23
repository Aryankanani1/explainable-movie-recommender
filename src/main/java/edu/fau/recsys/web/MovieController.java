package edu.fau.recsys.web;

import edu.fau.recsys.catalog.MovieCatalog;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/movies")
class MovieController {

	private static final String SIZE_RANGE = "must be between 1 and 100";

	private final MovieCatalog movieCatalog;

	MovieController(MovieCatalog movieCatalog) {
		this.movieCatalog = movieCatalog;
	}

	/** Searches display titles (case-insensitive contains), most-rated first. Invalid paging → 400 ProblemDetail. */
	@GetMapping
	PagedModel<MovieSummaryDto> search(@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "0") @PositiveOrZero(message = "must be 0 or more") int page,
			@RequestParam(defaultValue = "20") @Min(value = 1, message = SIZE_RANGE) @Max(value = 100,
					message = SIZE_RANGE) int size) {
		return new PagedModel<>(movieCatalog.search(q, page, size).map(MovieSummaryDto::from));
	}

	/** Genres, top tags and rating stats of one movie; an unknown ID → 404 ProblemDetail. */
	@GetMapping("/{id}")
	MovieDetailDto detail(@PathVariable int id) {
		return movieCatalog.detail(id)
			.map(MovieDetailDto::from)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No movie with id " + id));
	}
}
