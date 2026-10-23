package edu.fau.recsys.engine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.fau.recsys.dataset.MovieLensFiles;
import edu.fau.recsys.dataset.MovieLensMovie;
import org.junit.jupiter.api.Test;

import static edu.fau.recsys.engine.LectureFixture.BATMAN_BEGINS;
import static edu.fau.recsys.engine.LectureFixture.GUARDIANS;
import static edu.fau.recsys.engine.LectureFixture.LADY_BIRD;
import static org.assertj.core.api.Assertions.assertThat;

class StarterSetTest {

	@Test
	void greedilyAddsTheMostNewGenresThenFallsBackToRatingCount() {
		List<StarterSet.Candidate> candidates = fixtureCandidates();

		// Guardians covers 4 genres; then only Lady Bird adds one (Drama); then nothing new, so most-rated wins.
		assertThat(StarterSet.select(candidates, 3)).containsExactly(GUARDIANS, LADY_BIRD, BATMAN_BEGINS);
	}

	@Test
	void isDeterministicAndStopsWhenCandidatesRunOut() {
		List<StarterSet.Candidate> candidates = fixtureCandidates();

		assertThat(StarterSet.select(candidates, 20)).hasSize(7).isEqualTo(StarterSet.select(candidates, 20));
	}

	@Test
	void equalGainAndCountBreakByMovieId() {
		List<StarterSet.Candidate> candidates = List.of(new StarterSet.Candidate(9, List.of("Drama"), 10),
				new StarterSet.Candidate(4, List.of("Comedy"), 10));

		assertThat(StarterSet.select(candidates, 2)).containsExactly(4, 9);
	}

	/** Smoke test on the real MovieLens files: the candidate rule of ticket 07, then the greedy pick. */
	@Test
	void realCatalogStarterSetCoversAtLeastTwelveGenres() {
		MovieLensFiles files = new MovieLensFiles(Path.of("data/ml-latest-small"));
		Map<Integer, int[]> stats = new HashMap<>();
		files.forEachRating(rating -> {
			int[] s = stats.computeIfAbsent(rating.movieId(), id -> new int[2]);
			s[0]++;
			s[1] += (int) Math.round(rating.rating() * 2);
		});
		List<MovieLensMovie> movies = new ArrayList<>();
		files.forEachMovie(movies::add);
		List<StarterSet.Candidate> candidates = new ArrayList<>();
		for (MovieLensMovie movie : movies) {
			int[] s = stats.get(movie.movieId());
			if (s != null && s[1] / 2.0 / s[0] >= StarterSet.MIN_MEAN) {
				candidates.add(new StarterSet.Candidate(movie.movieId(), movie.genres(), s[0]));
			}
		}
		List<StarterSet.Candidate> pool = StarterSet.mostRated(candidates, StarterSet.POOL_SIZE);

		List<Integer> starter = StarterSet.select(pool, 15);

		Set<String> covered = new HashSet<>();
		for (int movieId : starter) {
			covered.addAll(pool.stream().filter(c -> c.movieId() == movieId).findFirst().orElseThrow().genres());
		}
		assertThat(pool).hasSize(300);
		assertThat(starter).hasSize(15).isEqualTo(StarterSet.select(pool, 15));
		assertThat(covered).as("genres covered: %s", covered).hasSizeGreaterThanOrEqualTo(12);
	}

	private static List<StarterSet.Candidate> fixtureCandidates() {
		CatalogSnapshot catalog = LectureFixture.catalog();
		List<StarterSet.Candidate> candidates = new ArrayList<>();
		for (int i = 0; i < catalog.size(); i++) {
			candidates.add(new StarterSet.Candidate(catalog.movieId(i), catalog.genres(i), catalog.ratingCount(i)));
		}
		return candidates;
	}
}
