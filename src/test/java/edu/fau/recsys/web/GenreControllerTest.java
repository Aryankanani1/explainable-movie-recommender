package edu.fau.recsys.web;

import java.util.List;

import edu.fau.recsys.catalog.GenreCount;
import edu.fau.recsys.catalog.MovieCatalog;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(GenreController.class)
class GenreControllerTest {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	MovieCatalog movieCatalog;

	@Test
	void listsGenresWithMovieCountsInCatalogOrder() {
		given(movieCatalog.genres()).willReturn(List.of(new GenreCount("Action", 1828), new GenreCount("Adventure", 1263)));

		var json = assertThat(mvc.get().uri("/api/genres")).hasStatusOk().bodyJson();

		json.extractingPath("$[*].name").isEqualTo(List.of("Action", "Adventure"));
		json.extractingPath("$[0].movieCount").isEqualTo(1828);
	}
}
