package edu.fau.recsys.catalog;

/** A genre and how many catalog movies have it. */
public record GenreCount(String name, int movieCount) {
}
