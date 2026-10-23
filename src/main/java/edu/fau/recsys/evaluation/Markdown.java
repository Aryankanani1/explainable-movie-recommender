package edu.fau.recsys.evaluation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** GitHub-flavoured Markdown tables and number formatting for the report assets. Deterministic: Locale.ROOT. */
final class Markdown {

	private Markdown() {
	}

	/** A table whose first {@code leftColumns} columns are left-aligned and the rest right-aligned (numbers). */
	static String table(List<String> headers, List<List<String>> rows, int leftColumns) {
		return table(headers, rows, "l".repeat(Math.min(leftColumns, headers.size()))
				+ "r".repeat(Math.max(0, headers.size() - leftColumns)));
	}

	/** A table with one alignment letter per column: {@code l} (text) or {@code r} (numbers). */
	static String table(List<String> headers, List<List<String>> rows, String align) {
		StringBuilder md = new StringBuilder();
		md.append(row(headers));
		StringBuilder rule = new StringBuilder("|");
		for (int c = 0; c < headers.size(); c++) {
			rule.append(align.charAt(c) == 'l' ? "---|" : "---:|");
		}
		md.append(rule).append('\n');
		for (List<String> cells : rows) {
			md.append(row(cells));
		}
		return md.toString();
	}

	static String table(List<String> headers, List<List<String>> rows) {
		return table(headers, rows, 1);
	}

	private static String row(List<String> cells) {
		StringBuilder line = new StringBuilder("|");
		for (String cell : cells) {
			line.append(' ').append(cell.replace("|", "\\|")).append(" |");
		}
		return line.append('\n').toString();
	}

	/** Fixed decimals, never "-0.0000". */
	static String fixed(double value, int digits) {
		String text = String.format(Locale.ROOT, "%." + digits + "f", value);
		return text.matches("-0\\.?0*") ? text.substring(1) : text;
	}

	/** 10.0 → "10", 4.5 → "4.5". */
	static String number(double value) {
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}
}
