package edu.fau.recsys.web;

import java.util.List;

import edu.fau.recsys.engine.Fallback;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	RecommendationService recommendationService;

	@Test
	void defaultsToTheLectureMethodAndLeavesKToTheConfiguredDefault() {
		given(recommendationService.recommend("lecture", null, 0))
			.willReturn(new RecommendationResultDto("lecture", Fallback.POPULARITY, List.of()));

		assertThat(mvc.get().uri("/api/me/recommendations")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.fallback")
			.isEqualTo("POPULARITY");
	}

	@Test
	void unknownMethodIsBadRequestProblemDetail() {
		given(recommendationService.recommend("bogus", null, 0)).willThrow(new UnknownMethodException("bogus"));

		assertThat(mvc.get().uri("/api/me/recommendations").param("method", "bogus"))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void kOutOfRangeIsBadRequest() {
		assertThat(mvc.get().uri("/api/me/recommendations").param("k", "0")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("k must be between 1 and 100");
		assertThat(mvc.get().uri("/api/me/recommendations/math").param("k", "21")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("k must be between 1 and 20");
	}
}
