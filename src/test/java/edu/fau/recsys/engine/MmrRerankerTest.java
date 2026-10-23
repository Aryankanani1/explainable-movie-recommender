package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static edu.fau.recsys.engine.LectureFixture.BATMAN_BEGINS;
import static edu.fau.recsys.engine.LectureFixture.HITCHHIKERS;
import static edu.fau.recsys.engine.LectureFixture.LADY_BIRD;
import static edu.fau.recsys.engine.LectureFixture.SPIDER_MAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Golden value G6 (ALGORITHMS.md §10): MMR over the G5 pool, k = 3. */
class MmrRerankerTest {

	private static final double TOLERANCE = 1e-4;

	private final CatalogSnapshot catalog = LectureFixture.catalog();

	private final MmrReranker mmr = new MmrReranker();

	/** The G5 ranking: Spider-Man, Hitchhiker's, Lady Bird, Batman Begins. */
	private final List<Recommendation> g5Pool = new ContentBasedRecommender(new ScoringConfig(true, true, 0, 0))
		.recommend(catalog, LectureFixture.defaultGrades(), RecommendOptions.defaults())
		.items();

	@Test
	void relevanceIsMinMaxNormalizedWithinThePool() {
		assertThat(MmrReranker.relevance(g5Pool)).containsExactly(new double[] { 1.0, 0.8179, 0.1671, 0.0 },
				within(TOLERANCE));
	}

	@Test
	void similarityIsTheIdfWeightedNormalizedGenreCosine() {
		assertThat(MmrReranker.similarity(catalog, HITCHHIKERS, SPIDER_MAN)).isCloseTo(0.2974, within(TOLERANCE));
		assertThat(MmrReranker.similarity(catalog, BATMAN_BEGINS, SPIDER_MAN)).isCloseTo(0.5153, within(TOLERANCE));
		assertThat(MmrReranker.similarity(catalog, LADY_BIRD, SPIDER_MAN)).isZero();
		assertThat(MmrReranker.similarity(catalog, LADY_BIRD, HITCHHIKERS)).isZero();
	}

	@Test
	void lambdaOneKeepsTheRelevanceOrder() {
		assertThat(mmr.rerank(catalog, g5Pool, 3, 1.0)).extracting(Recommendation::movieId)
			.containsExactly(SPIDER_MAN, HITCHHIKERS, LADY_BIRD);
	}

	@Test
	void lambdaPointThreePullsLadyBirdOutOfTheFilterBubble() {
		List<MmrReranker.Pick> picks = mmr.picks(catalog, g5Pool, 3, 0.3);

		assertThat(picks).extracting(pick -> pick.item().movieId()).containsExactly(SPIDER_MAN, LADY_BIRD, HITCHHIKERS);
		assertThat(picks.get(0).mmr()).isCloseTo(0.3000, within(TOLERANCE));
		assertThat(picks.get(1).mmr()).isCloseTo(0.0501, within(TOLERANCE));
		assertThat(picks.get(2).mmr()).isCloseTo(0.0372, within(TOLERANCE));
	}

	@Test
	void reportedScoreStaysTheRelevanceScoreAndRankIsTheMmrPosition() {
		List<Recommendation> reranked = mmr.rerank(catalog, g5Pool, 3, 0.3);

		Recommendation ladyBird = reranked.get(1);
		assertThat(ladyBird.rank()).isEqualTo(2);
		assertThat(ladyBird.score()).isZero();
		assertThat(reranked.get(2).score()).isCloseTo(0.3201, within(TOLERANCE));
	}

	@Test
	void lambdaOneReturnsAnyPoolInItsInputOrder() {
		Random random = new Random(42);
		for (int trial = 0; trial < 50; trial++) {
			CatalogSnapshot randomCatalog = randomCatalog(random, 40);
			List<Recommendation> pool = sortedPool(randomCatalog, random);
			int k = 1 + random.nextInt(pool.size());

			List<Recommendation> reranked = mmr.rerank(randomCatalog, pool, k, 1.0);

			assertThat(reranked).extracting(Recommendation::movieId)
				.as("trial %d", trial)
				.containsExactlyElementsOf(pool.subList(0, k).stream().map(Recommendation::movieId).toList());
		}
	}

	@Test
	void diversifyWithLambdaOneIsThePlainRecommendation() {
		Recommender improved = new ContentBasedRecommender(ScoringConfig.improved(0));
		RecommendOptions options = new RecommendOptions(3, 0, 1.0, true);

		assertThat(mmr.diversify(improved, catalog, LectureFixture.defaultGrades(), options, 100))
			.isEqualTo(improved.recommend(catalog, LectureFixture.defaultGrades(), options));
	}

	@Test
	void diversifyRerankesAPoolLargerThanK() {
		Recommender improved = new ContentBasedRecommender(new ScoringConfig(true, true, 0, 0));

		RecommendationResult result = mmr.diversify(improved, catalog, LectureFixture.defaultGrades(),
				new RecommendOptions(3, 0, 0.3, true), 100);

		assertThat(result.method()).isEqualTo("content-based");
		assertThat(result.items()).extracting(Recommendation::movieId).containsExactly(SPIDER_MAN, LADY_BIRD, HITCHHIKERS);
		assertThat(result.items()).extracting(Recommendation::rank).containsExactly(1, 2, 3);
	}

	/** Movies with random genres (a few with none) and rating counts, some of them equal to force ties. */
	private static CatalogSnapshot randomCatalog(Random random, int size) {
		List<String> genres = List.of("A", "B", "C", "D", "E", "F");
		CatalogSnapshot.Builder builder = CatalogSnapshot.builder().genres(genres);
		for (int id = 1; id <= size; id++) {
			List<String> movieGenres = new ArrayList<>();
			for (String genre : genres) {
				if (random.nextInt(3) == 0) {
					movieGenres.add(genre);
				}
			}
			builder.addMovie(id, "Movie " + id, null, movieGenres, Map.of(), random.nextInt(5));
		}
		return builder.build();
	}

	/** Random scores, some repeated, sorted by the §1 order like any recommender's output. */
	private static List<Recommendation> sortedPool(CatalogSnapshot catalog, Random random) {
		List<Ranking.Scored> scored = new ArrayList<>();
		Map<Integer, Double> scores = new HashMap<>();
		for (int i = 0; i < catalog.size(); i++) {
			double score = random.nextInt(8) / 4.0 - 0.5;
			scores.put(catalog.movieId(i), score);
			scored.add(new Ranking.Scored(i, catalog.movieId(i), catalog.ratingCount(i), score));
		}
		scored.sort(Ranking.ORDER);
		List<Recommendation> pool = new ArrayList<>();
		for (int n = 0; n < scored.size(); n++) {
			pool.add(new Recommendation(scored.get(n).movieId(), n + 1, scored.get(n).score(), null));
		}
		return pool;
	}
}
