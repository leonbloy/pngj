package ar.com.hjg.pngj;

/**
 * Manages the writer strategy for selecting the internal png "filter"
 */
class FilterWriteStrategy {
	private static final int COMPUTE_STATS_EVERY_N_LINES = 8;
	
	final ImageInfo imgInfo;
	private final PngFilterType configuredType; // can be negative (fin dout) 
	private PngFilterType currentType; // 0-4 
	private int lastRowTested = -1000000;
	private long[] lastSums = new long[5];
	private int discoverEachLines = -1;

	FilterWriteStrategy(ImageInfo imgInfo, PngFilterType configuredType) {
		this.imgInfo = imgInfo;
		this.configuredType = configuredType;
		if (configuredType.val < 0) { // first guess
			if ((imgInfo.rows < 8 && imgInfo.cols < 8) || imgInfo.indexed
					|| imgInfo.bitDepth < 8)
				currentType = PngFilterType.FILTER_NONE;
			else
				currentType = PngFilterType.FILTER_PAETH;
		} else {
			currentType = configuredType;
		}
		if (configuredType == PngFilterType.FILTER_AGRESSIVE)
			discoverEachLines = COMPUTE_STATS_EVERY_N_LINES;
	}

	boolean shouldTestAll(int rown) {
		if (discoverEachLines > 0 && lastRowTested + discoverEachLines <= rown)
			return true;
		else
			return false;
	}

	void fillResultsForFilter(int rown, PngFilterType type, long sum) {
		lastRowTested = rown;
		lastSums[type.val] = sum;
		currentType = null;
	}

	PngFilterType gimmeFilterType(int rown) {
		if (currentType == null) { // get better
			long bestsum = Long.MAX_VALUE;
			for (int i = 0; i < 5; i++)
				if (lastSums[i] <= bestsum) {
					bestsum = lastSums[i];
					currentType = PngFilterType.getByVal(i);
				}
		}
		if (configuredType == PngFilterType.FILTER_ALTERNATE) {
			currentType = PngFilterType.getByVal((currentType.val + 1) % 5);
		}
		return currentType;
	}
}
