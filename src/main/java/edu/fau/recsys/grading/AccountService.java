package edu.fau.recsys.grading;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates accounts. Usernames are stored lowercased; passwords only ever as BCrypt hashes. */
@Service
public class AccountService {

	private final AppUserRepository users;

	private final PasswordEncoder passwordEncoder;

	AccountService(AppUserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	/** @throws UsernameTakenException if the (lowercased) username exists */
	@Transactional
	public CurrentUser register(String username, String rawPassword) {
		String name = normalize(username);
		if (users.existsByUsername(name)) {
			throw new UsernameTakenException(name);
		}
		try {
			AppUser user = users.saveAndFlush(new AppUser(name, passwordEncoder.encode(rawPassword)));
			return new CurrentUser(user.getId(), user.getUsername());
		}
		catch (DataIntegrityViolationException ex) {
			// Two registrations raced for the same name; the unique constraint caught it.
			throw new UsernameTakenException(name);
		}
	}

	/** Creates the account, or sets its password if it exists without one (the pre-login demo user did). */
	@Transactional
	public void ensureAccount(String username, String rawPassword) {
		String name = normalize(username);
		AppUser user = users.findByUsername(name).orElseGet(() -> new AppUser(name, null));
		if (user.getPasswordHash() == null) {
			user.setPasswordHash(passwordEncoder.encode(rawPassword));
			users.save(user);
		}
	}

	static String normalize(String username) {
		return username.strip().toLowerCase(Locale.ROOT);
	}
}
