package edu.fau.recsys.web;

import java.nio.file.Path;
import java.util.Map;

import edu.fau.recsys.catalog.CatalogSnapshotHolder;
import edu.fau.recsys.engine.Fallback;
import edu.fau.recsys.engine.LectureFixture;
import edu.fau.recsys.engine.MathTrace;
import edu.fau.recsys.grading.CurrentUser;
import edu.fau.recsys.grading.GradeService;
import edu.fau.recsys.infra.RecsysProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RecommendationServiceTest {

	private final CatalogSnapshotHolder catalogHolder = mock(CatalogSnapshotHolder.class);

	private final GradeService gradeService = mock(GradeService.class);

	private final RecsysProperties properties = new RecsysProperties(Path.of("data"), new RecsysProperties.Import(true),
			new RecsysProperties.Recommend(2, 0.0));

	private final RecommendationService service = new RecommendationService(catalogHolder, gradeService,
			() -> new CurrentUser(7, "demo"), properties);

	@BeforeEach
	void fixtureCatalog() {
		given(catalogHolder.get()).willReturn(LectureFixture.catalog());
	}

	@Test
	void lectureRecommendationsCarryTitlesGenresAndExplanations() {
		given(gradeService.scores(7)).willReturn(Map.of(1, 2.0, 2, 10.0, 3, 8.0));

		RecommendationResultDto result = service.recommend("lecture", 10, 0);

		assertThat(result.fallback()).isEqualTo(Fallback.NONE);
		assertThat(result.items()).extracting(RecommendationDto::title)
			.containsExactly("The Hitchhiker's Guide to the Galaxy", "Spider-Man", "Batman Begins", "Lady Bird");
		assertThat(result.items().get(1).genres()).containsExactly("Comedy", "Super Hero");
		assertThat(result.items().get(1).year()).isEqualTo(2002);
		assertThat(result.items().get(1).explanation().sentence())
			.isEqualTo("Matches your taste for Super Hero and Comedy; you rated Captain America: Civil War 8/10.");
	}

	@Test
	void noGradesIsFlaggedAsPopularityFallback() {
		given(gradeService.scores(7)).willReturn(Map.of());

		RecommendationResultDto result = service.recommend("lecture", 3, 0);

		assertThat(result.fallback()).isEqualTo(Fallback.POPULARITY);
		assertThat(result.items()).extracting(RecommendationDto::title)
			.containsExactly("Guardians of the Galaxy", "Batman Begins", "Captain America: Civil War");
	}

	@Test
	void mathTraceUsesTheCurrentGrades() {
		given(gradeService.scores(7)).willReturn(Map.of(1, 2.0, 2, 10.0, 3, 8.0));

		MathTrace trace = service.math("lecture", 3);

		assertThat(trace.lecture().profile().T()).isEqualTo(60.0);
		assertThat(trace.columns()).containsExactly("Comedy", "Adventure", "Super Hero", "Sci-Fi");
	}

	@Test
	void improvedIsTheCenteredIdfContentBasedMethod() {
		given(gradeService.scores(7)).willReturn(Map.of(1, 2.0, 2, 10.0, 3, 8.0));

		RecommendationResultDto result = service.recommend("improved", 10, 0);
		MathTrace trace = service.math("improved", 3);

		assertThat(result.method()).isEqualTo("content-based");
		assertThat(result.items()).extracting(RecommendationDto::title)
			.containsExactly("Spider-Man", "The Hitchhiker's Guide to the Galaxy", "Lady Bird", "Batman Begins");
		assertThat(trace.contentBased().config().centerRatings()).isTrue();
		assertThat(trace.contentBased().config().useIdf()).isTrue();
	}

	@Test
	void kDefaultsToTheConfiguredValue() {
		given(gradeService.scores(7)).willReturn(Map.of(1, 2.0, 2, 10.0, 3, 8.0));

		assertThat(service.recommend("lecture", null, 0).items()).hasSize(2);
	}

	@Test
	void unknownMethodIsRejectedBeforeAnyWork() {
		assertThatThrownBy(() -> service.recommend("hybrid", 10, 0)).isInstanceOf(UnknownMethodException.class)
			.hasMessageContaining("Supported: [lecture, improved]");
	}
}
