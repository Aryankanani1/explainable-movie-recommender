package edu.fau.recsys.evaluation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import edu.fau.recsys.dataset.MovieLensFiles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ticket 10 acceptance: real titles only, every persona well-formed, and identical output on every run. */
class ReportAssetsTest {

	private static EvaluationData data;

	@BeforeAll
	static void loadMovieLens() {
		data = EvaluationData.load(new MovieLensFiles(Path.of("data/ml-latest-small")));
	}

	@Test
	void everyPersonaHasEightToTenFilmsAndAtLeastTwoLowGrades() {
		for (Personas.Persona persona : Personas.ALL) {
			assertThat(persona.grades()).as(persona.name()).hasSizeBetween(8, 10);
			assertThat(persona.grades()).as(persona.name()).allSatisfy(g -> assertThat(g.grade()).isBetween(1, 10));
			assertThat(persona.grades().stream().filter(g -> g.grade() < 5)).as(persona.name()).hasSizeGreaterThanOrEqualTo(2);
		}
		assertThat(Personas.ALL).hasSize(4);
	}

	@Test
	void anUnknownTitleFailsLoudlyAndNamesIt() {
		Personas.Persona typo = new Personas.Persona("Typo", "",
				List.of(new Personas.Grade("Matrix, The (1999)", 9), new Personas.Grade("The Matrix (1999)", 9)));

		assertThatThrownBy(() -> new ReportAssets(data, List.of(typo))).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Typo: The Matrix (1999)")
			.hasMessageNotContaining("Matrix, The");
	}

	@Test
	void recommendationsNeverIncludeAGradedFilm() {
		String md = new ReportAssets(data, Personas.ALL).recommendationsMd();

		for (Personas.Persona persona : Personas.ALL) {
			for (Personas.Grade grade : persona.grades()) {
				String display = grade.title().replaceFirst("^(.*), (The|A|An) (\\(\\d{4}\\))$", "$2 $1 $3");
				assertThat(md).as("%s must not be recommended", display).doesNotContain("| " + display + " |");
			}
		}
	}

	@Test
	void calculationsReproduceTheSlideExample() {
		String md = new ReportAssets(data, Personas.ALL).calculationsMd();

		assertThat(md).contains("| **P** (column sums) | 18 | 12 | 20 | 10 | 0 |")
			.contains("T = Σ P = 60.")
			.contains("| The Hitchhiker's Guide to the Galaxy | 0.3000 | 0.2000 | 0.0000 | 0.1667 | 0.0000 | 0.6667 | 6.67 |");
	}

	@Test
	void twoRunsWriteByteIdenticalFiles(@TempDir Path first, @TempDir Path second) throws IOException {
		new ReportAssets(data, Personas.ALL).writeAll(first, Path.of("results"), false);
		new ReportAssets(data, Personas.ALL).writeAll(second, Path.of("results"), false);

		try (var files = Files.list(first)) {
			for (Path file : files.sorted().toList()) {
				assertThat(Files.readAllBytes(second.resolve(file.getFileName())))
					.as(file.getFileName().toString())
					.isEqualTo(Files.readAllBytes(file));
			}
		}
		assertThat(first.resolve("personas.md")).exists();
		assertThat(first.resolve("metrics-chart.csv")).content().startsWith("method,NDCG@10,coverage,ILD\nrandom,");
	}
}
