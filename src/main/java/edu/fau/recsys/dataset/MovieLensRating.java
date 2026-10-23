package edu.fau.recsys.dataset;

/** One row of {@code ratings.csv}: 0.5–5.0 stars, timestamp in epoch seconds (UTC). */
public record MovieLensRating(int userId, int movieId, double rating, long timestamp) {
}
