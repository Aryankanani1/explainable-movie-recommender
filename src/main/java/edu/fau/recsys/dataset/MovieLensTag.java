package edu.fau.recsys.dataset;

/** One row of {@code tags.csv}, with the tag text exactly as the user wrote it. */
public record MovieLensTag(int userId, int movieId, String tag, long timestamp) {
}
