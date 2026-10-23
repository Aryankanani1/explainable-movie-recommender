package edu.fau.recsys.engine;

/**
 * Item vectors v_i = [ĝ_i ; β·t̂_i] (ALGORITHMS.md §3) for one configuration. The genre block (≈20 columns) is
 * dense; the tag block (hundreds of columns, and empty for most movies) is sparse. Column order: genres in catalog
 * order, then the tag vocabulary. Built by {@link CatalogSnapshot#itemVectors}; read-only afterwards.
 */
final class ItemVectors {

	private final int G;

	private final int T;

	/** ĝ_i, N × G. */
	private final double[][] genre;

	/** Per movie: vocabulary indices of its tags, ascending, and the matching values of β·t̂_i. */
	private final int[][] tagIdx;

	private final double[][] tagVal;

	/** ‖v_i‖, precomputed. */
	private final double[] norm;

	ItemVectors(int G, int T, double[][] genre, int[][] tagIdx, double[][] tagVal) {
		this.G = G;
		this.T = T;
		this.genre = genre;
		this.tagIdx = tagIdx;
		this.tagVal = tagVal;
		this.norm = new double[genre.length];
		for (int i = 0; i < genre.length; i++) {
			double sumOfSquares = 0;
			for (int g = 0; g < G; g++) {
				sumOfSquares += genre[i][g] * genre[i][g];
			}
			for (int k = 0; k < tagVal[i].length; k++) {
				sumOfSquares += tagVal[i][k] * tagVal[i][k];
			}
			norm[i] = Math.sqrt(sumOfSquares);
		}
	}

	/** G + T; just G when β = 0. */
	int dimensions() {
		return G + T;
	}

	int genreDimensions() {
		return G;
	}

	double norm(int i) {
		return norm[i];
	}

	/** ĝ_i. Shared, not copied. */
	double[] genreRow(int i) {
		return genre[i];
	}

	int[] tagIndices(int i) {
		return tagIdx[i];
	}

	double[] tagValues(int i) {
		return tagVal[i];
	}

	/** p += w · v_i. */
	void addTo(double[] p, int i, double w) {
		for (int g = 0; g < G; g++) {
			p[g] += w * genre[i][g];
		}
		for (int k = 0; k < tagIdx[i].length; k++) {
			p[G + tagIdx[i][k]] += w * tagVal[i][k];
		}
	}

	/** p · v_c. */
	double dot(double[] p, int c) {
		double sum = 0;
		for (int g = 0; g < G; g++) {
			sum += p[g] * genre[c][g];
		}
		for (int k = 0; k < tagIdx[c].length; k++) {
			sum += p[G + tagIdx[c][k]] * tagVal[c][k];
		}
		return sum;
	}

	/** cos(v_i, v_c), or 0 when either vector is zero. Tag lists are sorted, so their dot product is a merge. */
	double cosine(int i, int c) {
		if (norm[i] == 0 || norm[c] == 0) {
			return 0;
		}
		double sum = 0;
		for (int g = 0; g < G; g++) {
			sum += genre[i][g] * genre[c][g];
		}
		int a = 0;
		int b = 0;
		while (a < tagIdx[i].length && b < tagIdx[c].length) {
			if (tagIdx[i][a] == tagIdx[c][b]) {
				sum += tagVal[i][a] * tagVal[c][b];
				a++;
				b++;
			}
			else if (tagIdx[i][a] < tagIdx[c][b]) {
				a++;
			}
			else {
				b++;
			}
		}
		return sum / (norm[i] * norm[c]);
	}

	/** v_i as one dense array of length G + T (for tests and traces). */
	double[] dense(int i) {
		double[] v = new double[G + T];
		System.arraycopy(genre[i], 0, v, 0, G);
		for (int k = 0; k < tagIdx[i].length; k++) {
			v[G + tagIdx[i][k]] = tagVal[i][k];
		}
		return v;
	}
}
