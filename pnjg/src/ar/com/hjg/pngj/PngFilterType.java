package ar.com.hjg.pngj;

/** 
 * Internal PNG predictor filter, or strategy to select it.
 *  
 */
public enum PngFilterType {
	/**
	 * No filter.
	 */
	FILTER_NONE(0), 
	/**
	 * SUB filter (uses same row)
	 */
	FILTER_SUB(1), 
	/**
	 * UP filter  (uses previous row)
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
	 * Aggresive strategy: select one of the above filters trying each of the filters 
	 * (this is done every 8 rows) 
	 */
	FILTER_AGRESSIVE(-2), 
	/**
	 * Uses all fiters, one for lines, cyciclally. Only for tests. 
	 */
	FILTER_ALTERNATE(-3)
	;
	public final int val;

	private PngFilterType(int val) {
		this.val = val;
	}

	public static PngFilterType getByVal(int i) {
		for (PngFilterType ft : values()) {
			if (ft.val == i)
				return ft;
		}
		return null;
	}
}
