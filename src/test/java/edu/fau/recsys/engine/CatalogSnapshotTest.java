package edu.fau.recsys.engine;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogSnapshotTest {

	@Test
	void moviesAreIndexedInMovieIdOrderWhateverTheInsertionOrder() {
		CatalogSnapshot catalog = CatalogSnapshot.builder()
			.addMovie(30, "Thirty", 2003, List.of("Drama"), Map.of(), 3)
			.addMovie(10, "Ten", null, List.of("Comedy"), Map.of(), 1)
			.addMovie(20, "Twenty", 2002, List.of(), Map.of(), 2)
			.build();

		assertThat(catalog.size()).isEqualTo(3);
		assertThat(catalog.movieId(0)).isEqualTo(10);
		assertThat(catalog.indexOf(20)).isEqualTo(1);
		assertThat(catalog.title(2)).isEqualTo("Thirty");
		assertThat(catalog.year(1)).isEqualTo(2002);
		assertThat(catalog.year(0)).isNull();
		assertThat(catalog.ratingCount(2)).isEqualTo(3);
		assertThat(catalog.indexOf(99)).isEqualTo(-1);
	}

	@Test
	void declaredGenresComeFirstThenUndeclaredOnesInFirstSeenOrder() {
		CatalogSnapshot catalog = CatalogSnapshot.builder()
			.genres(List.of("Sci-Fi", "Comedy"))
			.addMovie(2, "Two", null, List.of("Western", "Comedy"), Map.of(), 0)
			.addMovie(1, "One", null, List.of("Horror", "Sci-Fi"), Map.of(), 0)
			.build();

		// Movie 1 is visited first (ID order), so Horror precedes Western.
		assertThat(catalog.genreNames()).containsExactly("Sci-Fi", "Comedy", "Horror", "Western");
		assertThat(catalog.genres(catalog.indexOf(2))).containsExactly("Comedy", "Western");
	}

	@Test
	void fixtureGenreRowsAreZeroOneInCatalogOrder() {
		CatalogSnapshot catalog = LectureFixture.catalog();

		assertThat(catalog.genreNames()).containsExactly("Comedy", "Adventure", "Super Hero", "Sci-Fi", "Drama");
		assertThat(catalog.genreRows()[catalog.indexOf(LectureFixture.GUARDIANS)]).containsExactly(1, 1, 1, 1, 0);
		assertThat(catalog.genres(catalog.indexOf(LectureFixture.LADY_BIRD))).containsExactly("Drama");
		assertThat(catalog.maxRatingCount()).isEqualTo(400);
	}

	@Test
	void duplicateMovieIdsAreRejected() {
		CatalogSnapshot.Builder builder = CatalogSnapshot.builder()
			.addMovie(1, "One", null, List.of(), Map.of(), 0)
			.addMovie(1, "Again", null, List.of(), Map.of(), 0);

		assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1");
	}

	@Test
	void userRatingsIterateInMovieIdOrderAndRejectOffScaleValues() {
		UserRatings ratings = new UserRatings(Map.of(30, 3.0, 10, 1.0, 20, 2.0), LectureFixture.SCALE);

		assertThat(ratings.byMovieId().keySet()).containsExactly(10, 20, 30);
		assertThatThrownBy(() -> new UserRatings(Map.of(1, 11.0), LectureFixture.SCALE))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
