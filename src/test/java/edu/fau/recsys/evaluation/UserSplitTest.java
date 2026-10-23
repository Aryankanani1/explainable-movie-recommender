package edu.fau.recsys.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.fau.recsys.dataset.MovieLensRating;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSplitTest {

	@Test
	void eightyPercentRoundedDownIsTrain() {
		UserSplit split = UserSplit.of(7, ratings(7, 23));

		assertThat(split.userId()).isEqualTo(7);
		assertThat(split.train()).hasSize(18);
		assertThat(split.test()).hasSize(5);
	}

	@Test
	void trainAndTestAreDisjointAndCoverEveryRating() {
		List<MovieLensRating> all = ratings(3, 40);
		UserSplit split = UserSplit.of(3, all);

		Set<Integer> train = movieIds(split.train());
		Set<Integer> test = movieIds(split.test());
		assertThat(train).doesNotContainAnyElementsOf(test);
		Set<Integer> union = new HashSet<>(train);
		union.addAll(test);
		assertThat(union).isEqualTo(movieIds(all));
	}

	@Test
	void sameUserAlwaysGetsTheSameSplitWhateverTheInputOrder() {
		List<MovieLensRating> sorted = ratings(5, 30);
		List<MovieLensRating> shuffled = new ArrayList<>(sorted);
		Collections.shuffle(shuffled, new Random(123));

		assertThat(UserSplit.of(5, shuffled)).isEqualTo(UserSplit.of(5, sorted));
		assertThat(UserSplit.of(5, sorted)).isEqualTo(UserSplit.of(5, sorted));
	}

	@Test
	void theSeedDependsOnTheUser() {
		assertThat(movieIds(UserSplit.of(1, ratings(1, 30)).test()))
			.isNotEqualTo(movieIds(UserSplit.of(2, ratings(1, 30)).test()));
	}

	@Test
	void validationIsTheLastTenPercentOfTrain() {
		UserSplit split = UserSplit.of(9, ratings(9, 50));
		UserSplit validation = split.validation();

		// 40 train ratings: the first 36 stay, the last 4 become validation.
		assertThat(validation.train()).isEqualTo(split.train().subList(0, 36));
		assertThat(validation.test()).isEqualTo(split.train().subList(36, 40));
		assertThat(movieIds(validation.test())).doesNotContainAnyElementsOf(movieIds(split.test()));
	}

	private static List<MovieLensRating> ratings(int userId, int count) {
		List<MovieLensRating> ratings = new ArrayList<>();
		for (int n = 1; n <= count; n++) {
			ratings.add(new MovieLensRating(userId, n * 10, 0.5 + (n % 10) * 0.5, 1_000_000L + n));
		}
		return ratings;
	}

	private static Set<Integer> movieIds(List<MovieLensRating> ratings) {
		Set<Integer> ids = new HashSet<>();
		for (MovieLensRating rating : ratings) {
			ids.add(rating.movieId());
		}
		return ids;
	}
}
