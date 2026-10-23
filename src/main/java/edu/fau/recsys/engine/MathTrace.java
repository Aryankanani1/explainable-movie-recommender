package edu.fau.recsys.engine;

import java.util.List;

/**
 * "Show the math" (ALGORITHMS.md §8): the slides' matrices computed for the user's actual grades. Exactly one of
 * {@code lecture} and {@code contentBased} is set, matching {@code method}; both are null on a popularity fallback.
 * Every vector is restricted to {@code columns}, in catalog order. This is output for display; the arrays are built
 * fresh for each trace.
 */
public record MathTrace(String method, Fallback fallback, List<String> columns, Lecture lecture,
		ContentBased contentBased) {

	public MathTrace {
		columns = List.copyOf(columns);
	}

	/** No usable profile: nothing to show. */
	static MathTrace empty(String method) {
		return new MathTrace(method, Fallback.POPULARITY, List.of(), null, null);
	}

	/** The slides' tables: weighted genre matrix, profile and candidate scores. */
	public record Lecture(List<GradedRow> rows, Profile profile, List<CandidateRow> candidates) {

		public Lecture {
			rows = List.copyOf(rows);
			candidates = List.copyOf(candidates);
		}
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

	/**
	 * The improved method's tables. {@code idf} is null when the config does not use IDF; {@code p} is the genre
	 * part of the profile; {@code topTags} summarizes the tag part (the 5 largest |p_t|) instead of drawing hundreds
	 * of tag columns.
	 */
	public record ContentBased(ScoringConfig config, double rBar, double[] idf, List<ContentRow> rows, double[] p,
			List<TagWeight> topTags, List<ContentCandidate> candidates) {

		public ContentBased {
			rows = List.copyOf(rows);
			topTags = List.copyOf(topTags);
			candidates = List.copyOf(candidates);
		}
	}

	/** One tag's weight p_t in the user's profile; negative means the user's low grades carry this tag. */
	public record TagWeight(String tag, double weight) {
	}

	/** A graded movie: grade r_i, weight w_i (r_i − r̄ when centered), genre row x_i, ĝ_i and w_i·ĝ_i. */
	public record ContentRow(int movieId, String title, double grade, double w, double[] x, double[] gHat,
			double[] wGHat) {
	}

	/**
	 * A shown candidate: ĝ_c, its score, and contrib_g = p_g·v_cg / (‖p‖‖v_c‖) per genre. {@code genrePart} +
	 * {@code tagPart} is the cosine; {@code score} also includes any popularity prior.
	 */
	public record ContentCandidate(int movieId, String title, double[] gHat, double score, double genrePart,
			double tagPart, double[] contrib) {
	}
}
