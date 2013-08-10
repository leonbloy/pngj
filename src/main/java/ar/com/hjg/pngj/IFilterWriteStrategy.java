package ar.com.hjg.pngj;

import java.util.List;

/**
 * Abstracts the strategy usdd by {@link PngWriter} to choose the PNG predictor
 * filter for each line.
 * <p>
 * Only {@link PngWriter} should normally call these methods.
 */
public interface IFilterWriteStrategy {

	/**
	 * Returns which filter types should be tested (tried) for this row. Empty
	 * if we should not test.
	 */
	public abstract List<FilterType> shouldTest(int rown);

	/**
	 * Reports the filtered row, so that the strategy can compute statistics.
	 * 
	 * @param rown
	 *            Row number
	 * @param type
	 *            To which (real) filter this result corresponds
	 * @param rowbfilter
	 *            Filtered row (first byte is filter type)
	 * @param tentative
	 *            If true, this signals that this is a test/try; if false, we
	 *            confirm that this row is to be written
	 */
	public void reportResultsForFilter(int rown, FilterType type, byte[] rowbfilter, boolean tentative);

	/**
	 * Returns which (actual) filter is recommended to use for this row.
	 * 
	 * @param rown
	 * @return One {@link FilterType}
	 */
	public FilterType preferedType(int rown);

}
