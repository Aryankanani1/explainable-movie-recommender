package edu.fau.recsys.catalog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Read model of a movie for search results. Genres are in catalog order. */
public record MovieSummary(int id, String title, Integer year, List<String> genres, int ratingCount,
		BigDecimal ratingMean) {

	public MovieSummary {
		genres = List.copyOf(genres);
	}

	static MovieSummary from(Movie movie) {
		List<String> genres = new ArrayList<>();
		for (Genre genre : movie.getGenres()) {
			genres.add(genre.getName());
		}
		return new MovieSummary(movie.getId(), movie.getDisplayTitle(), movie.getYear(), genres, movie.getRatingCount(),
				movie.getRatingMean());
	}
}
