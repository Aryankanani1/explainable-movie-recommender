package edu.fau.recsys.evaluation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.fau.recsys.dataset.MovieLensMovie;
import edu.fau.recsys.dataset.MovieLensRating;
import edu.fau.recsys.dataset.TagCounts;
import edu.fau.recsys.engine.CatalogSnapshot;
import edu.fau.recsys.engine.ContentBasedRecommender;
import edu.fau.recsys.engine.LectureFixture;
import edu.fau.recsys.engine.LectureRecommender;
import edu.fau.recsys.engine.MathTrace;
import edu.fau.recsys.engine.MmrReranker;
import edu.fau.recsys.engine.RatingScale;
import edu.fau.recsys.engine.RecommendOptions;
import edu.fau.recsys.engine.Recommendation;
import edu.fau.recsys.engine.RecommendationResult;
import edu.fau.recsys.engine.ScoringConfig;
import edu.fau.recsys.engine.UserRatings;

/**
 * Tables and numbers for the report (ticket 10): personas, their recommendations, worked calculations, database
 * excerpts and the evaluation results. Material only, no interpretation. Every output is deterministic.
 */
final class ReportAssets {

	static final int K = 10;

	static final int TRACE_K = 5;

	/** The app's improved method: centred grades, IDF, cosine on genres (recsys.recommend.tag-weight = 0). */
	static final ScoringConfig IMPROVED = ScoringConfig.improved(0);

	static final double[] SWEEP = { 1.0, 0.9, 0.7, 0.5, 0.3 };

	private static final String CITATION = "F. Maxwell Harper and Joseph A. Konstan. 2015. The MovieLens Datasets: "
			+ "History and Context. ACM Transactions on Interactive Intelligent Systems (TiiS) 5, 4: 19:1–19:19. "
			+ "https://doi.org/10.1145/2827872";

	private final EvaluationData data;

	private final CatalogSnapshot catalog;

	private final List<Resolved> personas;

	private final LectureRecommender lecture = new LectureRecommender();

	private final ContentBasedRecommender improved = new ContentBasedRecommender(IMPROVED);

	private final MmrReranker mmr = new MmrReranker();

	/** @throws IllegalStateException naming every persona title that is not in {@code movies.csv} */
	ReportAssets(EvaluationData data, List<Personas.Persona> personas) {
		this.data = data;
		this.catalog = data.fullSnapshot();
		Map<String, MovieLensMovie> byTitle = new TreeMap<>();
		for (MovieLensMovie movie : data.movieList()) {
			byTitle.put(movie.rawTitle().strip(), movie);
		}
		List<String> missing = new ArrayList<>();
		List<Resolved> resolved = new ArrayList<>();
		for (Personas.Persona persona : personas) {
			List<MovieLensMovie> movies = new ArrayList<>();
			Map<Integer, Double> grades = new TreeMap<>();
			for (Personas.Grade grade : persona.grades()) {
				MovieLensMovie movie = byTitle.get(grade.title());
				if (movie == null) {
					missing.add(persona.name() + ": " + grade.title());
					continue;
				}
				movies.add(movie);
				grades.put(movie.movieId(), (double) grade.grade());
			}
			resolved.add(new Resolved(persona, movies, new UserRatings(grades, RatingScale.APP_GRADES)));
		}
		if (!missing.isEmpty()) {
			throw new IllegalStateException("Persona titles not found in movies.csv (they must match exactly): " + missing);
		}
		this.personas = List.copyOf(resolved);
	}

	/** Writes every asset to {@code out}; {@code sweep} adds the MMR λ sweep (about 10 s). */
	void writeAll(Path out, Path resultsDir, boolean sweep) throws IOException {
		Files.createDirectories(out);
		write(out, "personas.md", personasMd());
		write(out, "recommendations.md", recommendationsMd());
		write(out, "calculations.md", calculationsMd());
		write(out, "database.md", databaseMd());
		List<double[]> sweepRows = sweep ? mmrSweep() : List.of();
		write(out, "metrics.md", metricsMd(resultsDir, sweepRows));
		write(out, "metrics-chart.csv", metricsChartCsv(resultsDir));
		if (sweep) {
			write(out, "mmr-sweep.csv", sweepCsv(sweepRows));
		}
	}

	// ---------- personas.md ----------

	String personasMd() {
		StringBuilder md = new StringBuilder("# Personas\n\n");
		md.append("Four example viewers. Titles are real MovieLens titles; grades are on the lecture's 1–10 scale and "
				+ "were chosen to give each persona a clear taste, with at least two low grades each.\n");
		int n = 1;
		for (Resolved p : personas) {
			md.append("\n## ").append(n++).append(". ").append(p.persona().name()).append("\n\n");
			md.append(p.persona().description()).append("\n\n");
			List<List<String>> rows = new ArrayList<>();
			double sum = 0;
			for (int i = 0; i < p.movies().size(); i++) {
				MovieLensMovie movie = p.movies().get(i);
				int grade = p.persona().grades().get(i).grade();
				sum += grade;
				rows.add(List.of(movie.displayTitle(), year(movie.year()), String.join(", ", movie.genres()),
						String.valueOf(movie.movieId()), String.valueOf(grade)));
			}
			md.append(Markdown.table(List.of("Film", "Year", "Genres", "movieId", "Grade"), rows, "lrlrr"));
			md.append("\nMean grade r̄ = ").append(Markdown.fixed(sum / p.movies().size(), 2)).append(".\n");
		}
		return md.toString();
	}

	// ---------- recommendations.md ----------

	String recommendationsMd() {
		StringBuilder md = new StringBuilder("# Recommendations for the personas\n\n");
		md.append("Top ").append(K).append(" unseen films out of ").append(String.format(Locale.ROOT, "%,d", catalog.size()))
			.append(" for each persona: the lecture method; the improved method (centred grades, IDF-weighted genres, "
					+ "cosine; tag weight 0, as in the app); and the improved method re-ranked for variety by MMR "
					+ "(λ = ")
			.append(Markdown.number(MethodRegistry.MMR_LAMBDA))
			.append(", over its top ").append(Evaluator.MMR_POOL).append("). Rating counts are from all MovieLens "
					+ "ratings. Each film's explanation is the same whether or not MMR re-ranked it.\n");
		int n = 1;
		for (Resolved p : personas) {
			RecommendOptions options = new RecommendOptions(K, 0, 1.0, true);
			RecommendationResult byLecture = lecture.recommend(catalog, p.ratings(), options);
			RecommendationResult byImproved = improved.recommend(catalog, p.ratings(), options);
			RecommendationResult byMmr = mmr.diversify(improved, catalog, p.ratings(),
					new RecommendOptions(K, 0, MethodRegistry.MMR_LAMBDA, true), Evaluator.MMR_POOL);

			md.append("\n## ").append(n++).append(". ").append(p.persona().name()).append("\n\n");
			List<List<String>> rows = new ArrayList<>();
			for (int r = 0; r < K; r++) {
				Recommendation a = byLecture.items().get(r);
				Recommendation b = byImproved.items().get(r);
				Recommendation c = byMmr.items().get(r);
				rows.add(List.of(String.valueOf(r + 1), label(a.movieId()), Markdown.fixed(a.score() * 10, 2),
						label(b.movieId()), Markdown.fixed(b.score(), 4), label(c.movieId()), Markdown.fixed(c.score(), 4)));
			}
			md.append(Markdown.table(List.of("#", "Lecture", "Score ×10", "Improved", "Score",
					"Improved + MMR (λ = " + Markdown.number(MethodRegistry.MMR_LAMBDA) + ")", "Score"), rows, "rlrlrlr"));
			md.append("\nDistinct genres in each top ").append(K).append(": lecture ").append(genreCount(byLecture))
				.append(", improved ").append(genreCount(byImproved)).append(", improved + MMR ")
				.append(genreCount(byMmr)).append(".\n");
			md.append("\n### Why, improved method\n\n").append(sentences(byImproved));
			md.append("\n### Why, lecture method\n\n").append(sentences(byLecture));
		}
		return md.toString();
	}

	// ---------- calculations.md ----------

	String calculationsMd() {
		StringBuilder md = new StringBuilder("# Examples of calculations\n\n");
		md.append("Every number below was computed by the engine that ranks the films (the same classes as the app). "
				+ "Matrices show only the genres of the graded films and of the candidates shown.\n");

		md.append("\n## A. The lecture's worked example (slides), reproduced\n\n");
		md.append("Grades: Batman v Superman 2, Guardians of the Galaxy 10, Captain America: Civil War 8. "
				+ "Candidates: the four other films of the example.\n");
		md.append(lectureTables(lecture.trace(LectureFixture.catalog(), LectureFixture.defaultGrades(), 4)));

		Resolved first = personas.get(0);
		md.append("\n## B. Persona 1 (").append(first.persona().name()).append("), lecture method\n");
		md.append(lectureTables(lecture.trace(catalog, first.ratings(), TRACE_K)));

		md.append("\n## C. Persona 1 (").append(first.persona().name()).append("), improved method\n");
		md.append(contentTables(improved.trace(catalog, first.ratings(), TRACE_K)));
		return md.toString();
	}

	private static String lectureTables(MathTrace trace) {
		MathTrace.Lecture t = trace.lecture();
		List<String> columns = trace.columns();
		StringBuilder md = new StringBuilder();

		md.append("\n### Step 1. Genre matrix x (1 = the film has the genre) and grades r\n\n");
		List<List<String>> rows = new ArrayList<>();
		for (MathTrace.GradedRow row : t.rows()) {
			List<String> cells = new ArrayList<>(List.of(row.title(), Markdown.number(row.grade())));
			for (double v : row.x()) {
				cells.add(Markdown.number(v));
			}
			rows.add(cells);
		}
		md.append(Markdown.table(concat(List.of("Film", "r"), columns), rows));

		md.append("\n### Step 2. Weighted genre matrix r · x, column sums P and profile p̂ = P / T\n\n");
		rows = new ArrayList<>();
		for (MathTrace.GradedRow row : t.rows()) {
			List<String> cells = new ArrayList<>(List.of(row.title()));
			for (double v : row.weighted()) {
				cells.add(Markdown.number(v));
			}
			rows.add(cells);
		}
		rows.add(concat(List.of("**P** (column sums)"), format(t.profile().P(), -1)));
		rows.add(concat(List.of("**p̂ = P / T**"), format(t.profile().pHat(), 4)));
		md.append(Markdown.table(concat(List.of("Film"), columns), rows));
		md.append("\nT = Σ P = ").append(Markdown.number(t.profile().T())).append(".\n");

		md.append("\n### Step 3. Scores: score(c) = Σ_g p̂_g · x_cg (each cell is p̂_g · x_cg)\n\n");
		rows = new ArrayList<>();
		for (MathTrace.CandidateRow candidate : t.candidates()) {
			List<String> cells = new ArrayList<>(List.of(candidate.title()));
			cells.addAll(format(candidate.contrib(), 4));
			cells.add(Markdown.fixed(candidate.score(), 4));
			cells.add(Markdown.fixed(candidate.scoreTimes10(), 2));
			rows.add(cells);
		}
		md.append(Markdown.table(concat(concat(List.of("Film"), columns), List.of("Score", "×10")), rows));
		return md.toString();
	}

	private static String contentTables(MathTrace trace) {
		MathTrace.ContentBased c = trace.contentBased();
		List<String> columns = trace.columns();
		StringBuilder md = new StringBuilder();

		md.append("\n### Step 1. Centred weights w = r − r̄, with r̄ = ").append(Markdown.fixed(c.rBar(), 2))
			.append("\n\n");
		List<List<String>> rows = new ArrayList<>();
		for (MathTrace.ContentRow row : c.rows()) {
			rows.add(List.of(row.title(), Markdown.number(row.grade()), Markdown.fixed(row.w(), 2)));
		}
		md.append(Markdown.table(List.of("Film", "r", "w"), rows));

		md.append("\n### Step 2. Genre rarity: idf_g = ln(N / n_g)\n\n");
		md.append(Markdown.table(concat(List.of(""), columns), List.of(concat(List.of("idf"), format(c.idf(), 3)))));

		md.append("\n### Step 3. Normalized genre vectors ĝ (IDF-weighted, length 1)\n\n");
		rows = new ArrayList<>();
		for (MathTrace.ContentRow row : c.rows()) {
			rows.add(concat(List.of(row.title()), format(row.gHat(), 3)));
		}
		md.append(Markdown.table(concat(List.of("Film"), columns), rows));

		md.append("\n### Step 4. Weighted vectors w · ĝ and the profile p = Σ w · ĝ\n\n");
		rows = new ArrayList<>();
		for (MathTrace.ContentRow row : c.rows()) {
			rows.add(concat(List.of(row.title()), format(row.wGHat(), 3)));
		}
		rows.add(concat(List.of("**p**"), format(c.p(), 3)));
		md.append(Markdown.table(concat(List.of("Film"), columns), rows));

		md.append("\n### Step 5. Scores: score(c) = cos(p, v_c) (each cell is p_g · v_cg / (‖p‖ · ‖v_c‖))\n\n");
		rows = new ArrayList<>();
		for (MathTrace.ContentCandidate candidate : c.candidates()) {
			List<String> cells = new ArrayList<>(List.of(candidate.title()));
			cells.addAll(format(candidate.contrib(), 4));
			cells.add(Markdown.fixed(candidate.score(), 4));
			rows.add(cells);
		}
		md.append(Markdown.table(concat(concat(List.of("Film"), columns), List.of("Score")), rows));
		return md.toString();
	}

	// ---------- database.md ----------

	String databaseMd() {
		List<MovieLensMovie> movies = data.movieList();
		List<MovieLensRating> ratings = data.allRatings();
		TagCounts tags = data.tagCounts();

		StringBuilder md = new StringBuilder("# Elements of the database\n\n");
		md.append("MovieLens ml-latest-small as imported: the year is split off each title and moved leading "
				+ "articles are put back (\"Matrix, The\" → \"The Matrix\"); tags are lowercased, trimmed and counted.\n");

		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		for (MovieLensRating rating : ratings) {
			min = Math.min(min, rating.timestamp());
			max = Math.max(max, rating.timestamp());
		}
		int withoutGenres = 0;
		for (MovieLensMovie movie : movies) {
			if (movie.genres().isEmpty()) {
				withoutGenres++;
			}
		}
		SortedMap<String, int[]> perTag = new TreeMap<>();
		for (Map.Entry<Integer, SortedMap<String, Integer>> movie : tags.byMovie().entrySet()) {
			for (Map.Entry<String, Integer> tag : movie.getValue().entrySet()) {
				int[] counts = perTag.computeIfAbsent(tag.getKey(), t -> new int[2]);
				counts[0] += tag.getValue();
				counts[1]++;
			}
		}

		md.append("\n## Totals\n\n");
		md.append(Markdown.table(List.of("Item", "Value"), List.of(
				List.of("Movies", count(movies.size())),
				List.of("Ratings (0.5–5.0 stars, half-star steps)", count(ratings.size())),
				List.of("Users who rated", count(data.users())),
				List.of("Ratings from – to", date(min) + " – " + date(max)),
				List.of("Genres", count(catalog.genreNames().size())),
				List.of("Movies with no genres listed", count(withoutGenres)),
				List.of("Tag applications", count(tags.applications())),
				List.of("Distinct (movie, tag) pairs", count(tags.pairs())),
				List.of("Distinct tags", count(perTag.size())),
				List.of("Tags on at least 2 movies (the tag vocabulary)", count(catalog.tagVocabulary().size())),
				List.of("Movies with a vocabulary tag", count(catalog.moviesWithVocabularyTag())))));

		md.append("\n## The first 15 rows of movies.csv, parsed\n\n");
		List<List<String>> rows = new ArrayList<>();
		for (MovieLensMovie movie : movies.subList(0, Math.min(15, movies.size()))) {
			rows.add(List.of(String.valueOf(movie.movieId()), movie.rawTitle().strip(), movie.displayTitle(),
					year(movie.year()), String.join(", ", movie.genres())));
		}
		md.append(Markdown.table(List.of("movieId", "Title in movies.csv", "Display title", "Year", "Genres"), rows, "rllrl"));

		md.append("\n## Genres, with how many movies have each and their IDF weight\n\n");
		rows = new ArrayList<>();
		double[] idf = catalog.genreIdf();
		List<String> genres = catalog.genreNames();
		for (int g = 0; g < genres.size(); g++) {
			int n = 0;
			for (MovieLensMovie movie : movies) {
				if (movie.genres().contains(genres.get(g))) {
					n++;
				}
			}
			rows.add(List.of(genres.get(g), count(n), Markdown.fixed(idf[g], 3)));
		}
		md.append(Markdown.table(List.of("Genre", "Movies", "idf = ln(N / n)"), rows));

		md.append("\n## The 10 most-applied tags\n\n");
		List<Map.Entry<String, int[]>> ranked = new ArrayList<>(perTag.entrySet());
		ranked.sort((a, b) -> a.getValue()[0] != b.getValue()[0] ? Integer.compare(b.getValue()[0], a.getValue()[0])
				: a.getKey().compareTo(b.getKey()));
		rows = new ArrayList<>();
		for (Map.Entry<String, int[]> tag : ranked.subList(0, Math.min(10, ranked.size()))) {
			rows.add(List.of(tag.getKey(), count(tag.getValue()[0]), count(tag.getValue()[1])));
		}
		md.append(Markdown.table(List.of("Tag", "Applications", "Movies"), rows));

		md.append("\n## Citation\n\n").append(CITATION).append("\n");
		return md.toString();
	}

	// ---------- metrics.md, metrics-chart.csv, mmr-sweep.csv ----------

	String metricsMd(Path resultsDir, List<double[]> sweep) throws IOException {
		String results = Files.readString(resultsDir.resolve("metrics.md"), StandardCharsets.UTF_8);
		String runInfo = Files.readString(resultsDir.resolve("run-info.json"), StandardCharsets.UTF_8);
		Matcher commit = Pattern.compile("\"gitCommit\": \"([^\"]*)\"").matcher(runInfo);
		StringBuilder md = new StringBuilder("# Offline evaluation results\n\n");
		md.append("Copied from results/metrics.md, written by EvaluationRunner at commit ")
			.append(commit.find() ? commit.group(1) : "unknown").append(".\n\n");
		md.append(results.replaceFirst("^# Offline evaluation\\s*", "").replaceAll("(?m)^## ", "### "));
		if (!sweep.isEmpty()) {
			md.append("\n## MMR trade-off: cb-idf re-ranked with different λ (same protocol, top ")
				.append(Evaluator.MMR_POOL).append(")\n\n");
			List<List<String>> rows = new ArrayList<>();
			for (double[] row : sweep) {
				rows.add(List.of(Markdown.number(row[0]), Markdown.fixed(row[1], 4), Markdown.fixed(row[2], 4),
						Markdown.fixed(row[3], 4), Markdown.fixed(row[4], 2)));
			}
			md.append(Markdown.table(List.of("λ", "NDCG@10", "ILD", "Coverage", "Novelty"), rows));
		}
		return md.toString();
	}

	/** method, NDCG@10, coverage, ILD from results/metrics.csv, for charting. */
	String metricsChartCsv(Path resultsDir) throws IOException {
		List<String> lines = Files.readAllLines(resultsDir.resolve("metrics.csv"), StandardCharsets.UTF_8);
		List<String> header = List.of(lines.get(0).split(","));
		int method = header.indexOf("method");
		int ndcg = header.indexOf("NDCG@" + K);
		int coverage = header.indexOf("coverage");
		int ild = header.indexOf("ILD");
		if (method < 0 || ndcg < 0 || coverage < 0 || ild < 0) {
			throw new IllegalStateException("Unexpected header in results/metrics.csv: " + lines.get(0));
		}
		StringBuilder csv = new StringBuilder("method,NDCG@" + K + ",coverage,ILD\n");
		for (String line : lines.subList(1, lines.size())) {
			String[] cells = line.split(",");
			csv.append(cells[method]).append(',').append(cells[ndcg]).append(',').append(cells[coverage]).append(',')
				.append(cells[ild]).append('\n');
		}
		return csv.toString();
	}

	/** Rows of {λ, NDCG@10, ILD, coverage, novelty} for cb-idf + MMR, on the evaluation's train/test split. */
	List<double[]> mmrSweep() {
		List<UserSplit> splits = data.splits();
		Evaluator evaluator = new Evaluator(data.snapshot(splits), splits, K, EvaluationRunner.RELEVANCE_THRESHOLD);
		EvaluatedMethod cbIdf = MethodRegistry.contentBased("cb-idf", IMPROVED);
		List<double[]> rows = new ArrayList<>();
		for (double lambda : SWEEP) {
			MethodResult result = evaluator.run(MethodRegistry.diversified(cbIdf, lambda));
			rows.add(new double[] { lambda, result.ndcg(), result.ild(), result.coverage(), result.novelty() });
		}
		return rows;
	}

	private static String sweepCsv(List<double[]> sweep) {
		StringBuilder csv = new StringBuilder("lambda,NDCG@" + K + ",ILD,coverage,novelty\n");
		for (double[] row : sweep) {
			csv.append(Markdown.number(row[0])).append(',').append(Markdown.fixed(row[1], 4)).append(',')
				.append(Markdown.fixed(row[2], 4)).append(',').append(Markdown.fixed(row[3], 4)).append(',')
				.append(Markdown.fixed(row[4], 4)).append('\n');
		}
		return csv.toString();
	}

	// ---------- helpers ----------

	private String label(int movieId) {
		int idx = catalog.indexOf(movieId);
		Integer year = catalog.year(idx);
		return catalog.title(idx) + (year != null ? " (" + year + ")" : "");
	}

	private int genreCount(RecommendationResult result) {
		Set<String> genres = new LinkedHashSet<>();
		for (Recommendation item : result.items()) {
			genres.addAll(catalog.genres(catalog.indexOf(item.movieId())));
		}
		return genres.size();
	}

	private String sentences(RecommendationResult result) {
		StringBuilder md = new StringBuilder();
		for (Recommendation item : result.items()) {
			md.append(item.rank()).append(". **").append(label(item.movieId())).append("**: ")
				.append(item.explanation().sentence()).append('\n');
		}
		return md.toString();
	}

	/** {@code digits < 0}: whole numbers without decimals. */
	private static List<String> format(double[] values, int digits) {
		List<String> cells = new ArrayList<>();
		for (double v : values) {
			cells.add(digits < 0 ? Markdown.number(v) : Markdown.fixed(v, digits));
		}
		return cells;
	}

	private static List<String> concat(List<String> a, List<String> b) {
		List<String> all = new ArrayList<>(a);
		all.addAll(b);
		return all;
	}

	private static String year(Integer year) {
		return year == null ? "" : String.valueOf(year);
	}

	private static String count(int n) {
		return String.format(Locale.ROOT, "%,d", n);
	}

	private static String date(long epochSeconds) {
		return Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC).toLocalDate().toString();
	}

	private static void write(Path dir, String name, String content) throws IOException {
		Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
	}

	record Resolved(Personas.Persona persona, List<MovieLensMovie> movies, UserRatings ratings) {
	}
}
