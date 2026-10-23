package edu.fau.recsys.dataset;

import java.util.List;

/** One row of {@code movies.csv}, with the title parsed and the genres split. */
public record MovieLensMovie(int movieId, String rawTitle, String displayTitle, Integer year, List<String> genres) {

	public MovieLensMovie {
		genres = List.copyOf(genres);
	}
}
