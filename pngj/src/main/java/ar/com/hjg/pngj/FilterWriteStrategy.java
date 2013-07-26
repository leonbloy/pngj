package ar.com.hjg.pngj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Manages the writer strategy for selecting the internal png predictor filter
 * 
 * TODO: this needs work
 */
public class FilterWriteStrategy implements IFilterWriteStrategy {

	final ImageInfo imgInfo;
	private  FilterType configuredType; // can be negative (fin dout)
	private FilterType computedType;
	private int lastRowTested = -1000000;
	// performance of each filter (less is better) (can be negative)
	private double[] lastSums = new double[5];
	// a priori preference (NONE SUB UP AVERAGE PAETH)
	private double[] preference;
	private int discoverEachLines = -1;
	private List<FilterType> toTest;

	public FilterWriteStrategy(ImageInfo imgInfo, FilterType ctype) {
		this.imgInfo = imgInfo;
		preference = imgInfo.indexed || imgInfo.packed ? new double[] { 1.2, 1.1, 1.1, 1.0, 1.1 } : new double[] { 1.3,
				1.1, 1.1, 1.1, 1.2 };
		toTest = new ArrayList<FilterType>(Arrays.asList(FilterType.getAllStandard()));
		configuredType = ctype;
		computedType = FilterType.FILTER_NONE;
	}

	public FilterWriteStrategy(ImageInfo imgInfo) {
		this(imgInfo, FilterType.FILTER_DEFAULT);
	}

	void setConfiguredType(FilterType cType) {
		configuredType = cType;
		discoverEachLines = 0;
		if (configuredType.val >= 0)
			computedType = configuredType;
		else {
			computedType = FilterType.FILTER_PAETH; // set a nice default
			if (configuredType == FilterType.FILTER_AGGRESSIVE)
				discoverEachLines = 8;
			if (configuredType == FilterType.FILTER_VERYAGGRESSIVE)
				discoverEachLines = 1;
			if (configuredType == FilterType.FILTER_DEFAULT) {
				if ((imgInfo.rows < 8 && imgInfo.cols < 8) || imgInfo.indexed || imgInfo.bitDepth < 8)
					computedType = FilterType.FILTER_NONE;
				else
					computedType = FilterType.FILTER_PAETH;
			}
		}
	}

	public List<FilterType> shouldTest(int rown) {
		if (discoverEachLines > 0 && lastRowTested + discoverEachLines <= rown) {
			return toTest;
		} else
			return Collections.emptyList();
	}

	public void setPreference(double none, double sub, double up, double ave, double paeth) {
		preference = new double[] { none, sub, up, ave, paeth };
	}

	public boolean computesStatistics() {
		return (discoverEachLines > 0);
	}

	public void reportResultsForFilter(int rown, FilterType type, byte[] rowbfilter, boolean tentative) {
		if (!computesStatistics())
			return;
		int sum = 0, val;
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			val = rowbfilter[i];
			if (val < 0)
				sum -= (int) val;
			else
				sum += (int) val;
		}
		lastRowTested = rown;
		lastSums[type.val] = sum;
	}

	public FilterType preferedType(int rown) {
		if (configuredType == null || configuredType.val < 0) { // not fixed?
			if (rown == 0)
				computedType = FilterType.FILTER_NONE;
			else if (configuredType == FilterType.FILTER_CYCLIC)
				computedType = FilterType.getByVal((computedType.val + 1) % 5);
			else {
				double bestval = Double.MAX_VALUE;
				double val;
				for (int i = 0; i < 5; i++) {
					val = lastSums[i];
					val /= preference[i];
					if (val <= bestval) {
						bestval = val;
						computedType = FilterType.getByVal(i);
					}
				}
			}
		}
		return computedType;
	}

}
