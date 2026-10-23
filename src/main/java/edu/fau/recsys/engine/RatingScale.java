package edu.fau.recsys.engine;

/** The range a user's ratings come from. The engine never assumes a scale; each {@link UserRatings} carries its own. */
public record RatingScale(double min, double max) {

	/** App grades, as in the lecture. */
	public static final RatingScale APP_GRADES = new RatingScale(1, 10);

	/** MovieLens stars, in half-star steps. */
	public static final RatingScale MOVIELENS_STARS = new RatingScale(0.5, 5.0);

	public RatingScale {
		if (!(min < max)) {
			throw new IllegalArgumentException("min must be below max: " + min + " / " + max);
		}
	}

	public double midpoint() {
		return (min + max) / 2;
	}

	public boolean contains(double rating) {
		return rating >= min && rating <= max;
	}
}
