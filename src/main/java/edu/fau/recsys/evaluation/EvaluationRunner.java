package edu.fau.recsys.evaluation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.fau.recsys.dataset.MovieLensFiles;
import edu.fau.recsys.engine.CatalogSnapshot;
import edu.fau.recsys.engine.ScoringConfig;

/**
 * Offline evaluation (EVALUATION.md): the ablation ladder on every MovieLens user, a β search on a validation split,
 * paired bootstrap intervals, and the results under {@code results/}. Plain Java: no database, no Spring.
 * <p>
 * Run with {@code ./mvnw -q compile exec:java -Dexec.mainClass=edu.fau.recsys.evaluation.EvaluationRunner}.
 * Optional arguments: data directory (default {@code data/ml-latest-small}), output directory (default
 * {@code results}).
 */
public final class EvaluationRunner {

	static final int K = 10;

	static final double RELEVANCE_THRESHOLD = 4.0;

	static final int BOOTSTRAP_RESAMPLES = 1000;

	private EvaluationRunner() {
	}

	public static void main(String[] args) throws IOException {
		Path dataDir = Path.of(args.length > 0 ? args[0] : "data/ml-latest-small");
		Path outDir = Path.of(args.length > 1 ? args[1] : "results");
		long start = System.nanoTime();

		EvaluationData data = EvaluationData.load(new MovieLensFiles(dataDir));
		List<UserSplit> splits = data.splits();
		CatalogSnapshot catalog = data.snapshot(splits);
		System.out.printf(Locale.ROOT, "Loaded %d users, %d movies, %d ratings (%d vocabulary tags)%n", data.users(),
				data.movies(), data.ratings(), catalog.tagVocabulary().size());

		Evaluator onTest = new Evaluator(catalog, splits, K, RELEVANCE_THRESHOLD);
		Map<String, String> definitions = new LinkedHashMap<>();
		Map<String, MethodResult> byId = new LinkedHashMap<>();
		for (EvaluatedMethod method : MethodRegistry.ladder()) {
			byId.put(method.id(), run(onTest, method, definitions));
		}

		BetaSearch beta = searchBeta(data, splits);
		MethodResult tuned = run(onTest, MethodRegistry.tunedTags(beta.chosen()), definitions);

		// Report order: the ladder, with the tuned row right after the row it tunes.
		List<MethodResult> results = new ArrayList<>();
		for (MethodResult result : byId.values()) {
			results.add(result);
			if (result.method().equals("cb-tags")) {
				results.add(tuned);
			}
		}

		Map<String, Bootstrap.Interval> comparisons = new LinkedHashMap<>();
		compare(comparisons, byId.get("cb-tags"), byId.get("lecture"));
		compare(comparisons, byId.get("cb-idf"), byId.get("lecture"));
		compare(comparisons, byId.get("cb-idf"), byId.get("cb-centered"));
		compare(comparisons, byId.get("cb-tags"), byId.get("cb-idf"));
		compare(comparisons, byId.get("cb-tags-mmr"), byId.get("cb-tags"));
		compare(comparisons, byId.get("cb-idf-mmr"), byId.get("cb-idf"));
		compare(comparisons, byId.get("popularity"), byId.get("cb-idf"));

		String dataLine = String.format(Locale.ROOT,
				"MovieLens ml-latest-small: %d users, %d movies, %d ratings. Per user, a seeded 80/20 split "
						+ "(new Random(42 + userId)); a hidden movie is relevant when rated ≥ %.1f; K = %d. "
						+ "Popularity counts come from train ratings only.",
				data.users(), data.movies(), data.ratings(), RELEVANCE_THRESHOLD, K);
		long totalMs = (System.nanoTime() - start) / 1_000_000;
		ResultsWriter.write(outDir, ResultsWriter.csv(results, K),
				ResultsWriter.markdown(dataLine, results, K, beta, comparisons),
				ResultsWriter.json(runInfo(data, results, definitions, beta, totalMs)));
		System.out.printf(Locale.ROOT, "Wrote %s in %.1f s%n", outDir.toAbsolutePath(), totalMs / 1000.0);
	}

	private static MethodResult run(Evaluator evaluator, EvaluatedMethod method, Map<String, String> definitions) {
		MethodResult result = evaluator.run(method);
		definitions.put(method.id(), method.definition());
		System.out.printf(Locale.ROOT, "%-14s NDCG@%d %.4f  P@%d %.4f  coverage %.4f  (%d ms)%n", method.id(), K,
				result.ndcg(), K, result.precision(), result.coverage(), result.runtimeMs());
		return result;
	}

	/** Each β on validation (train minus its last 10%, counts from that train only); ties go to the smaller β. */
	private static BetaSearch searchBeta(EvaluationData data, List<UserSplit> splits) {
		List<UserSplit> validationSplits = new ArrayList<>();
		for (UserSplit split : splits) {
			validationSplits.add(split.validation());
		}
		Evaluator onValidation = new Evaluator(data.snapshot(validationSplits), validationSplits, K,
				RELEVANCE_THRESHOLD);
		double[] grid = MethodRegistry.BETA_GRID;
		double[] ndcg = new double[grid.length];
		int best = 0;
		int users = 0;
		for (int g = 0; g < grid.length; g++) {
			MethodResult result = onValidation
				.run(MethodRegistry.contentBased("validation", new ScoringConfig(true, true, grid[g], 0)));
			ndcg[g] = result.ndcg();
			users = result.usersEvaluated();
			if (ndcg[g] > ndcg[best]) {
				best = g;
			}
			System.out.printf(Locale.ROOT, "  validation β=%-4s NDCG@%d %.4f%n", grid[g], K, ndcg[g]);
		}
		return new BetaSearch(grid, ndcg, users, grid[best]);
	}

	private static void compare(Map<String, Bootstrap.Interval> comparisons, MethodResult a, MethodResult b) {
		comparisons.put(a.method() + " − " + b.method(),
				Bootstrap.pairedDifference(a.ndcgPerUser(), b.ndcgPerUser(), BOOTSTRAP_RESAMPLES, UserSplit.SEED));
	}

	private static Map<String, Object> runInfo(EvaluationData data, List<MethodResult> results,
			Map<String, String> definitions, BetaSearch beta, long totalMs) {
		Map<String, Object> runtimes = new LinkedHashMap<>();
		for (MethodResult result : results) {
			runtimes.put(result.method(), result.runtimeMs());
		}
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("seed", UserSplit.SEED);
		info.put("k", K);
		info.put("relevanceThreshold", RELEVANCE_THRESHOLD);
		info.put("split", "per user: sort by movieId, shuffle with new Random(42 + userId), first floor(0.8 n) train");
		info.put("validation", "last 10% of each user's train split; counts from the remaining 90%");
		info.put("users", data.users());
		info.put("movies", data.movies());
		info.put("ratings", data.ratings());
		info.put("methods", definitions);
		info.put("betaGrid", beta.grid());
		info.put("betaValidationNdcg", beta.validationNdcg());
		info.put("betaChosen", beta.chosen());
		info.put("mmrLambda", MethodRegistry.MMR_LAMBDA);
		info.put("mmrPool", Evaluator.MMR_POOL);
		info.put("bootstrapResamples", BOOTSTRAP_RESAMPLES);
		info.put("gitCommit", gitCommit());
		info.put("timestamp", Instant.now().toString());
		info.put("runtimeMs", runtimes);
		info.put("totalRuntimeMs", totalMs);
		return info;
	}

	/** {@code git describe --always --dirty}, or "unknown" outside a git checkout. */
	private static String gitCommit() {
		try {
			Process git = new ProcessBuilder("git", "describe", "--always", "--dirty").redirectErrorStream(true).start();
			String output = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
			return git.waitFor() == 0 ? output : "unknown";
		}
		catch (IOException ex) {
			return "unknown";
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return "unknown";
		}
	}
}
