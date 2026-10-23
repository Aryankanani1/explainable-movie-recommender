package edu.fau.recsys.engine;

/** Dense vector helpers. Plain loops on purpose: every line should read like the formula. */
final class Vectors {

	private Vectors() {
	}

	static double dot(double[] a, double[] b) {
		double sum = 0;
		for (int n = 0; n < a.length; n++) {
			sum += a[n] * b[n];
		}
		return sum;
	}

	static double norm(double[] a) {
		return Math.sqrt(dot(a, a));
	}

	/** cos(a, b) = a·b / (‖a‖‖b‖), or 0 when either vector is zero. */
	static double cosine(double[] a, double[] b) {
		double normA = norm(a);
		double normB = norm(b);
		if (normA == 0 || normB == 0) {
			return 0;
		}
		return dot(a, b) / (normA * normB);
	}
}
