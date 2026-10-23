package edu.fau.recsys.web;

import edu.fau.recsys.grading.AccountService;
import edu.fau.recsys.grading.CurrentUser;
import edu.fau.recsys.grading.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registration and "who am I". Signing in and out are Spring Security's {@code POST /login} and {@code /logout}. */
@RestController
class AuthController {

	private final AccountService accounts;

	private final CurrentUserProvider currentUserProvider;

	AuthController(AccountService accounts, CurrentUserProvider currentUserProvider) {
		this.accounts = accounts;
		this.currentUserProvider = currentUserProvider;
	}

	/** Creates an account; the UI then signs in with it. A taken username → 409 ProblemDetail. */
	@PostMapping("/api/auth/register")
	@ResponseStatus(HttpStatus.CREATED)
	UserDto register(@Valid @RequestBody RegisterRequest request) {
		return UserDto.from(accounts.register(request.username(), request.password()));
	}

	@GetMapping("/api/me")
	UserDto me() {
		return UserDto.from(currentUserProvider.currentUser());
	}

	record RegisterRequest(
			@NotNull(message = "is required") @Pattern(regexp = "[A-Za-z0-9._-]{3,30}",
					message = "must be 3 to 30 letters, digits, dots, dashes or underscores") String username,
			@NotNull(message = "is required") @Size(min = 8, max = 72,
					message = "must be 8 to 72 characters") String password) {
	}

	record UserDto(String username) {

		static UserDto from(CurrentUser user) {
			return new UserDto(user.username());
		}
	}
}
