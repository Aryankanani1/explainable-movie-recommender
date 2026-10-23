package edu.fau.recsys.evaluation;

import java.io.IOException;
import java.nio.file.Path;

import edu.fau.recsys.dataset.MovieLensFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Regenerates {@code report-assets/} from the MovieLens CSVs and {@code results/} (run {@link EvaluationRunner}
 * first). Plain Java: no database, no Spring.
 * <p>
 * {@code ./mvnw -q compile exec:java -Dexec.mainClass=edu.fau.recsys.evaluation.ReportAssetsRunner}. Optional
 * arguments: data directory, results directory, output directory.
 */
public final class ReportAssetsRunner {

	private static final Logger log = LoggerFactory.getLogger(ReportAssetsRunner.class);

	private ReportAssetsRunner() {
	}

	public static void main(String[] args) throws IOException {
		Path dataDir = Path.of(args.length > 0 ? args[0] : "data/ml-latest-small");
		Path resultsDir = Path.of(args.length > 1 ? args[1] : "results");
		Path outDir = Path.of(args.length > 2 ? args[2] : "report-assets");
		long start = System.nanoTime();
		EvaluationData data = EvaluationData.load(new MovieLensFiles(dataDir));
		new ReportAssets(data, Personas.ALL).writeAll(outDir, resultsDir, true);
		log.info("Wrote {} in {} s", outDir.toAbsolutePath(), (System.nanoTime() - start) / 1_000_000_000);
	}
}
