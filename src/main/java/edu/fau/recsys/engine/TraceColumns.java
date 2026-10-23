package edu.fau.recsys.engine;

import java.util.ArrayList;
import java.util.List;

/** Picks the genre columns a {@link MathTrace} shows (ALGORITHMS.md §8) and cuts vectors down to them. */
final class TraceColumns {

	private TraceColumns() {
	}

	/** Genres already marked in {@code shown}, plus every genre of a shown candidate, in catalog order. */
	static int[] select(CatalogSnapshot catalog, boolean[] shown, List<Ranking.Scored> candidates) {
		double[][] x = catalog.genreRows();
		int G = shown.length;
		boolean[] keep = shown.clone();
		for (Ranking.Scored candidate : candidates) {
			for (int g = 0; g < G; g++) {
				keep[g] = keep[g] || x[candidate.idx()][g] != 0;
			}
		}
		int count = 0;
		for (int g = 0; g < G; g++) {
			if (keep[g]) {
				count++;
			}
		}
		int[] columns = new int[count];
		int j = 0;
		for (int g = 0; g < G; g++) {
			if (keep[g]) {
				columns[j++] = g;
			}
		}
		return columns;
	}

	static List<String> names(CatalogSnapshot catalog, int[] columns) {
		List<String> names = new ArrayList<>();
		for (int g : columns) {
			names.add(catalog.genreNames().get(g));
		}
		return names;
	}

	static double[] pick(double[] full, int[] columns) {
		double[] picked = new double[columns.length];
		for (int j = 0; j < columns.length; j++) {
			picked[j] = full[columns[j]];
		}
		return picked;
	}
}
