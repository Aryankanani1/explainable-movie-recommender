package edu.fau.recsys.dataset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieLensFilesTest {

	@TempDir
	Path dir;

	MovieLensFiles files;

	@BeforeEach
	void writeTinyDataset() throws IOException {
		write("movies.csv", """
				movieId,title,genres
				1,Toy Story (1995),Adventure|Animation|Children|Comedy|Fantasy
				2571,"Matrix, The (1999)",Action|Sci-Fi|Thriller
				171749,Death Note: Desu nôto (2006–2007),(no genres listed)
				""");
		write("ratings.csv", """
				userId,movieId,rating,timestamp
				1,1,4.0,964982703
				1,2571,0.5,964981247
				""");
		write("tags.csv", """
				userId,movieId,tag,timestamp
				2,2571,Philosophy,1445714994
				7,2571,philosophy ,1445714996
				7,2571,dystopia,1445714997
				3,1,pixar,1445714998
				""");
		write("links.csv", """
				movieId,imdbId,tmdbId
				1,0114709,862
				171749,0877057,
				""");
		files = new MovieLensFiles(dir);
	}

	@Test
	void readsMoviesWithQuotedCommasParsedTitlesAndGenres() {
		List<MovieLensMovie> movies = new ArrayList<>();
		files.forEachMovie(movies::add);

		assertThat(movies).containsExactly(
				new MovieLensMovie(1, "Toy Story (1995)", "Toy Story", 1995,
						List.of("Adventure", "Animation", "Children", "Comedy", "Fantasy")),
				new MovieLensMovie(2571, "Matrix, The (1999)", "The Matrix", 1999, List.of("Action", "Sci-Fi", "Thriller")),
				new MovieLensMovie(171749, "Death Note: Desu nôto (2006–2007)", "Death Note: Desu nôto (2006–2007)", null,
						List.of()));
	}

	@Test
	void readsRatings() {
		List<MovieLensRating> ratings = new ArrayList<>();
		files.forEachRating(ratings::add);

		assertThat(ratings).containsExactly(new MovieLensRating(1, 1, 4.0, 964982703L),
				new MovieLensRating(1, 2571, 0.5, 964981247L));
	}

	@Test
	void readsLinksWithEmptyTmdbIdAsNull() {
		List<MovieLensLink> links = new ArrayList<>();
		files.forEachLink(links::add);

		assertThat(links).containsExactly(new MovieLensLink(1, "0114709", 862), new MovieLensLink(171749, "0877057", null));
	}

	@Test
	void aggregatesTagsAfterLowercasingAndTrimming() {
		TagCounts tags = files.tagCounts();

		assertThat(tags.applications()).isEqualTo(4);
		assertThat(tags.pairs()).isEqualTo(3);
		assertThat(tags.byMovie().keySet()).containsExactly(1, 2571);
		assertThat(tags.byMovie().get(2571)).containsExactly(Map.entry("dystopia", 1), Map.entry("philosophy", 2));
		assertThat(tags.byMovie().get(1)).containsExactly(Map.entry("pixar", 1));
	}

	@Test
	void reportsFileAndRowOfAMalformedRow() throws IOException {
		write("ratings.csv", """
				userId,movieId,rating,timestamp
				1,1,4.0,964982703
				1,abc,4.0,964982703
				""");

		assertThatThrownBy(() -> files.forEachRating(rating -> {
		})).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("row 2").hasMessageContaining("ratings.csv");
	}

	private void write(String name, String content) throws IOException {
		Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
	}
}
