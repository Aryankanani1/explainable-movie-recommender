package edu.fau.recsys.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import edu.fau.recsys.catalog.MovieDetail;

/** One movie for the detail view; {@code ratingMean} is null for movies nobody rated. */
record MovieDetailDto(int id, String title, Integer year, List<String> genres, List<TagDto> topTags, int ratingCount,
		BigDecimal ratingMean) {

	static MovieDetailDto from(MovieDetail movie) {
		List<TagDto> tags = new ArrayList<>();
		for (MovieDetail.TagCount tag : movie.topTags()) {
			tags.add(new TagDto(tag.tag(), tag.count()));
		}
		return new MovieDetailDto(movie.id(), movie.title(), movie.year(), movie.genres(), tags, movie.ratingCount(),
				movie.ratingMean());
	}

	record TagDto(String tag, int count) {
	}
}
