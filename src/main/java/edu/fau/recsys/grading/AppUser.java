package edu.fau.recsys.grading;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A person using the app. {@code passwordHash} is an encoded password, e.g. {@code {bcrypt}$2a$10$...}. */
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

	AppUser(String username, String passwordHash) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.createdAt = Instant.now();
	}

	Long getId() {
		return id;
	}

	String getUsername() {
		return username;
	}

	String getPasswordHash() {
		return passwordHash;
	}

	void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
}
