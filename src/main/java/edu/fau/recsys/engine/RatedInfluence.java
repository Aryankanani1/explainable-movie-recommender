package edu.fau.recsys.engine;

/** A graded movie that pushed a recommendation up: {@code influence} = weight × cosine similarity. */
public record RatedInfluence(int movieId, String title, double grade, double influence) {
}
