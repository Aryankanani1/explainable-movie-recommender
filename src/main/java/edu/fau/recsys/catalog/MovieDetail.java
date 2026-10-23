package edu.fau.recsys.catalog;

import java.math.BigDecimal;
import java.util.List;

/** Read model of one movie: genres in catalog order, its most-applied tags, and MovieLens rating stats. */
public record MovieDetail(int id, String title, Integer year, List<String> genres, List<TagCount> topTags,
		int ratingCount, BigDecimal ratingMean) {

	public MovieDetail {
		genres = List.copyOf(genres);
		topTags = List.copyOf(topTags);
	}

	/** A normalized tag and how many times MovieLens users applied it to this movie. */
	public record TagCount(String tag, int count) {
	}
}
