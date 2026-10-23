package edu.fau.recsys.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.fau.recsys.dataset.MovieLensFiles;
import edu.fau.recsys.dataset.MovieLensMovie;
import edu.fau.recsys.dataset.MovieLensRating;
import edu.fau.recsys.dataset.TagCounts;
import edu.fau.recsys.engine.CatalogSnapshot;

/**
 * MovieLens read straight from the CSVs (no database, no Spring), plus the train-only snapshots the evaluation
 * scores against. Genres are declared in alphabetical order, the same order the app's genre IDs give.
 */
final class EvaluationData {

	private final List<MovieLensMovie> movies;

	private final TagCounts tags;

	private final SortedMap<Integer, List<MovieLensRating>> ratingsByUser;

	private final List<String> genreOrder;

	private EvaluationData(List<MovieLensMovie> movies, TagCounts tags,
			SortedMap<Integer, List<MovieLensRating>> ratingsByUser) {
		this.movies = movies;
		this.tags = tags;
		this.ratingsByUser = ratingsByUser;
		SortedSet<String> genres = new TreeSet<>();
		for (MovieLensMovie movie : movies) {
			genres.addAll(movie.genres());
		}
		this.genreOrder = List.copyOf(genres);
	}

	static EvaluationData load(MovieLensFiles files) {
		List<MovieLensMovie> movies = new ArrayList<>();
		files.forEachMovie(movies::add);
		SortedMap<Integer, List<MovieLensRating>> ratingsByUser = new TreeMap<>();
		files.forEachRating(rating -> ratingsByUser.computeIfAbsent(rating.userId(), id -> new ArrayList<>()).add(rating));
		return new EvaluationData(movies, files.tagCounts(), ratingsByUser);
	}

	int users() {
		return ratingsByUser.size();
	}

	int movies() {
		return movies.size();
	}

	int ratings() {
		int count = 0;
		for (List<MovieLensRating> ratings : ratingsByUser.values()) {
			count += ratings.size();
		}
		return count;
	}

	/** One split per user, in userId order. */
	List<UserSplit> splits() {
		List<UserSplit> splits = new ArrayList<>();
		for (Map.Entry<Integer, List<MovieLensRating>> user : ratingsByUser.entrySet()) {
			splits.add(UserSplit.of(user.getKey(), user.getValue()));
		}
		return splits;
	}

	/**
	 * Catalog content (titles, genres, tags) with ratingCount taken from the splits' train ratings only, so that
	 * popularity, the popularity prior and tie-breaks never see a hidden rating (EVALUATION.md, leakage rules).
	 */
	CatalogSnapshot snapshot(List<UserSplit> splits) {
		Map<Integer, Integer> trainCounts = new TreeMap<>();
		for (UserSplit split : splits) {
			for (MovieLensRating rating : split.train()) {
				trainCounts.merge(rating.movieId(), 1, Integer::sum);
			}
		}
		CatalogSnapshot.Builder builder = CatalogSnapshot.builder().genres(genreOrder);
		for (MovieLensMovie movie : movies) {
			builder.addMovie(movie.movieId(), movie.displayTitle(), movie.year(), movie.genres(),
					tags.byMovie().getOrDefault(movie.movieId(), new TreeMap<>()),
					trainCounts.getOrDefault(movie.movieId(), 0));
		}
		return builder.build();
	}
}
