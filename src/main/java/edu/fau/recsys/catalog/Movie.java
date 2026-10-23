package edu.fau.recsys.catalog;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A MovieLens movie. The ID is the MovieLens {@code movieId} (assigned, never generated).
 * {@code ratingCount} and {@code ratingMean} are filled from {@code ml_rating} after import.
 */
@Entity
class Movie {

	@Id
	private Integer id;

	/** The title exactly as in {@code movies.csv}, e.g. {@code Matrix, The (1999)}. */
	@Column(nullable = false)
	private String title;

	/** The readable title without the year, e.g. {@code The Matrix}. */
	@Column(nullable = false)
	private String displayTitle;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	private Integer year;

	private Integer tmdbId;

	private int ratingCount;

	@Column(precision = 3, scale = 2)
	private BigDecimal ratingMean;

	/** Loaded lazily in batches ({@code hibernate.default_batch_fetch_size}), never fetch-joined with paging. */
	@ManyToMany
	@JoinTable(name = "movie_genre", joinColumns = @JoinColumn(name = "movie_id"),
			inverseJoinColumns = @JoinColumn(name = "genre_id"))
	@OrderBy("id")
	private Set<Genre> genres = new LinkedHashSet<>();

	protected Movie() {
	}

	Movie(int id, String title, String displayTitle, Integer year, Integer tmdbId) {
		this.id = id;
		this.title = title;
		this.displayTitle = displayTitle;
		this.year = year;
		this.tmdbId = tmdbId;
	}

	void addGenre(Genre genre) {
		genres.add(genre);
	}

	Integer getId() {
		return id;
	}

	String getTitle() {
		return title;
	}

	String getDisplayTitle() {
		return displayTitle;
	}

	Integer getYear() {
		return year;
	}

	Integer getTmdbId() {
		return tmdbId;
	}

	int getRatingCount() {
		return ratingCount;
	}

	BigDecimal getRatingMean() {
		return ratingMean;
	}

	Set<Genre> getGenres() {
		return genres;
	}
}
