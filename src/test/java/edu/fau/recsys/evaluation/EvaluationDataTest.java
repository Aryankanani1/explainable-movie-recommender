package edu.fau.recsys.evaluation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import edu.fau.recsys.dataset.MovieLensFiles;
import edu.fau.recsys.dataset.MovieLensRating;
import edu.fau.recsys.engine.CatalogSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** Leakage rule: the evaluation snapshot's popularity counts come from train ratings only. */
class EvaluationDataTest {

	@TempDir
	Path dir;

	private EvaluationData data;

	@BeforeEach
	void tinyDataset() throws IOException {
		write("movies.csv", """
				movieId,title,genres
				1,Alpha (2001),Drama
				2,"Beta, The (2002)",Comedy|Drama
				3,Gamma (2003),(no genres listed)
				""");
		// Three users each rate all three movies.
		StringBuilder ratings = new StringBuilder("userId,movieId,rating,timestamp\n");
		for (int user = 1; user <= 3; user++) {
			for (int movie = 1; movie <= 3; movie++) {
				ratings.append(user).append(',').append(movie).append(",4.0,").append(1_000 + movie).append('\n');
			}
		}
		write("ratings.csv", ratings.toString());
		write("tags.csv", """
				userId,movieId,tag,timestamp
				1,1,Moody,1
				2,2,moody ,2
				""");
		write("links.csv", """
				movieId,imdbId,tmdbId
				1,1,1
				""");
		data = EvaluationData.load(new MovieLensFiles(dir));
	}

	@Test
	void loadsMoviesUsersAndAggregatedTags() {
		assertThat(data.users()).isEqualTo(3);
		assertThat(data.splits()).extracting(UserSplit::userId).containsExactly(1, 2, 3);
		CatalogSnapshot snapshot = data.snapshot(data.splits());
		assertThat(snapshot.size()).isEqualTo(3);
		assertThat(snapshot.title(snapshot.indexOf(2))).isEqualTo("The Beta");
		assertThat(snapshot.tagVocabulary()).containsExactly("moody");
	}

	@Test
	void ratingCountsComeFromTrainRatingsOnly() {
		List<UserSplit> splits = data.splits();
		CatalogSnapshot snapshot = data.snapshot(splits);

		int[] trainCounts = new int[4];
		int[] allCounts = new int[4];
		for (UserSplit split : splits) {
			for (MovieLensRating rating : split.train()) {
				trainCounts[rating.movieId()]++;
			}
			for (MovieLensRating rating : split.test()) {
				allCounts[rating.movieId()]++;
			}
		}
		int hidden = 0;
		for (int movie = 1; movie <= 3; movie++) {
			allCounts[movie] += trainCounts[movie];
			assertThat(snapshot.ratingCount(snapshot.indexOf(movie))).as("movie %d", movie).isEqualTo(trainCounts[movie]);
			hidden += allCounts[movie] - trainCounts[movie];
		}
		// Each user has 3 ratings: floor(0.8 · 3) = 2 train, so 3 ratings are hidden in total.
		assertThat(hidden).isEqualTo(3);
	}

	@Test
	void validationSnapshotsExcludeValidationRatingsToo() {
		List<UserSplit> validation = data.splits().stream().map(UserSplit::validation).toList();
		CatalogSnapshot snapshot = data.snapshot(validation);

		int total = 0;
		for (int movie = 1; movie <= 3; movie++) {
			total += snapshot.ratingCount(snapshot.indexOf(movie));
		}
		// floor(0.9 · 2) = 1 rating per user stays in validation-train.
		assertThat(total).isEqualTo(3);
	}

	private void write(String name, String content) throws IOException {
		Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
	}
}
