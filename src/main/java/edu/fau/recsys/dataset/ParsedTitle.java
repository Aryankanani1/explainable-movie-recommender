package edu.fau.recsys.dataset;

/** A MovieLens title split into a human-readable title and its release year (null if the title has none). */
record ParsedTitle(String displayTitle, Integer year) {
}
