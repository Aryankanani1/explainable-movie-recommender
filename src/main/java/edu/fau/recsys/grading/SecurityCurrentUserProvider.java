package edu.fau.recsys.grading;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** "Me" is the signed-in user of the current request. {@code /api/me/**} is only reachable when signed in. */
@Component
class SecurityCurrentUserProvider implements CurrentUserProvider {

	private final AppUserRepository users;

	SecurityCurrentUserProvider(AppUserRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public CurrentUser currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthenticationCredentialsNotFoundException("Not signed in");
		}
		AppUser user = users.findByUsername(authentication.getName())
			.orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Unknown user " + authentication.getName()));
		return new CurrentUser(user.getId(), user.getUsername());
	}
}
