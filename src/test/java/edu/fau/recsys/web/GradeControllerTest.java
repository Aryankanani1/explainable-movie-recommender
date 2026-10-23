package edu.fau.recsys.web;

import java.time.Instant;
import java.util.List;

import edu.fau.recsys.grading.CurrentUser;
import edu.fau.recsys.grading.CurrentUserProvider;
import edu.fau.recsys.grading.GradeService;
import edu.fau.recsys.grading.GradeView;
import edu.fau.recsys.grading.UnknownMovieException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(GradeController.class)
class GradeControllerTest {

	private static final long ME = 7;

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	GradeService gradeService;

	@MockitoBean
	CurrentUserProvider currentUserProvider;

	@BeforeEach
	void signedInAsDemo() {
		given(currentUserProvider.currentUser()).willReturn(new CurrentUser(ME, "demo"));
	}

	@Test
	void putStoresTheGradeForTheCurrentUser() {
		given(gradeService.upsert(ME, 2571, 9))
			.willReturn(new GradeView(2571, "The Matrix", 1999, 9, Instant.parse("2026-10-22T20:00:00Z")));

		var json = assertThat(put(2571, "{\"score\": 9}")).hasStatusOk().bodyJson();

		json.extractingPath("$.title").isEqualTo("The Matrix");
		json.extractingPath("$.score").isEqualTo(9);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"score\": 0}", "{\"score\": 11}", "{}", "{\"score\": \"nine\"}", "{\"score\": 8.5}" })
	void rejectsAnythingButAWholeGradeFromOneToTen(String body) {
		assertThat(put(2571, body)).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		verifyNoInteractions(gradeService);
	}

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource(delimiter = '|', textBlock = """
			{"score": 0}       | score must be a whole number from 1 to 10
			{"score": 11}      | score must be a whole number from 1 to 10
			{}                 | score is required
			{"score": "nine"}  | Malformed request body, or a value of the wrong type.
			{"score": 8.5}     | Malformed request body, or a value of the wrong type.
			""")
	void badGradesGetAReadableDetail(String body, String detail) {
		assertThat(put(2571, body)).bodyJson().extractingPath("$.detail").isEqualTo(detail);
	}

	@Test
	void unknownMovieIsNotFoundProblemDetail() {
		given(gradeService.upsert(ME, 999999, 5)).willThrow(new UnknownMovieException(999999));

		var response = assertThat(put(999999, "{\"score\": 5}")).hasStatus(HttpStatus.NOT_FOUND)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);

		response.bodyJson().extractingPath("$.detail").isEqualTo("No movie with id 999999");
	}

	@Test
	void listsMyGrades() {
		given(gradeService.list(ME)).willReturn(
				List.of(new GradeView(2571, "The Matrix", 1999, 9, Instant.parse("2026-10-22T20:00:00Z"))));

		assertThat(mvc.get().uri("/api/me/grades")).hasStatusOk().bodyJson().extractingPath("$[0].movieId").isEqualTo(2571);
	}

	@Test
	void deleteIsNoContent() {
		assertThat(mvc.delete().uri("/api/me/grades/2571")).hasStatus(HttpStatus.NO_CONTENT);
		verify(gradeService).delete(ME, 2571);
	}

	private MockMvcTester.MockMvcRequestBuilder put(int movieId, String body) {
		return mvc.put().uri("/api/me/grades/{movieId}", movieId).contentType(MediaType.APPLICATION_JSON).content(body);
	}
}
