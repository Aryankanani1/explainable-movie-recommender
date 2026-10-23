package edu.fau.recsys.web;

import java.util.List;
import java.util.Locale;

import edu.fau.recsys.catalog.CatalogSnapshotHolder;
import edu.fau.recsys.engine.CatalogSnapshot;
import edu.fau.recsys.engine.LectureRecommender;
import edu.fau.recsys.engine.MathTrace;
import edu.fau.recsys.engine.RatingScale;
import edu.fau.recsys.engine.RecommendOptions;
import edu.fau.recsys.engine.RecommendationResult;
import edu.fau.recsys.engine.Recommender;
import edu.fau.recsys.engine.UserRatings;
import edu.fau.recsys.grading.CurrentUserProvider;
import edu.fau.recsys.grading.GradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/** Turns the current user's grades into {@link UserRatings}, runs the chosen engine method, enriches the result. */
@Service
class RecommendationService {

	static final List<String> SUPPORTED_METHODS = List.of("lecture");

	private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

	private final CatalogSnapshotHolder catalogHolder;

	private final GradeService gradeService;

	private final CurrentUserProvider currentUserProvider;

	private final LectureRecommender lecture = new LectureRecommender();

	RecommendationService(CatalogSnapshotHolder catalogHolder, GradeService gradeService,
			CurrentUserProvider currentUserProvider) {
		this.catalogHolder = catalogHolder;
		this.gradeService = gradeService;
		this.currentUserProvider = currentUserProvider;
	}

	RecommendationResultDto recommend(String method, int k, int minRatings) {
		Recommender recommender = recommender(method);
		CatalogSnapshot catalog = catalogHolder.get();
		UserRatings user = currentUserRatings();

		long start = System.nanoTime();
		RecommendationResult result = recommender.recommend(catalog, user, new RecommendOptions(k, minRatings, 1.0, true));
		double elapsedMs = (System.nanoTime() - start) / 1e6;
		log.info("Recommended method={} k={} grades={} fallback={} in {} ms", method, k, user.byMovieId().size(),
				result.fallback(), String.format(Locale.ROOT, "%.1f", elapsedMs));

		return RecommendationResultDto.from(result, catalog);
	}

	MathTrace math(String method, int k) {
		recommender(method);
		return lecture.trace(catalogHolder.get(), currentUserRatings(), k);
	}

	private UserRatings currentUserRatings() {
		long userId = currentUserProvider.currentUser().id();
		return new UserRatings(gradeService.scores(userId), RatingScale.APP_GRADES);
	}

	private Recommender recommender(String method) {
		if (method.equals("lecture")) {
			return lecture;
		}
		throw new UnknownMethodException(method);
	}
}
