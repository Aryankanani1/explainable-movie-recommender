package edu.fau.recsys;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole app against a real PostgreSQL (Testcontainers) and the tiny dataset in {@code src/test/resources/ml-tiny}:
 * Flyway, the MovieLens import, the snapshot, Spring Security and every endpoint a user touches.
 * <p>
 * CSRF is handled exactly as the browser UI does it: read the {@code XSRF-TOKEN} cookie from any response and send it
 * back as the {@code X-XSRF-TOKEN} header. (Spring Security's test {@code csrf()} helper is deliberately not used: it
 * swaps the filter's cookie repository for a session-based one, which would hide a broken cookie setup.)
 */
@SpringBootTest(properties = { "recsys.data-dir=src/test/resources/ml-tiny", "recsys.demo-user.enabled=false" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RecommenderFlowIT {

	private static final List<Integer> GRADED = List.of(1, 2, 3, 7, 10);

	@Autowired
	MockMvcTester mvc;

	@Test
	void importSearchRegisterLoginGradeRecommendMathLogout() {
		// Import: all 13 movies, titles parsed (quoted comma, moved article), tags aggregated.
		var all = assertThat(mvc.get().uri("/api/movies").param("size", "100")).hasStatusOk().bodyJson();
		all.extractingPath("$.page.totalElements").isEqualTo(13);
		assertThat(mvc.get().uri("/api/movies").param("q", "usual")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.content[0].title")
			.isEqualTo("The Usual Suspects");
		assertThat(mvc.get().uri("/api/movies/13")).hasStatusOk().bodyJson().extractingPath("$.genres").isEqualTo(List.of());
		assertThat(mvc.get().uri("/api/movies/1")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.topTags[0]")
			.isEqualTo(Map.of("tag", "pixar", "count", 2));

		// Register, then sign in.
		register("flow-user", "password123");
		MockHttpSession session = login("flow-user", "password123");
		assertThat(mvc.get().uri("/api/me").session(session)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.username")
			.isEqualTo("flow-user");

		// Grade five films.
		int[] scores = { 9, 8, 3, 2, 9 };
		for (int n = 0; n < GRADED.size(); n++) {
			assertThat(grade(session, GRADED.get(n), scores[n])).hasStatusOk();
		}
		assertThat(mvc.get().uri("/api/me/grades").session(session)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.length()")
			.isEqualTo(5);

		// Both methods recommend, from five grades, never a graded film.
		for (String method : List.of("lecture", "improved")) {
			var recommendations = assertThat(
					mvc.get().uri("/api/me/recommendations").param("method", method).param("k", "5").session(session))
				.hasStatusOk()
				.bodyJson();
			recommendations.extractingPath("$.fallback").isEqualTo("NONE");
			recommendations.extractingPath("$.gradeCount").isEqualTo(5);
			recommendations.extractingPath("$.items.length()").isEqualTo(5);
			recommendations.extractingPath("$.items[*].movieId").asArray().doesNotContainAnyElementsOf(GRADED);
			recommendations.extractingPath("$.items[0].explanation.sentence").asString().isNotBlank();
		}

		// Show the math.
		assertThat(mvc.get().uri("/api/me/recommendations/math").param("method", "improved").session(session))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.contentBased.rows.length()")
			.isEqualTo(5);

		// Sign out: the same session no longer reaches "my" data.
		Cookie token = csrfToken(session);
		assertThat(mvc.post().uri("/logout").session(session).cookie(token).header("X-XSRF-TOKEN", token.getValue()))
			.hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri("/api/me/grades").session(session)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void signedOutIs401AndSignedInIs200() {
		assertThat(mvc.get().uri("/api/me/grades")).hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);

		register("door-user", "password123");
		assertThat(mvc.get().uri("/api/me/grades").session(login("door-user", "password123"))).hasStatusOk();
	}

	@Test
	void wrongPasswordIsRejected() {
		register("careful-user", "password123");

		assertThat(postLogin("careful-user", "wrong-password")).hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void twoUsersGradesNeverMix() {
		register("alice", "password123");
		register("bob", "password123");
		MockHttpSession alice = login("alice", "password123");
		MockHttpSession bob = login("bob", "password123");

		assertThat(grade(alice, 12, 10)).hasStatusOk();
		assertThat(grade(bob, 4, 7)).hasStatusOk();

		assertThat(mvc.get().uri("/api/me/grades").session(alice)).bodyJson()
			.extractingPath("$[*].movieId")
			.isEqualTo(List.of(12));
		assertThat(mvc.get().uri("/api/me/grades").session(bob)).bodyJson()
			.extractingPath("$[*].movieId")
			.isEqualTo(List.of(4));
	}

	@Test
	void writesNeedTheCsrfCookieEchoedAsAHeader() {
		register("csrf-user", "password123");
		MockHttpSession session = login("csrf-user", "password123");

		// Without a token: refused, and nothing is saved.
		assertThat(mvc.put().uri("/api/me/grades/5").session(session).contentType(MediaType.APPLICATION_JSON)
			.content("{\"score\": 6}")).hasStatus(HttpStatus.FORBIDDEN);
		// A header that does not match the cookie: refused too.
		Cookie token = csrfToken(session);
		assertThat(mvc.put().uri("/api/me/grades/5").session(session).cookie(token).header("X-XSRF-TOKEN", "forged")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"score\": 6}")).hasStatus(HttpStatus.FORBIDDEN);

		// What the browser does: the cookie is readable by JavaScript, and echoing it as a header is accepted.
		assertThat(token.isHttpOnly()).as("readable by JavaScript").isFalse();
		assertThat(grade(session, 5, 6)).hasStatusOk();
		assertThat(mvc.get().uri("/api/me/grades").session(session)).bodyJson()
			.extractingPath("$[*].movieId")
			.isEqualTo(List.of(5));
	}

	/** Any response carries the XSRF-TOKEN cookie, as the UI relies on. */
	private Cookie csrfToken(MockHttpSession session) {
		MockMvcTester.MockMvcRequestBuilder page = mvc.get().uri("/api/genres");
		MvcTestResult result = (session == null ? page : page.session(session)).exchange();
		Cookie token = result.getResponse().getCookie("XSRF-TOKEN");
		assertThat(token).as("XSRF-TOKEN cookie on a plain GET").isNotNull();
		return token;
	}

	private void register(String username, String password) {
		Cookie token = csrfToken(null);
		assertThat(mvc.post().uri("/api/auth/register").cookie(token).header("X-XSRF-TOKEN", token.getValue())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}"))
			.hasStatus(HttpStatus.CREATED);
	}

	private MockHttpSession login(String username, String password) {
		MvcTestResult result = postLogin(username, password);
		assertThat(result).hasStatusOk().bodyJson().extractingPath("$.username").isEqualTo(username);
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private MvcTestResult postLogin(String username, String password) {
		Cookie token = csrfToken(null);
		return mvc.post().uri("/login").cookie(token).header("X-XSRF-TOKEN", token.getValue())
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("username", username)
			.param("password", password)
			.exchange();
	}

	private MvcTestResult grade(MockHttpSession session, int movieId, int score) {
		Cookie token = csrfToken(session);
		return mvc.put().uri("/api/me/grades/{id}", movieId).session(session).cookie(token)
			.header("X-XSRF-TOKEN", token.getValue())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"score\": " + score + "}")
			.exchange();
	}
}
