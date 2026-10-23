package edu.fau.recsys.dataset;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TitleParserTest {

	@ParameterizedTest(name = "{0} → {1} ({2})")
	@CsvSource(delimiter = '|', quoteCharacter = '"', textBlock = """
			Toy Story (1995)                                                    | Toy Story                                            | 1995
			Matrix, The (1999)                                                  | The Matrix                                           | 1999
			Shawshank Redemption, The (1994)                                    | The Shawshank Redemption                             | 1994
			City of Lost Children, The (Cité des enfants perdus, La) (1995)     | The City of Lost Children (Cité des enfants perdus, La) | 1995
			Babylon 5                                                           | Babylon 5                                            |
			"   Toy Story (1995)   "                                            | Toy Story                                            | 1995
			"From Dusk Till Dawn 2: Texas Blood Money (1999) "                  | From Dusk Till Dawn 2: Texas Blood Money             | 1999
			Thousand Acres, A (1997)                                            | A Thousand Acres                                     | 1997
			Postman, The (Postino, Il) (1994)                                   | The Postman (Postino, Il)                            | 1994
			Good, the Bad and the Ugly, The (Buono, il brutto, il cattivo, Il) (1966) | The Good, the Bad and the Ugly (Buono, il brutto, il cattivo, Il) | 1966
			Femme Nikita, La (Nikita) (1990)                                    | Femme Nikita, La (Nikita)                            | 1990
			Death Note: Desu nôto (2006–2007)                                   | Death Note: Desu nôto (2006–2007)                    |
			""")
	void parsesDisplayTitleAndYear(String raw, String displayTitle, Integer year) {
		ParsedTitle parsed = TitleParser.parse(raw);

		assertThat(parsed.displayTitle()).isEqualTo(displayTitle);
		assertThat(parsed.year()).isEqualTo(year);
	}
}
