package edu.fau.recsys.evaluation;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes {@code metrics.csv}, {@code metrics.md} and {@code run-info.json}. The two metrics files hold only
 * deterministic values, so a second run is byte-identical; timings and the timestamp go to run-info.json.
 */
final class ResultsWriter {

	private ResultsWriter() {
	}

	static void write(Path dir, String csv, String markdown, String runInfoJson) throws IOException {
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("metrics.csv"), csv, StandardCharsets.UTF_8);
		Files.writeString(dir.resolve("metrics.md"), markdown, StandardCharsets.UTF_8);
		Files.writeString(dir.resolve("run-info.json"), runInfoJson, StandardCharsets.UTF_8);
	}

	static String csv(List<MethodResult> results, int k) {
		StringBuilder csv = new StringBuilder();
		csv.append(String.format(Locale.ROOT,
				"method,usersEvaluated,usersSkipped,P@%d,R@%d,NDCG@%d,HR@%d,coverage,ILD,novelty%n", k, k, k, k));
		for (MethodResult r : results) {
			csv.append(String.format(Locale.ROOT, "%s,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n", r.method(),
					r.usersEvaluated(), r.usersSkipped(), r.precision(), r.recall(), r.ndcg(), r.hitRate(), r.coverage(),
					r.ild(), r.novelty()));
		}
		return csv.toString();
	}

	static String markdown(String dataLine, List<MethodResult> results, int k, BetaSearch beta,
			Map<String, Bootstrap.Interval> comparisons) {
		StringBuilder md = new StringBuilder();
		md.append("# Offline evaluation\n\n").append(dataLine).append("\n\n");
		md.append(String.format(Locale.ROOT,
				"| Method | Users | Skipped | P@%d | R@%d | NDCG@%d | HR@%d | Coverage | ILD | Novelty |%n", k, k, k, k));
		md.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
		for (MethodResult r : results) {
			md.append(String.format(Locale.ROOT, "| %s | %d | %d | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.2f |%n",
					r.method(), r.usersEvaluated(), r.usersSkipped(), r.precision(), r.recall(), r.ndcg(), r.hitRate(),
					r.coverage(), r.ild(), r.novelty()));
		}

		md.append(String.format(Locale.ROOT,
				"%n## β search for cb-tags (validation split, %d users)%n%n| β | NDCG@%d on validation |%n|---:|---:|%n",
				beta.validationUsers(), k));
		for (int g = 0; g < beta.grid().length; g++) {
			String marker = beta.grid()[g] == beta.chosen() ? " ← chosen" : "";
			md.append(String.format(Locale.ROOT, "| %s | %.4f%s |%n", number(beta.grid()[g]), beta.validationNdcg()[g],
					marker));
		}
		md.append(String.format(Locale.ROOT,
				"%ncb-tags-tuned uses β = %s, chosen on validation, then evaluated once on test.%n",
				number(beta.chosen())));

		md.append(String.format(Locale.ROOT,
				"%n## Paired bootstrap, NDCG@%d difference (1000 resamples of users, seed 42)%n%n"
						+ "| Comparison | Mean difference | 95%% CI | Excludes 0? |%n|---|---:|---|---|%n",
				k));
		for (Map.Entry<String, Bootstrap.Interval> comparison : comparisons.entrySet()) {
			Bootstrap.Interval ci = comparison.getValue();
			md.append(String.format(Locale.ROOT, "| %s | %+.4f | [%+.4f, %+.4f] | %s |%n", comparison.getKey(),
					ci.mean(), ci.low(), ci.high(), ci.excludesZero() ? "yes" : "no"));
		}
		return md.toString();
	}

	/** Minimal JSON for flat run metadata: strings, numbers, booleans, maps and lists. */
	static String json(Object value) {
		StringBuilder json = new StringBuilder();
		appendJson(json, value, 0);
		return json.append('\n').toString();
	}

	private static void appendJson(StringBuilder json, Object value, int indent) {
		if (value == null) {
			json.append("null");
		}
		else if (value instanceof String text) {
			json.append('"').append(text.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
		}
		else if (value instanceof Number || value instanceof Boolean) {
			json.append(value);
		}
		else if (value instanceof double[] numbers) {
			json.append('[');
			for (int n = 0; n < numbers.length; n++) {
				json.append(n == 0 ? "" : ", ").append(numbers[n]);
			}
			json.append(']');
		}
		else if (value instanceof Map<?, ?> map) {
			json.append("{\n");
			int n = 0;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				json.append("  ".repeat(indent + 1));
				appendJson(json, String.valueOf(entry.getKey()), indent + 1);
				json.append(": ");
				appendJson(json, entry.getValue(), indent + 1);
				json.append(++n < map.size() ? ",\n" : "\n");
			}
			json.append("  ".repeat(indent)).append('}');
		}
		else if (value instanceof List<?> list) {
			json.append('[');
			for (int n = 0; n < list.size(); n++) {
				json.append(n == 0 ? "" : ", ");
				appendJson(json, list.get(n), indent);
			}
			json.append(']');
		}
		else {
			throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
		}
	}

	/** 0.5 → "0.5", 1.0 → "1". */
	private static String number(double value) {
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}
}
