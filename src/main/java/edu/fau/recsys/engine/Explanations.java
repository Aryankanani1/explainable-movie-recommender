package edu.fau.recsys.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Turns raw contributions and influences into an {@link Explanation} (ALGORITHMS.md §7). Shared by all methods. */
final class Explanations {

	static final int MAX_FEATURES = 3;

	static final int MAX_RATED = 2;

	private Explanations() {
	}

	/**
	 * Keeps the positive entries, largest first. The sorts are stable, so equal values keep their input order
	 * (catalog order for features, movieId order for graded movies).
	 */
	static Explanation of(List<FeatureContribution> contributions, List<RatedInfluence> influences, RatingScale scale) {
		List<FeatureContribution> topFeatures = new ArrayList<>();
		for (FeatureContribution contribution : contributions) {
			if (contribution.contribution() > 0) {
				topFeatures.add(contribution);
			}
		}
		topFeatures.sort(Comparator.comparingDouble(FeatureContribution::contribution).reversed());

		List<RatedInfluence> becauseYouRated = new ArrayList<>();
		for (RatedInfluence influence : influences) {
			if (influence.influence() > 0) {
				becauseYouRated.add(influence);
			}
		}
		becauseYouRated.sort(Comparator.comparingDouble(RatedInfluence::influence).reversed());

		topFeatures = limit(topFeatures, MAX_FEATURES);
		becauseYouRated = limit(becauseYouRated, MAX_RATED);
		return new Explanation(topFeatures, becauseYouRated, sentence(topFeatures, becauseYouRated, scale));
	}

	/** e.g. {@code Matches your taste for Comedy and "marvel"; you rated Guardians of the Galaxy 10/10.} */
	static String sentence(List<FeatureContribution> topFeatures, List<RatedInfluence> becauseYouRated,
			RatingScale scale) {
		if (topFeatures.isEmpty() && becauseYouRated.isEmpty()) {
			return "No overlap with what you graded.";
		}
		if (becauseYouRated.isEmpty()) {
			return "Matches your taste for " + featureList(topFeatures) + ".";
		}
		RatedInfluence strongest = becauseYouRated.get(0);
		String grade = number(strongest.grade()) + "/" + number(scale.max());
		if (topFeatures.isEmpty()) {
			return "Similar to " + strongest.title() + ", which you rated " + grade + ".";
		}
		return "Matches your taste for " + featureList(topFeatures) + "; you rated " + strongest.title() + " " + grade
				+ ".";
	}

	/** "A", "A and B", "A, B and C". Tags are quoted so they read differently from genres. */
	private static String featureList(List<FeatureContribution> features) {
		StringBuilder list = new StringBuilder();
		for (int n = 0; n < features.size(); n++) {
			if (n > 0) {
				list.append(n == features.size() - 1 ? " and " : ", ");
			}
			FeatureContribution feature = features.get(n);
			list.append(feature.kind().equals("tag") ? "\"" + feature.feature() + "\"" : feature.feature());
		}
		return list.toString();
	}

	/** 10.0 → "10", 4.5 → "4.5". */
	static String number(double value) {
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}

	private static <T> List<T> limit(List<T> list, int max) {
		return list.size() > max ? List.copyOf(list.subList(0, max)) : List.copyOf(list);
	}
}
