package edu.fau.recsys.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import edu.fau.recsys.catalog.MovieCatalog;
import edu.fau.recsys.catalog.MovieDetail;
import edu.fau.recsys.catalog.MovieSummary;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(MovieController.class)
class MovieControllerTest {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	MovieCatalog movieCatalog;

	@Test
	void returnsPagedSummariesWithStableJsonShape() {
		MovieSummary matrix = new MovieSummary(2571, "The Matrix", 1999, List.of("Action", "Sci-Fi", "Thriller"), 278,
				new BigDecimal("4.19"));
		given(movieCatalog.search("matrix", 0, 20)).willReturn(new PageImpl<>(List.of(matrix), PageRequest.of(0, 20), 1));

		var response = assertThat(mvc.get().uri("/api/movies").param("q", "matrix")).hasStatusOk().bodyJson();

		response.extractingPath("$.content[0].id").isEqualTo(2571);
		response.extractingPath("$.content[0].title").isEqualTo("The Matrix");
		response.extractingPath("$.content[0].year").isEqualTo(1999);
		response.extractingPath("$.content[0].genres").isEqualTo(List.of("Action", "Sci-Fi", "Thriller"));
		response.extractingPath("$.content[0].ratingCount").isEqualTo(278);
		response.extractingPath("$.page.totalElements").isEqualTo(1);
	}

	@Test
	void rejectsOutOfRangePageSizeWithProblemDetail() {
		assertThat(mvc.get().uri("/api/movies").param("size", "0")).hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("size must be between 1 and 100");
		verifyNoInteractions(movieCatalog);
	}

	@Test
	void rejectsNegativePageWithProblemDetail() {
		assertThat(mvc.get().uri("/api/movies").param("page", "-1")).hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("page must be 0 or more");
	}

	@Test
	void detailHasGenresTopTagsAndRatingStats() {
		given(movieCatalog.detail(2571)).willReturn(Optional.of(new MovieDetail(2571, "The Matrix", 1999,
				List.of("Action", "Sci-Fi", "Thriller"),
				List.of(new MovieDetail.TagCount("philosophy", 3), new MovieDetail.TagCount("dystopia", 2)), 278,
				new BigDecimal("4.19"))));

		var json = assertThat(mvc.get().uri("/api/movies/2571")).hasStatusOk().bodyJson();

		json.extractingPath("$.title").isEqualTo("The Matrix");
		json.extractingPath("$.genres").isEqualTo(List.of("Action", "Sci-Fi", "Thriller"));
		json.extractingPath("$.topTags[0].tag").isEqualTo("philosophy");
		json.extractingPath("$.topTags[0].count").isEqualTo(3);
		json.extractingPath("$.ratingCount").isEqualTo(278);
	}

	@Test
	void unknownMovieDetailIsNotFoundProblemDetail() {
		given(movieCatalog.detail(999999)).willReturn(Optional.empty());

		assertThat(mvc.get().uri("/api/movies/999999")).hasStatus(HttpStatus.NOT_FOUND)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("No movie with id 999999");
	}
}
