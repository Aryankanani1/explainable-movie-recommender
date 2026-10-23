package edu.fau.recsys.grading;

import java.time.Instant;

/** A grade with the movie's display title and year, for "My grades". */
public record GradeView(int movieId, String title, Integer year, int score, Instant updatedAt) {
}
