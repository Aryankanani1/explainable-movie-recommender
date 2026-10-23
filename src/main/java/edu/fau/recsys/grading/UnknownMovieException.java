package edu.fau.recsys.grading;

/** A grade was given for a movie ID that is not in the catalog. */
public class UnknownMovieException extends RuntimeException {

	private final int movieId;

	public UnknownMovieException(int movieId) {
		super("No movie with id " + movieId);
		this.movieId = movieId;
	}

	public int movieId() {
		return movieId;
	}
}
