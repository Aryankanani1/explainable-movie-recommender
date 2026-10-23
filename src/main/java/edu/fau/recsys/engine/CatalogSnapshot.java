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
import java.util.concurrent.ConcurrentHashMap;

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

	/** idf_g = ln(N / n_g); 0 for a declared genre no movie has. */
	private final double[] genreIdf;

	/** Tags on at least {@value #MIN_TAG_DOCUMENT_FREQUENCY} movies, alphabetical: the tag block's column order. */
	private final List<String> tagVocabulary;

	/** idf_t = ln(N / df_t) per vocabulary tag. */
	private final double[] tagIdf;

	/** Per movie: vocabulary indices of its tags, ascending, and t_it = (1 + ln count_it) · idf_t. */
	private final int[][] tagIdx;

	private final double[][] tagTfIdf;

	private final int moviesWithVocabularyTag;

	/** Item vectors depend only on catalog content, so they are built once per configuration and shared. */
	private final Map<VectorKey, ItemVectors> itemVectorCache = new ConcurrentHashMap<>();

	/** A tag on one movie cannot link two movies, so it is left out of the vocabulary. */
	static final int MIN_TAG_DOCUMENT_FREQUENCY = 2;

	private CatalogSnapshot(int[] movieIds, String[] titles, Integer[] years, int[] ratingCounts,
			List<String> genreNames, double[][] genreRows, List<SortedMap<String, Integer>> tagCounts) {
		this.movieIds = movieIds;
		this.titles = titles;
		this.years = years;
		this.ratingCounts = ratingCounts;
		this.genreNames = List.copyOf(genreNames);
		this.genreRows = genreRows;
		int max = 0;
		for (int count : ratingCounts) {
			max = Math.max(max, count);
		}
		this.maxRatingCount = max;

		int N = movieIds.length;
		int G = genreNames.size();
		this.genreIdf = new double[G];
		for (int g = 0; g < G; g++) {
			int n = 0;
			for (int i = 0; i < N; i++) {
				if (genreRows[i][g] != 0) {
					n++;
				}
			}
			genreIdf[g] = n == 0 ? 0 : Math.log((double) N / n);
		}

		// Tag vocabulary: document frequency df_t = number of movies with tag t; keep df_t >= 2.
		SortedMap<String, Integer> documentFrequency = new TreeMap<>();
		for (SortedMap<String, Integer> movieTags : tagCounts) {
			for (String tag : movieTags.keySet()) {
				documentFrequency.merge(tag, 1, Integer::sum);
			}
		}
		List<String> vocabulary = new ArrayList<>();
		for (Map.Entry<String, Integer> tag : documentFrequency.entrySet()) {
			if (tag.getValue() >= MIN_TAG_DOCUMENT_FREQUENCY) {
				vocabulary.add(tag.getKey());
			}
		}
		this.tagVocabulary = List.copyOf(vocabulary);
		Map<String, Integer> vocabularyIndex = new HashMap<>();
		this.tagIdf = new double[vocabulary.size()];
		for (int t = 0; t < vocabulary.size(); t++) {
			vocabularyIndex.put(vocabulary.get(t), t);
			tagIdf[t] = Math.log((double) N / documentFrequency.get(vocabulary.get(t)));
		}

		// t_it = (1 + ln count_it) · idf_t. Each movie's tags are alphabetical, so indices come out ascending.
		this.tagIdx = new int[N][];
		this.tagTfIdf = new double[N][];
		int withTag = 0;
		for (int i = 0; i < N; i++) {
			List<Integer> indices = new ArrayList<>();
			List<Double> values = new ArrayList<>();
			for (Map.Entry<String, Integer> tag : tagCounts.get(i).entrySet()) {
				Integer t = vocabularyIndex.get(tag.getKey());
				if (t != null) {
					indices.add(t);
					values.add((1 + Math.log(tag.getValue())) * tagIdf[t]);
				}
			}
			tagIdx[i] = new int[indices.size()];
			tagTfIdf[i] = new double[values.size()];
			for (int k = 0; k < indices.size(); k++) {
				tagIdx[i][k] = indices.get(k);
				tagTfIdf[i][k] = values.get(k);
			}
			if (!indices.isEmpty()) {
				withTag++;
			}
		}
		this.moviesWithVocabularyTag = withTag;
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

	/** idf_g = ln(N / n_g) per genre, in catalog order. A copy: changing it does not change the snapshot. */
	public double[] genreIdf() {
		return genreIdf.clone();
	}

	/** Tags with document frequency >= 2, alphabetical. */
	public List<String> tagVocabulary() {
		return tagVocabulary;
	}

	/** How many movies have at least one vocabulary tag (most have none). */
	public int moviesWithVocabularyTag() {
		return moviesWithVocabularyTag;
	}

	/** idf_t per vocabulary tag, aligned with {@link #tagVocabulary()}. A copy. */
	double[] tagIdf() {
		return tagIdf.clone();
	}

	int maxRatingCount() {
		return maxRatingCount;
	}

	/** The 0/1 genre matrix {@code x}. Shared, not copied: engine code must not modify it. */
	double[][] genreRows() {
		return genreRows;
	}

	/**
	 * Item vectors v_i (ALGORITHMS.md §3), built on first use for each configuration and then reused:
	 * <pre>
	 *   ĝ_i = g_i / ‖g_i‖  with g_i = useIdf ? x_i ⊙ idf : x_i     (zero vector if the movie has no genres)
	 *   t̂_i = t_i / ‖t_i‖                                          (zero vector if no vocabulary tags)
	 *   v_i = [ĝ_i ; β · t̂_i]                                       (just ĝ_i when β = 0)
	 * </pre>
	 * Each block is normalized on its own, so movies with many genres or tags do not dominate a profile.
	 */
	ItemVectors itemVectors(boolean useIdf, double tagWeight) {
		return itemVectorCache.computeIfAbsent(new VectorKey(useIdf, tagWeight),
				key -> buildItemVectors(key.useIdf(), key.tagWeight()));
	}

	private ItemVectors buildItemVectors(boolean useIdf, double beta) {
		int N = movieIds.length;
		int G = genreNames.size();
		double[][] gHat = new double[N][G];
		int[][] idx = new int[N][];
		double[][] val = new double[N][];
		for (int i = 0; i < N; i++) {
			for (int g = 0; g < G; g++) {
				gHat[i][g] = useIdf ? genreRows[i][g] * genreIdf[g] : genreRows[i][g];
			}
			double genreNorm = Vectors.norm(gHat[i]);
			if (genreNorm > 0) {
				for (int g = 0; g < G; g++) {
					gHat[i][g] = gHat[i][g] / genreNorm;
				}
			}

			double tagNorm = Vectors.norm(tagTfIdf[i]);
			if (beta > 0 && tagNorm > 0) {
				idx[i] = tagIdx[i];
				val[i] = new double[tagTfIdf[i].length];
				for (int k = 0; k < val[i].length; k++) {
					val[i][k] = beta * tagTfIdf[i][k] / tagNorm;
				}
			}
			else {
				idx[i] = new int[0];
				val[i] = new double[0];
			}
		}
		return new ItemVectors(G, beta > 0 ? tagVocabulary.size() : 0, gHat, idx, val);
	}

	private record VectorKey(boolean useIdf, double tagWeight) {
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
			for (Map.Entry<String, Integer> tag : tagCounts.entrySet()) {
				if (tag.getValue() < 1) {
					throw new IllegalArgumentException("Tag count must be at least 1: " + tag + " on movie " + id);
				}
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
