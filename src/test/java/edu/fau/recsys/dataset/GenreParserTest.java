package edu.fau.recsys.dataset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenreParserTest {

	@Test
	void splitsPipeSeparatedGenresInFileOrder() {
		assertThat(GenreParser.parse("Adventure|Animation|Children|Comedy|Fantasy"))
			.containsExactly("Adventure", "Animation", "Children", "Comedy", "Fantasy");
	}

	@Test
	void singleGenre() {
		assertThat(GenreParser.parse("Drama")).containsExactly("Drama");
	}

	@Test
	void noGenresListedMeansZeroGenres() {
		assertThat(GenreParser.parse("(no genres listed)")).isEmpty();
	}
}
