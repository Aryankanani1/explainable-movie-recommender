package edu.fau.recsys.grading;

/** The user a request acts for ("me" in {@code /api/me/**}). */
public record CurrentUser(long id, String username) {
}
