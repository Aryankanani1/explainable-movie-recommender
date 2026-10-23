package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Films to show a new user before they have grades (ticket 07): well-known, well-liked, and spread across genres so
 * the first few grades already say something about taste. Candidates are the {@value #POOL_SIZE} most-rated films with
 * a mean rating of at least {@value #MIN_MEAN}; each step then takes the one adding the most genres not yet covered.
 */
public final class StarterSet {

	public static final double MIN_MEAN = 3.5;

	public static final int POOL_SIZE = 300;

	/** Most-rated first, then lower movieId: the order that breaks every tie here. */
	private static final Comparator<Candidate> MOST_RATED = Comparator.comparingInt(Candidate::ratingCount)
		.reversed()
		.thenComparingInt(Candidate::movieId);

	private StarterSet() {
	}

	/** The {@code limit} most-rated candidates. */
	public static List<Candidate> mostRated(List<Candidate> candidates, int limit) {
		List<Candidate> sorted = new ArrayList<>(candidates);
		sorted.sort(MOST_RATED);
		return List.copyOf(sorted.subList(0, Math.min(limit, sorted.size())));
	}

	/**
	 * Greedy pick of {@code size} movieIds: each step takes the candidate that adds the most genres not yet covered;
	 * ties (including "adds nothing new") go to the more-rated film, then the lower movieId. Deterministic.
	 */
	public static List<Integer> select(List<Candidate> candidates, int size) {
		List<Candidate> remaining = new ArrayList<>(candidates);
		remaining.sort(MOST_RATED);
		Set<String> covered = new HashSet<>();
		List<Integer> picked = new ArrayList<>();
		while (picked.size() < size && !remaining.isEmpty()) {
			Candidate best = null;
			int bestGain = -1;
			for (Candidate candidate : remaining) {
				int gain = 0;
				for (String genre : candidate.genres()) {
					if (!covered.contains(genre)) {
						gain++;
					}
				}
				// remaining is in MOST_RATED order, so a strictly greater gain is the only reason to switch.
				if (gain > bestGain) {
					best = candidate;
					bestGain = gain;
				}
			}
			picked.add(best.movieId());
			covered.addAll(best.genres());
			remaining.remove(best);
		}
		return List.copyOf(picked);
	}

	public record Candidate(int movieId, List<String> genres, int ratingCount) {

		public Candidate {
			genres = List.copyOf(genres);
		}
	}
}
