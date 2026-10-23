package edu.fau.recsys.web;

import edu.fau.recsys.grading.AccountService;
import edu.fau.recsys.grading.CurrentUser;
import edu.fau.recsys.grading.CurrentUserProvider;
import edu.fau.recsys.grading.UsernameTakenException;
import edu.fau.recsys.infra.SecurityConfig;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	AccountService accounts;

	@MockitoBean
	CurrentUserProvider currentUserProvider;

	@Test
	void registersAnAccount() {
		given(accounts.register("alice", "correct horse")).willReturn(new CurrentUser(3, "alice"));

		assertThat(register("{\"username\": \"alice\", \"password\": \"correct horse\"}")).hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.extractingPath("$.username")
			.isEqualTo("alice");
	}

	@Test
	void rejectsShortPasswordsAndOddUsernamesReadably() {
		assertThat(register("{\"username\": \"a b\", \"password\": \"short\"}")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("password must be 8 to 72 characters; "
					+ "username must be 3 to 30 letters, digits, dots, dashes or underscores");
		verifyNoInteractions(accounts);
	}

	@Test
	void aTakenUsernameIsAConflict() {
		given(accounts.register("demo", "password123")).willThrow(new UsernameTakenException("demo"));

		assertThat(register("{\"username\": \"demo\", \"password\": \"password123\"}")).hasStatus(HttpStatus.CONFLICT)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void registeringNeedsTheCsrfTokenToo() {
		assertThat(mvc.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\": \"alice\", \"password\": \"correct horse\"}")).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	@WithMockUser("alice")
	void meIsTheSignedInUser() {
		given(currentUserProvider.currentUser()).willReturn(new CurrentUser(3, "alice"));

		assertThat(mvc.get().uri("/api/me")).hasStatusOk().bodyJson().extractingPath("$.username").isEqualTo("alice");
	}

	@Test
	void meWhenSignedOutIsUnauthorized() {
		assertThat(mvc.get().uri("/api/me")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MockMvcTester.MockMvcRequestBuilder register(String body) {
		return mvc.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body).with(csrf());
	}
}
