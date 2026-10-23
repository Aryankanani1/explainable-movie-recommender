package edu.fau.recsys.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/** A normalized tag on a movie, with the number of times MovieLens users applied it. Written by the importer via JDBC. */
@Entity
class MovieTag {

	@EmbeddedId
	private MovieTagId id;

	@Column(nullable = false)
	private int count;

	protected MovieTag() {
	}

	MovieTagId getId() {
		return id;
	}

	int getCount() {
		return count;
	}
}
