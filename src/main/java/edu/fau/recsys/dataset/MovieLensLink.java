package edu.fau.recsys.dataset;

/** One row of {@code links.csv}. {@code tmdbId} is null when the file leaves it empty. */
public record MovieLensLink(int movieId, String imdbId, Integer tmdbId) {
}
