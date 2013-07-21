package ar.com.hjg.pngj;

import java.util.List;

public interface IFilterWriteStrategy {

	/** 
	 * Returns which filter types should be tested for this row. Empty if we should not test. 
	 */
	public abstract List<FilterType> shouldTest(int rown);

	/**
	 * Returns which (real) filter should be used for this row 
	 * @param rown
	 * @return
	 */
	public FilterType gimmeFilterType(int rown);

	/**
	 * Reports the filtered row, so that the strategy can comput statistics
	 * 
	 * @param rown Row number
	 * @param type To which (real) filter this result corresponds  
	 * @param rowbfilter Filtered row (remember that first byte is filter)
	 * @param tentative If true, this signals that this is a test/try; if false, we confirm that this row is to be written 
	 */
	public void reportResultsForFilter(int rown, FilterType type, byte[] rowbfilter, boolean tentative);

}
