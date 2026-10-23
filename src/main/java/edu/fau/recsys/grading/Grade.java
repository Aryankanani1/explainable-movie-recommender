package edu.fau.recsys.grading;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One user's grade (1–10) for one movie. Written by {@link GradeRepository#upsert}, read through JPA. */
@Entity
class Grade {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Integer movieId;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(nullable = false)
	private int score;

	@Column(nullable = false)
	private Instant updatedAt;

	protected Grade() {
	}

	Integer getMovieId() {
		return movieId;
	}

	int getScore() {
		return score;
	}
}
