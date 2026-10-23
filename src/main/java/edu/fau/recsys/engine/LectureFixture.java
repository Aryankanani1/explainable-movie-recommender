package edu.fau.recsys.engine;

import java.util.List;
import java.util.Map;

/**
 * The 7-movie example from the lecture slides (ALGORITHMS.md §10). Used by the golden tests and by
 * {@code GET /api/demo/lecture-example}.
 */
public final class LectureFixture {

	public static final int BATMAN_V_SUPERMAN = 1;

	public static final int GUARDIANS = 2;

	public static final int CAPTAIN_AMERICA = 3;

	public static final int HITCHHIKERS = 4;

	public static final int BATMAN_BEGINS = 5;

	public static final int SPIDER_MAN = 6;

	public static final int LADY_BIRD = 7;

	public static final RatingScale SCALE = RatingScale.APP_GRADES;

	private LectureFixture() {
	}

	public static CatalogSnapshot catalog() {
		return CatalogSnapshot.builder()
			.genres(List.of("Comedy", "Adventure", "Super Hero", "Sci-Fi", "Drama"))
			.addMovie(BATMAN_V_SUPERMAN, "Batman v Superman", 2016, List.of("Adventure", "Super Hero"),
					Map.of("dc", 2), 300)
			.addMovie(GUARDIANS, "Guardians of the Galaxy", 2014,
					List.of("Comedy", "Adventure", "Super Hero", "Sci-Fi"), Map.of("space", 2, "marvel", 3), 400)
			.addMovie(CAPTAIN_AMERICA, "Captain America: Civil War", 2016, List.of("Comedy", "Super Hero"),
					Map.of("marvel", 2), 350)
			.addMovie(HITCHHIKERS, "The Hitchhiker's Guide to the Galaxy", 2005,
					List.of("Comedy", "Adventure", "Sci-Fi"), Map.of("space", 1, "british", 1), 120)
			.addMovie(BATMAN_BEGINS, "Batman Begins", 2005, List.of("Super Hero"), Map.of("dc", 1), 380)
			.addMovie(SPIDER_MAN, "Spider-Man", 2002, List.of("Comedy", "Super Hero"), Map.of("marvel", 1), 250)
			.addMovie(LADY_BIRD, "Lady Bird", 2017, List.of("Drama"), Map.of(), 80)
			.build();
	}

	/** The slides' grades: Batman v Superman 2, Guardians of the Galaxy 10, Captain America: Civil War 8. */
	public static UserRatings defaultGrades() {
		return new UserRatings(Map.of(BATMAN_V_SUPERMAN, 2.0, GUARDIANS, 10.0, CAPTAIN_AMERICA, 8.0), SCALE);
	}
}
