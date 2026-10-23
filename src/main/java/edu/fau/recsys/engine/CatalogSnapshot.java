package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Immutable, in-memory view of the catalog that every recommender scores against.
 * Movies are indexed 0..N-1 in ascending movieId order; genres 0..G-1 in catalog order.
 */
public final class CatalogSnapshot {

	private final int[] movieIds;

	private final String[] titles;

	private final Integer[] years;

	private final int[] ratingCounts;

	private final int maxRatingCount;

	private final List<String> genreNames;

	/** {@code x[i][g]} = 1 if movie i has genre g, else 0. */
	private final double[][] genreRows;

	/** Normalized tag → number of applications, per movie (used from ticket 04). */
	private final List<SortedMap<String, Integer>> tagCounts;

	private CatalogSnapshot(int[] movieIds, String[] titles, Integer[] years, int[] ratingCounts,
			List<String> genreNames, double[][] genreRows, List<SortedMap<String, Integer>> tagCounts) {
		this.movieIds = movieIds;
		this.titles = titles;
		this.years = years;
		this.ratingCounts = ratingCounts;
		this.genreNames = List.copyOf(genreNames);
		this.genreRows = genreRows;
		this.tagCounts = List.copyOf(tagCounts);
		int max = 0;
		for (int count : ratingCounts) {
			max = Math.max(max, count);
		}
		this.maxRatingCount = max;
	}

	public static Builder builder() {
		return new Builder();
	}

	public int size() {
		return movieIds.length;
	}

	/** The index of a movie, or -1 if it is not in the catalog. */
	public int indexOf(int movieId) {
		int idx = Arrays.binarySearch(movieIds, movieId);
		return idx >= 0 ? idx : -1;
	}

	public int movieId(int idx) {
		return movieIds[idx];
	}

	public String title(int idx) {
		return titles[idx];
	}

	public Integer year(int idx) {
		return years[idx];
	}

	public int ratingCount(int idx) {
		return ratingCounts[idx];
	}

	/** All genres, in catalog order: the column order of every genre vector. */
	public List<String> genreNames() {
		return genreNames;
	}

	/** The genres of one movie, in catalog order. */
	public List<String> genres(int idx) {
		List<String> genres = new ArrayList<>();
		for (int g = 0; g < genreNames.size(); g++) {
			if (genreRows[idx][g] != 0) {
				genres.add(genreNames.get(g));
			}
		}
		return List.copyOf(genres);
	}

	int maxRatingCount() {
		return maxRatingCount;
	}

	/** The 0/1 genre matrix {@code x}. Shared, not copied: engine code must not modify it. */
	double[][] genreRows() {
		return genreRows;
	}

	SortedMap<String, Integer> tagCounts(int idx) {
		return tagCounts.get(idx);
	}

	public static final class Builder {

		private final List<String> declaredGenres = new ArrayList<>();

		private final List<MovieEntry> movies = new ArrayList<>();

		private Builder() {
		}

		/**
		 * Declares the catalog order of genres (the columns of every genre vector). Genres a movie uses but that
		 * were not declared are appended in the order they are first seen, visiting movies by ascending ID.
		 */
		public Builder genres(List<String> names) {
			for (String name : names) {
				if (declaredGenres.contains(name)) {
					throw new IllegalArgumentException("Genre declared twice: " + name);
				}
				declaredGenres.add(name);
			}
			return this;
		}

		public Builder addMovie(int id, String title, Integer year, List<String> genres, Map<String, Integer> tagCounts,
				int ratingCount) {
			if (ratingCount < 0) {
				throw new IllegalArgumentException("ratingCount must not be negative for movie " + id);
			}
			movies.add(new MovieEntry(id, title, year, List.copyOf(genres), new TreeMap<>(tagCounts), ratingCount));
			return this;
		}

		public CatalogSnapshot build() {
			List<MovieEntry> sorted = new ArrayList<>(movies);
			sorted.sort(Comparator.comparingInt(MovieEntry::id));
			for (int n = 1; n < sorted.size(); n++) {
				if (sorted.get(n).id() == sorted.get(n - 1).id()) {
					throw new IllegalArgumentException("Duplicate movie id " + sorted.get(n).id());
				}
			}

			List<String> genreNames = new ArrayList<>(declaredGenres);
			Map<String, Integer> genreIndex = new HashMap<>();
			for (int g = 0; g < genreNames.size(); g++) {
				genreIndex.put(genreNames.get(g), g);
			}
			for (MovieEntry movie : sorted) {
				for (String genre : movie.genres()) {
					if (!genreIndex.containsKey(genre)) {
						genreIndex.put(genre, genreNames.size());
						genreNames.add(genre);
					}
				}
			}

			int N = sorted.size();
			int G = genreNames.size();
			int[] movieIds = new int[N];
			String[] titles = new String[N];
			Integer[] years = new Integer[N];
			int[] ratingCounts = new int[N];
			double[][] x = new double[N][G];
			List<SortedMap<String, Integer>> tagCounts = new ArrayList<>(N);
			for (int i = 0; i < N; i++) {
				MovieEntry movie = sorted.get(i);
				movieIds[i] = movie.id();
				titles[i] = movie.title();
				years[i] = movie.year();
				ratingCounts[i] = movie.ratingCount();
				for (String genre : movie.genres()) {
					x[i][genreIndex.get(genre)] = 1;
				}
				tagCounts.add(Collections.unmodifiableSortedMap(movie.tagCounts()));
			}
			return new CatalogSnapshot(movieIds, titles, years, ratingCounts, genreNames, x, tagCounts);
		}

		private record MovieEntry(int id, String title, Integer year, List<String> genres,
				SortedMap<String, Integer> tagCounts, int ratingCount) {
		}
	}
}
