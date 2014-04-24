package ar.com.hjg.pngj;

/**
 * Utility class that wraps two objects in one
 */
public class Pair<TA, TB> {
	public final TA a;
	public final TB b;

	/**
	 * factory method
	 */
	public static <TA, TB> Pair<TA, TB> createPair(TA a, TB b) {
		return new Pair<TA, TB>(a, b);
	}

	/**
	 * private constructor - use instead factory method
	 */
	private Pair(final TA a, final TB b) {
		this.a = a;
		this.b = b;
	}

	public String toString() {
		return "(" + a + ", " + b + ")";
	}

}