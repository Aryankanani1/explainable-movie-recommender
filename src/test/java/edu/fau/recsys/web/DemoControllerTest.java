package edu.fau.recsys.web;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Acceptance: GET /api/demo/lecture-example returns 0.6667 / 0.6333 / 0.3333 / 0. */
@WebMvcTest(DemoController.class)
class DemoControllerTest {

	@Autowired
	MockMvcTester mvc;

	@Test
	void reproducesTheSlideExample() {
		var json = assertThat(mvc.get().uri("/api/demo/lecture-example")).hasStatusOk().bodyJson();

		json.extractingPath("$.recommendations.items[*].title")
			.isEqualTo(List.of("The Hitchhiker's Guide to the Galaxy", "Spider-Man", "Batman Begins", "Lady Bird"));
		json.extractingPath("$.recommendations.items[0].score").asNumber().satisfies(score ->
				assertThat(score.doubleValue()).isCloseTo(0.6667, within(1e-4)));
		json.extractingPath("$.recommendations.items[1].score").asNumber().satisfies(score ->
				assertThat(score.doubleValue()).isCloseTo(0.6333, within(1e-4)));
		json.extractingPath("$.recommendations.items[2].score").asNumber().satisfies(score ->
				assertThat(score.doubleValue()).isCloseTo(0.3333, within(1e-4)));
		json.extractingPath("$.recommendations.items[3].score").asNumber().satisfies(score ->
				assertThat(score.doubleValue()).isZero());
		json.extractingPath("$.math.lecture.profile.T").asNumber().satisfies(total ->
				assertThat(total.doubleValue()).isEqualTo(60.0));
		json.extractingPath("$.math.lecture.profile.P").isEqualTo(List.of(18.0, 12.0, 20.0, 10.0, 0.0));
	}
}
