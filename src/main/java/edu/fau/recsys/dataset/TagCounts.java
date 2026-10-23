package edu.fau.recsys.dataset;

import java.util.SortedMap;

/**
 * Tag applications aggregated per movie. Tags are lowercased and trimmed, then counted per (movie, tag).
 * Sorted maps keep iteration order deterministic.
 *
 * @param applications number of rows read from {@code tags.csv}
 * @param byMovie movieId → (tag → number of applications)
 */
public record TagCounts(int applications, SortedMap<Integer, SortedMap<String, Integer>> byMovie) {

	/** Number of distinct (movie, tag) pairs. */
	public int pairs() {
		int pairs = 0;
		for (SortedMap<String, Integer> tags : byMovie.values()) {
			pairs += tags.size();
		}
		return pairs;
	}
}
