package edu.fau.recsys.web;

import java.math.BigDecimal;
import java.util.List;

import edu.fau.recsys.catalog.MovieSummary;

/** A search result. {@code title} is the display title; {@code ratingMean} is null for unrated movies. */
record MovieSummaryDto(int id, String title, Integer year, List<String> genres, int ratingCount,
		BigDecimal ratingMean) {

	static MovieSummaryDto from(MovieSummary movie) {
		return new MovieSummaryDto(movie.id(), movie.title(), movie.year(), movie.genres(), movie.ratingCount(),
				movie.ratingMean());
	}
}
