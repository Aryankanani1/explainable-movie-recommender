package edu.fau.recsys.dataset;

import java.util.ArrayList;
import java.util.List;

/** Splits the pipe-separated {@code genres} column. {@code (no genres listed)} means zero genres. */
final class GenreParser {

	static final String NO_GENRES = "(no genres listed)";

	private GenreParser() {
	}

	static List<String> parse(String field) {
		String value = field.strip();
		if (value.isEmpty() || value.equals(NO_GENRES)) {
			return List.of();
		}
		List<String> genres = new ArrayList<>();
		for (String genre : value.split("\\|")) {
			String name = genre.strip();
			if (!name.isEmpty()) {
				genres.add(name);
			}
		}
		return List.copyOf(genres);
	}
}
