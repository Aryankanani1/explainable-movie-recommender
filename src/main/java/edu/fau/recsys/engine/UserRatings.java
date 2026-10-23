package edu.fau.recsys.engine;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * One user's ratings, movieId → rating. Copied into a {@link TreeMap} so iteration is always in movieId order.
 * IDs that are not in the catalog are allowed here and ignored by the recommenders.
 */
public record UserRatings(Map<Integer, Double> byMovieId, RatingScale scale) {

	public UserRatings {
		Objects.requireNonNull(scale, "scale");
		SortedMap<Integer, Double> copy = new TreeMap<>();
		for (Map.Entry<Integer, Double> rating : byMovieId.entrySet()) {
			if (!scale.contains(rating.getValue())) {
				throw new IllegalArgumentException(
						"Rating " + rating.getValue() + " for movie " + rating.getKey() + " is outside " + scale);
			}
			copy.put(rating.getKey(), rating.getValue());
		}
		byMovieId = Collections.unmodifiableSortedMap(copy);
	}
}
