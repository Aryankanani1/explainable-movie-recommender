package edu.fau.recsys.engine;

/** How much one feature adds to a movie's score. {@code kind} is "genre" or "tag". */
public record FeatureContribution(String feature, String kind, double contribution) {
}
