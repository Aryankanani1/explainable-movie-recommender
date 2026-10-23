package edu.fau.recsys.web;

import java.math.BigDecimal;
import java.util.List;

import edu.fau.recsys.catalog.MovieCatalog;
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
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		verifyNoInteractions(movieCatalog);
	}

	@Test
	void rejectsNegativePageWithProblemDetail() {
		assertThat(mvc.get().uri("/api/movies").param("page", "-1")).hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}
}
