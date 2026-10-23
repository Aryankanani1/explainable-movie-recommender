package edu.fau.recsys.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import edu.fau.recsys.dataset.MovieLensRating;

/**
 * One user's ratings, split for offline evaluation (EVALUATION.md, protocol step 3): sorted by movieId so the file
 * order cannot matter, shuffled with {@code new Random(42 + userId)}, then the first ⌊0.8·n⌋ are train and the rest
 * test. The split depends only on the user's ratings, so every run is identical.
 */
public record UserSplit(int userId, List<MovieLensRating> train, List<MovieLensRating> test) {

	public static final long SEED = 42;

	public UserSplit {
		train = List.copyOf(train);
		test = List.copyOf(test);
	}

	public static UserSplit of(int userId, List<MovieLensRating> ratings) {
		List<MovieLensRating> shuffled = new ArrayList<>(ratings);
		shuffled.sort(Comparator.comparingInt(MovieLensRating::movieId));
		Collections.shuffle(shuffled, new Random(SEED + userId));
		// Integer arithmetic: ⌊0.8·n⌋ without floating-point surprises.
		int trainSize = shuffled.size() * 8 / 10;
		return new UserSplit(userId, shuffled.subList(0, trainSize), shuffled.subList(trainSize, shuffled.size()));
	}

	/**
	 * For tuning without touching test (EVALUATION.md, "Don't tune on test"): the last 10% of train becomes the
	 * held-out set, the first ⌊0.9·|train|⌋ stay train. Test ratings are not used at all.
	 */
	public UserSplit validation() {
		int keep = train.size() * 9 / 10;
		return new UserSplit(userId, train.subList(0, keep), train.subList(keep, train.size()));
	}
}
