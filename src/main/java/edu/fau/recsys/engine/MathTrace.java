package edu.fau.recsys.engine;

import java.util.List;

/**
 * "Show the math" (ALGORITHMS.md §8): the slides' matrices computed for the user's actual grades.
 * Every vector is restricted to {@code columns}: the genres in the profile plus every genre of a shown candidate,
 * in catalog order. This is output for display; the arrays are built fresh for each trace.
 */
public record MathTrace(String method, Fallback fallback, List<String> columns, List<GradedRow> rows,
		Profile profile, List<CandidateRow> candidates) {

	public MathTrace {
		columns = List.copyOf(columns);
		rows = List.copyOf(rows);
		candidates = List.copyOf(candidates);
	}

	/** No usable profile: nothing to show. */
	static MathTrace empty(String method) {
		return new MathTrace(method, Fallback.POPULARITY, List.of(), List.of(),
				new Profile(new double[0], 0, new double[0]), List.of());
	}

	/** One row of the weighted genre matrix: grade r_i, genre row x_i and r_i·x_i. */
	public record GradedRow(int movieId, String title, double grade, double[] x, double[] weighted) {
	}

	/** Column sums P, their total T and the normalized profile p̂ = P / T. */
	public record Profile(double[] P, double T, double[] pHat) {
	}

	/** A shown candidate: genre row x_c, score, score × 10 (as on the slides) and per-genre contribution. */
	public record CandidateRow(int movieId, String title, double[] x, double score, double scoreTimes10,
			double[] contrib) {
	}
}
