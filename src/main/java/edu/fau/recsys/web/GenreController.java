package edu.fau.recsys.web;

import java.util.List;

import edu.fau.recsys.catalog.GenreCount;
import edu.fau.recsys.catalog.MovieCatalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class GenreController {

	private final MovieCatalog movieCatalog;

	GenreController(MovieCatalog movieCatalog) {
		this.movieCatalog = movieCatalog;
	}

	/** Genre names with how many movies have each, in catalog order. */
	@GetMapping("/api/genres")
	List<GenreDto> genres() {
		return movieCatalog.genres().stream().map(GenreDto::from).toList();
	}

	record GenreDto(String name, int movieCount) {

		static GenreDto from(GenreCount genre) {
			return new GenreDto(genre.name(), genre.movieCount());
		}
	}
}
