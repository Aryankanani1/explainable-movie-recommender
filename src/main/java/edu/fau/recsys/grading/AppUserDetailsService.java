package edu.fau.recsys.grading;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lets Spring Security sign people in against {@code app_user}. Accounts without a password cannot sign in. */
@Service
class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository users;

	AppUserDetailsService(AppUserRepository users) {
		this.users = users;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		AppUser user = users.findByUsername(AccountService.normalize(username))
			.filter((u) -> u.getPasswordHash() != null)
			.orElseThrow(() -> new UsernameNotFoundException(username));
		return User.withUsername(user.getUsername()).password(user.getPasswordHash()).roles("USER").build();
	}
}
