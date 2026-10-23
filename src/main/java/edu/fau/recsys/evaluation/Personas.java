package edu.fau.recsys.evaluation;

import java.util.List;

/**
 * The example viewers for the report (ticket 10). Titles are exactly as in {@code movies.csv}; grades are on the
 * lecture's 1–10 scale, and every persona has at least two low grades so the improved method has something to
 * steer away from. Edit here and re-run {@link ReportAssetsRunner} to change the examples.
 */
final class Personas {

	static final List<Persona> ALL = List.of(
			new Persona("Marvel fan who dislikes DC", "Loves Marvel super-hero films; gives DC's low grades.",
					List.of(g("Iron Man (2008)", 10), g("Guardians of the Galaxy (2014)", 10), g("Avengers, The (2012)", 9),
							g("Captain America: The Winter Soldier (2014)", 9), g("Captain America: Civil War (2016)", 8),
							g("Thor: Ragnarok (2017)", 8), g("Batman v Superman: Dawn of Justice (2016)", 2),
							g("Suicide Squad (2016)", 2), g("Justice League (2017)", 3), g("Man of Steel (2013)", 4))),
			new Persona("Family and Pixar viewer", "Watches animated family films; avoids horror.",
					List.of(g("Toy Story (1995)", 10), g("Up (2009)", 10), g("Finding Nemo (2003)", 9),
							g("WALL·E (2008)", 9), g("Inside Out (2015)", 9), g("Monsters, Inc. (2001)", 8),
							g("Ratatouille (2007)", 8), g("Saw (2004)", 1), g("Exorcist, The (1973)", 2),
							g("Scream (1996)", 2))),
			new Persona("Crime and noir buff", "Likes crime dramas and film noir; dislikes musicals and romance.",
					List.of(g("Godfather, The (1972)", 10), g("Pulp Fiction (1994)", 10), g("Goodfellas (1990)", 9),
							g("L.A. Confidential (1997)", 9), g("Chinatown (1974)", 9), g("Usual Suspects, The (1995)", 8),
							g("Maltese Falcon, The (1941)", 8), g("Mamma Mia! (2008)", 2), g("Notebook, The (2004)", 2),
							g("Shrek the Third (2007)", 3))),
			new Persona("Romantic-comedy fan", "Loves romantic comedies; dislikes war, horror and action.",
					List.of(g("When Harry Met Sally... (1989)", 10), g("Notting Hill (1999)", 9),
							g("Love Actually (2003)", 9), g("Pretty Woman (1990)", 8),
							g("10 Things I Hate About You (1999)", 8), g("Bridget Jones's Diary (2001)", 8),
							g("Proposal, The (2009)", 7), g("Saving Private Ryan (1998)", 3), g("Alien (1979)", 2),
							g("Terminator, The (1984)", 2))));

	private Personas() {
	}

	private static Grade g(String title, int grade) {
		return new Grade(title, grade);
	}

	/** @param title exactly as in {@code movies.csv}, e.g. {@code Godfather, The (1972)} */
	record Grade(String title, int grade) {
	}

	record Persona(String name, String description, List<Grade> grades) {

		Persona {
			grades = List.copyOf(grades);
		}
	}
}
