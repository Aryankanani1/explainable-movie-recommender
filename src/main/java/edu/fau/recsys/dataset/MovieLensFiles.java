package edu.fau.recsys.dataset;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * The four CSV files of a MovieLens download such as {@code ml-latest-small}.
 * Each {@code forEach…} method streams the file row by row and closes it before returning.
 */
public record MovieLensFiles(Path dir) {

	/** RFC 4180 with a header row; handles quoted commas such as {@code "Matrix, The (1999)"}. */
	private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
			.setHeader()
			.setSkipHeaderRecord(true)
			.get();

	public Path movies() {
		return dir.resolve("movies.csv");
	}

	public Path ratings() {
		return dir.resolve("ratings.csv");
	}

	public Path tags() {
		return dir.resolve("tags.csv");
	}

	public Path links() {
		return dir.resolve("links.csv");
	}

	public void forEachMovie(Consumer<? super MovieLensMovie> sink) {
		read(movies(), MovieLensFiles::toMovie, sink);
	}

	public void forEachRating(Consumer<? super MovieLensRating> sink) {
		read(ratings(), MovieLensFiles::toRating, sink);
	}

	public void forEachTag(Consumer<? super MovieLensTag> sink) {
		read(tags(), MovieLensFiles::toTag, sink);
	}

	public void forEachLink(Consumer<? super MovieLensLink> sink) {
		read(links(), MovieLensFiles::toLink, sink);
	}

	/** Reads {@code tags.csv} and aggregates it to (movie, normalized tag) → count. */
	public TagCounts tagCounts() {
		SortedMap<Integer, SortedMap<String, Integer>> byMovie = new TreeMap<>();
		int[] applications = {0};
		forEachTag(row -> {
			String tag = normalizeTag(row.tag());
			if (!tag.isEmpty()) {
				byMovie.computeIfAbsent(row.movieId(), id -> new TreeMap<>()).merge(tag, 1, Integer::sum);
			}
			applications[0]++;
		});
		for (Map.Entry<Integer, SortedMap<String, Integer>> entry : byMovie.entrySet()) {
			entry.setValue(Collections.unmodifiableSortedMap(entry.getValue()));
		}
		return new TagCounts(applications[0], Collections.unmodifiableSortedMap(byMovie));
	}

	/** Tags are free text with inconsistent case and spacing: "Funny ", "funny" and "FUNNY" are one tag. */
	public static String normalizeTag(String tag) {
		return tag.strip().toLowerCase(Locale.ROOT);
	}

	private static MovieLensMovie toMovie(CSVRecord row) {
		String rawTitle = row.get("title");
		ParsedTitle title = TitleParser.parse(rawTitle);
		return new MovieLensMovie(Integer.parseInt(row.get("movieId")), rawTitle, title.displayTitle(), title.year(),
				GenreParser.parse(row.get("genres")));
	}

	private static MovieLensRating toRating(CSVRecord row) {
		return new MovieLensRating(Integer.parseInt(row.get("userId")), Integer.parseInt(row.get("movieId")),
				Double.parseDouble(row.get("rating")), Long.parseLong(row.get("timestamp")));
	}

	private static MovieLensTag toTag(CSVRecord row) {
		return new MovieLensTag(Integer.parseInt(row.get("userId")), Integer.parseInt(row.get("movieId")),
				row.get("tag"), Long.parseLong(row.get("timestamp")));
	}

	private static MovieLensLink toLink(CSVRecord row) {
		String tmdbId = row.get("tmdbId").strip();
		return new MovieLensLink(Integer.parseInt(row.get("movieId")), row.get("imdbId"),
				tmdbId.isEmpty() ? null : Integer.valueOf(tmdbId));
	}

	private static <T> void read(Path file, Function<CSVRecord, T> mapper, Consumer<? super T> sink) {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
				CSVParser parser = FORMAT.parse(reader)) {
			for (CSVRecord row : parser) {
				sink.accept(map(file, row, mapper));
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot read " + file, ex);
		}
	}

	/** Wraps parse errors with the file and row number; errors thrown by the sink pass through unchanged. */
	private static <T> T map(Path file, CSVRecord row, Function<CSVRecord, T> mapper) {
		try {
			return mapper.apply(row);
		}
		catch (RuntimeException ex) {
			throw new IllegalArgumentException("Malformed row " + row.getRecordNumber() + " in " + file, ex);
		}
	}
}
