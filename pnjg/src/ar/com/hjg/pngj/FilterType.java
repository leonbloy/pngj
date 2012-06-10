package ar.com.hjg.pngj;

/**
 * Internal PNG predictor filter, or strategy to select it.
 * 
 */
public enum FilterType {
	/**
	 * No filter.
	 */
	FILTER_NONE(0),
	/**
	 * SUB filter (uses same row)
	 */
	FILTER_SUB(1),
	/**
	 * UP filter (uses previous row)
	 */
	FILTER_UP(2),
	/**
	 * AVERAGE filter
	 */
	FILTER_AVERAGE(3),
	/**
	 * PAETH predictor
	 */
	FILTER_PAETH(4),
	/**
	 * Default strategy: select one of the above filters depending on global image parameters
	 */
	FILTER_DEFAULT(-1),
	/**
	 * Aggressive strategy: select one of the above filters trying each of the filters (this is done every 8 rows)
	 */
	FILTER_AGGRESSIVE(-2),
	/**
	 * Uses all fiters, one for lines, cyciclally. Only for tests.
	 */
	FILTER_ALTERNATE(-3),
	/**
	 * Aggressive strategy: select one of the above filters trying each of the filters (this is done for every row!)
	 */
	FILTER_VERYAGGRESSIVE(-4), ;
	public final int val;

	private FilterType(int val) {
		this.val = val;
	}

	public static FilterType getByVal(int i) {
		for (FilterType ft : values()) {
			if (ft.val == i)
				return ft;
		}
		return null;
	}


}
