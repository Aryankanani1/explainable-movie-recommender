package edu.fau.recsys.dataset;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses MovieLens titles such as {@code City of Lost Children, The (Cité des enfants perdus, La) (1995)}.
 * <ol>
 *   <li>Trim, then take a trailing {@code (YYYY)} as the year.</li>
 *   <li>Set aside trailing parentheticals (alternate titles, a.k.a. names).</li>
 *   <li>Move a trailing English article back to the front: {@code Matrix, The} → {@code The Matrix}.</li>
 * </ol>
 */
final class TitleParser {

	private static final Pattern YEAR = Pattern.compile("\\((\\d{4})\\)\\s*$");

	/** The name, then one or more trailing "(...)" groups without nested parentheses. */
	private static final Pattern TRAILING_PARENTHETICALS = Pattern.compile("^(.+?)((?:\\s*\\([^()]*\\))+)$");

	/** Only English articles move; "Femme Nikita, La" stays as it is. */
	private static final Pattern MOVED_ARTICLE = Pattern.compile("^(.+), (The|A|An)$");

	private TitleParser() {
	}

	static ParsedTitle parse(String raw) {
		String title = raw.strip();

		Integer year = null;
		Matcher yearMatcher = YEAR.matcher(title);
		if (yearMatcher.find()) {
			year = Integer.valueOf(yearMatcher.group(1));
			title = title.substring(0, yearMatcher.start()).strip();
		}

		String name = title;
		String suffix = "";
		Matcher parentheticals = TRAILING_PARENTHETICALS.matcher(title);
		if (parentheticals.matches()) {
			name = parentheticals.group(1);
			suffix = parentheticals.group(2);
		}

		Matcher article = MOVED_ARTICLE.matcher(name);
		if (article.matches()) {
			name = article.group(2) + " " + article.group(1);
		}
		return new ParsedTitle(name + suffix, year);
	}
}
