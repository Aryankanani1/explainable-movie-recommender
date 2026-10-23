package edu.fau.recsys.web;

import java.math.BigDecimal;
import java.util.List;

import edu.fau.recsys.catalog.MovieSummary;
import edu.fau.recsys.catalog.StarterSetService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(OnboardingController.class)
class OnboardingControllerTest {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	StarterSetService starterSet;

	@Test
	void starterSetDefaultsToFifteenFilms() {
		given(starterSet.starterSet(15)).willReturn(List.of(new MovieSummary(356, "Forrest Gump", 1994,
				List.of("Comedy", "Drama", "Romance", "War"), 329, new BigDecimal("4.16"))));

		var json = assertThat(mvc.get().uri("/api/onboarding/starter-set")).hasStatusOk().bodyJson();

		json.extractingPath("$[0].title").isEqualTo("Forrest Gump");
		json.extractingPath("$[0].genres").isEqualTo(List.of("Comedy", "Drama", "Romance", "War"));
	}

	@Test
	void sizeOutsideOneToThirtyIsBadRequest() {
		assertThat(mvc.get().uri("/api/onboarding/starter-set").param("size", "31")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("size must be between 1 and 30");
	}
}
