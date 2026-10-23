package edu.fau.recsys.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A genre derived from {@code movies.csv}. IDs are assigned in alphabetical order of name. */
@Entity
class Genre {

	@Id
	@JdbcTypeCode(SqlTypes.SMALLINT)
	private Integer id;

	@Column(nullable = false, unique = true)
	private String name;

	protected Genre() {
	}

	Genre(int id, String name) {
		this.id = id;
		this.name = name;
	}

	Integer getId() {
		return id;
	}

	String getName() {
		return name;
	}
}
