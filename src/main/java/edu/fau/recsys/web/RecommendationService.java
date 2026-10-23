package edu.fau.recsys.web;

import java.util.List;
import java.util.Locale;

import edu.fau.recsys.catalog.CatalogSnapshotHolder;
import edu.fau.recsys.engine.CatalogSnapshot;
import edu.fau.recsys.engine.ContentBasedRecommender;
import edu.fau.recsys.engine.Fallback;
import edu.fau.recsys.engine.LectureRecommender;
import edu.fau.recsys.engine.MathTrace;
import edu.fau.recsys.engine.MmrReranker;
import edu.fau.recsys.engine.PopularityRecommender;
import edu.fau.recsys.engine.RatingScale;
import edu.fau.recsys.engine.RecommendOptions;
import edu.fau.recsys.engine.RecommendationResult;
import edu.fau.recsys.engine.Recommender;
import edu.fau.recsys.engine.ScoringConfig;
import edu.fau.recsys.engine.UserRatings;
import edu.fau.recsys.grading.CurrentUserProvider;
import edu.fau.recsys.grading.GradeService;
import edu.fau.recsys.infra.RecsysProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/** Turns the current user's grades into {@link UserRatings}, runs the chosen engine method, enriches the result. */
@Service
class RecommendationService {

	static final List<String> SUPPORTED_METHODS = List.of("lecture", "improved");

	private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

	private final CatalogSnapshotHolder catalogHolder;

	private final GradeService gradeService;

	private final CurrentUserProvider currentUserProvider;

	private final LectureRecommender lecture = new LectureRecommender();

	private final ContentBasedRecommender improved;

	private final PopularityRecommender popularity = new PopularityRecommender();

	private final MmrReranker mmr = new MmrReranker();

	private final int defaultK;

	private final int mmrPool;

	private final int minGrades;

	RecommendationService(CatalogSnapshotHolder catalogHolder, GradeService gradeService,
			CurrentUserProvider currentUserProvider, RecsysProperties properties) {
		this.catalogHolder = catalogHolder;
		this.gradeService = gradeService;
		this.currentUserProvider = currentUserProvider;
		this.improved = new ContentBasedRecommender(ScoringConfig.improved(properties.recommend().tagWeight()));
		this.defaultK = properties.recommend().defaultK();
		this.mmrPool = properties.recommend().mmrPool();
		this.minGrades = properties.recommend().minGrades();
	}

	/**
	 * {@code k == null} means the configured {@code recsys.recommend.default-k}; {@code lambda < 1} re-ranks the top
	 * {@code mmr-pool} for variety (MMR). With fewer than {@code min-grades} grades the list is popular films,
	 * flagged {@code POPULARITY}, so the UI can ask for more grades.
	 */
	RecommendationResultDto recommend(String method, Integer k, int minRatings, double lambda) {
		Recommender recommender = recommender(method);
		int listLength = k != null ? k : defaultK;
		CatalogSnapshot catalog = catalogHolder.get();
		UserRatings user = currentUserRatings();
		RecommendOptions options = new RecommendOptions(listLength, minRatings, lambda, true);

		long start = System.nanoTime();
		RecommendationResult result;
		if (user.byMovieId().size() < minGrades) {
			RecommendationResult popular = mmr.diversify(popularity, catalog, user, options, mmrPool);
			result = new RecommendationResult(recommender.id(), popular.items(), Fallback.POPULARITY);
		}
		else {
			result = mmr.diversify(recommender, catalog, user, options, mmrPool);
		}
		double elapsedMs = (System.nanoTime() - start) / 1e6;
		log.info("Recommended method={} k={} lambda={} grades={} fallback={} in {} ms", method, listLength, lambda,
				user.byMovieId().size(), result.fallback(), String.format(Locale.ROOT, "%.1f", elapsedMs));

		return RecommendationResultDto.from(result, catalog);
	}

	MathTrace math(String method, int k) {
		Recommender recommender = recommender(method);
		CatalogSnapshot catalog = catalogHolder.get();
		UserRatings user = currentUserRatings();
		return recommender == lecture ? lecture.trace(catalog, user, k) : improved.trace(catalog, user, k);
	}

	private UserRatings currentUserRatings() {
		long userId = currentUserProvider.currentUser().id();
		return new UserRatings(gradeService.scores(userId), RatingScale.APP_GRADES);
	}

	private Recommender recommender(String method) {
		return switch (method) {
			case "lecture" -> lecture;
			case "improved" -> improved;
			default -> throw new UnknownMethodException(method);
		};
	}
}
