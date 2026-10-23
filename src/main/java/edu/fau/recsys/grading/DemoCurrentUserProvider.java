package edu.fau.recsys.grading;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Every request is the {@code demo} user, created on first use. Replaced in ticket 08. */
@Component
class DemoCurrentUserProvider implements CurrentUserProvider {

	static final String DEMO_USERNAME = "demo";

	private final JdbcTemplate jdbcTemplate;

	private volatile CurrentUser demo;

	DemoCurrentUserProvider(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public CurrentUser currentUser() {
		CurrentUser cached = demo;
		if (cached == null) {
			// ON CONFLICT makes get-or-create safe when two first requests race.
			jdbcTemplate.update("INSERT INTO app_user (username) VALUES (?) ON CONFLICT (username) DO NOTHING",
					DEMO_USERNAME);
			Long id = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class,
					DEMO_USERNAME);
			cached = new CurrentUser(id, DEMO_USERNAME);
			demo = cached;
		}
		return cached;
	}
}
