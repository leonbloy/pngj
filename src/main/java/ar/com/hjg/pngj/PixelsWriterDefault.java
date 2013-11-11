package ar.com.hjg.pngj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 */
public class PixelsWriterDefault extends PixelsWriter {

	protected FilterType configuredType; // can be negative (fin dout)
	protected FilterType computedType;
	protected int lastRowTested = -1000000;
	// performance of each filter (less is better) (can be negative)
	protected double[] lastSums = new double[5];
	protected double preferenceForNone = 0.85;
	protected int discoverEachLines = -1;
	private FilterType[] toTest;

	public PixelsWriterDefault(ImageInfo imgInfo) {
		super(imgInfo);
		toTest =  new FilterType[]{FilterType.FILTER_NONE,FilterType.FILTER_PAETH
				,FilterType.FILTER_SUB,FilterType.FILTER_UP,FilterType.FILTER_AVERAGE};
		setConfiguredType(FilterType.FILTER_AGGRESSIVE);
	}

	protected void filterRow() {
		FilterType filterType = FilterType.FILTER_UNKNOWN;
		if (isFilterPreserve() && FilterType.isValidStandard(rowb[0])) {
			filterType = FilterType.getByVal(rowb[0]); // preserve original
														// filter
		} else {
			for (FilterType ftype : shouldTest(rown)) {
				filterRowWithFilterType(ftype);
				reportResultsForFilter(rown, ftype, rowbfilter, true);
			}
			filterType = preferedType(rown);
		}
		filterRowWithFilterType(filterType);
		reportResultsForFilter(rown, filterType, rowbfilter, false);
	}

	protected void init() {
		super.init();
	}

	public void end() {
		super.end();
	}

	protected int deflaterStrategy() {
		return super.deflaterStrategy();
	}

	protected int getDeflaterCompLevel() {
		return super.getDeflaterCompLevel();
	}

	public void setConfiguredType(FilterType cType) {
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
				if ((imgInfo.rows < 8 && imgInfo.cols < 8) || imgInfo.indexed
						|| imgInfo.bitDepth < 8)
					computedType = FilterType.FILTER_NONE;
				else
					computedType = FilterType.FILTER_PAETH;
			}
		}
	}

	protected FilterType[] shouldTest(int rown) {
		if (discoverEachLines > 0 && lastRowTested + discoverEachLines <= rown) {
			return toTest;
		} else
			return new FilterType[]{};
	}

	protected boolean computesStatistics() {
		return (discoverEachLines > 0);
	}

	protected void reportResultsForFilter(int rown, FilterType type,
			byte[] rowbfilter, boolean tentative) {
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

	protected FilterType preferedType(int rown) {
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
					if (FilterType.FILTER_NONE.val == i)
						val *= preferenceForNone;
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
