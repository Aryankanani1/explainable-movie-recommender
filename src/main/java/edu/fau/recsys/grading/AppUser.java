package edu.fau.recsys.grading;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A person using the app. {@code passwordHash} stays null until ticket 08 adds login. */
@Entity
@Table(name = "app_user")
class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	private String passwordHash;

	@Column(nullable = false)
	private Instant createdAt;

	protected AppUser() {
	}

	Long getId() {
		return id;
	}

	String getUsername() {
		return username;
	}
}
