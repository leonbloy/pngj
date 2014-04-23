package ar.com.hjg.pngj.pixels;

import java.util.Arrays;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngjExceptionInternal;

/** for use in adaptative strategy */
public class FiltersPerformance {

	public FiltersPerformance(ImageInfo imgInfo) {
		this.iminfo = imgInfo;
	}

	private void init() {
		System.arraycopy(filter_preference_default, 0, filter_preference, 0, 5);
		double preferNone = filter_preference[0];
		if (iminfo.bitDepth == 16)
			preferNone = 1.2;
		else if (iminfo.alpha)
			preferNone = 0.8;
		else if (iminfo.indexed || iminfo.bitDepth < 8)
			preferNone = 0.4; // we prefer NONE strongly
		filter_preference[0] = preferNone;
		Arrays.fill(cost, 1.0);
		initdone = true;
	}

	private final ImageInfo iminfo;
	private double memoryA = 0.7; // empirical (not very critical: 0.72)
	private int lastrow = -1;
	double[] absum = new double[5];// depending on the strategy not all values might be computed for all
	double[] entropy = new double[5];
	double[] cost = new double[5];
	int[] histog = new int[256]; // temporary, not normalized
	int lastprefered = -1;
	boolean initdone = false;
	// this values are empirical (montecarlo), for RGB8 images with entropy estimator for NONE and memory=0.7
	// DONT MODIFY THIS
	public static final double[] filter_preference_default = { 0.73, 1.03, 0.97, 1.11, 1.22 }; // lower is prefered

	private double[] filter_preference = new double[5];

	public void updateFromFiltered(FilterType ftype, byte[] rowff, int rown) {
		updateFromRawOrFiltered(ftype, rowff, null, null, rown);
	}

	/** alternative: computes statistic without filtering */
	public void updateFromRaw(FilterType ftype, byte[] rowb, byte[] rowbprev, int rown) {
		updateFromRawOrFiltered(ftype, null, rowb, rowbprev, rown);
	}

	private void updateFromRawOrFiltered(FilterType ftype, byte[] rowff, byte[] rowb, byte[] rowbprev, int rown) {
		if (!initdone)
			init();
		if (rown != lastrow) {
			Arrays.fill(absum, Double.NaN);
			Arrays.fill(entropy, Double.NaN);
		}
		lastrow = rown;
		if (rown == 0 && ftype != FilterType.FILTER_NONE && ftype != FilterType.FILTER_SUB)
			return;
		if (rowff != null)
			computeHistogram(rowff);
		else
			computeHistogramForFilter(ftype, rowb, rowbprev);
		if (ftype == FilterType.FILTER_NONE)
			entropy[ftype.val] = computeEntropyFromHistogram();
		else
			absum[ftype.val] = computeAbsFromHistogram();
	}

	public FilterType getPreferred() {
		int fi = 0;
		double vali = Double.MAX_VALUE, val = 0; // lower wins
		for (int i = 0; i < 5; i++) {
			if (!Double.isNaN(absum[i])) {
				val = absum[i];
			} else if (!Double.isNaN(entropy[i])) {
				val = (Math.pow(2.0, entropy[i]) - 1.0) * 0.5;
			} else
				continue;
			val *= filter_preference[i];
			val = cost[i] * memoryA + (1 - memoryA) * val;
			cost[i] = val;
			if (val < vali) {
				vali = val;
				fi = i;
			}
		}
		lastprefered = fi;
		//if(lastrow %100==1)		System.out.println("rown="+lastrow + " pref=" + fi+ " " + Arrays.toString(cost));
		return FilterType.getByVal(lastprefered);
	}

	public final void computeHistogramForFilter(FilterType filterType, byte[] rowb, byte[] rowbprev) {
		Arrays.fill(histog, 0);
		int i, j, imax = iminfo.bytesPerRow;
		switch (filterType) {
		case FILTER_NONE:
			for (i = 1; i <= imax; i++)
				histog[rowb[i] & 0xFF]++;
			break;
		case FILTER_PAETH:
			for (i = 1; i <= imax; i++)
				histog[PngHelperInternal.filterRowPaeth(rowb[i], 0, rowbprev[i] & 0xFF, 0)]++;
			for (j = 1, i = iminfo.bytesPixel + 1; i <= imax; i++, j++)
				histog[PngHelperInternal
						.filterRowPaeth(rowb[i], rowb[j] & 0xFF, rowbprev[i] & 0xFF, rowbprev[j] & 0xFF)]++;
			break;
		case FILTER_SUB:
			for (i = 1; i <= iminfo.bytesPixel; i++)
				histog[rowb[i] & 0xFF]++;
			for (j = 1, i = iminfo.bytesPixel + 1; i <= imax; i++, j++)
				histog[(rowb[i] - rowb[j]) & 0xFF]++;
			break;
		case FILTER_UP:
			for (i = 1; i <= iminfo.bytesPerRow; i++)
				histog[(rowb[i] - rowbprev[i]) & 0xFF]++;
			break;
		case FILTER_AVERAGE:
			for (i = 1; i <= iminfo.bytesPixel; i++)
				histog[((rowb[i] & 0xFF) - ((rowbprev[i] & 0xFF)) / 2) & 0xFF]++;
			for (j = 1, i = iminfo.bytesPixel + 1; i <= imax; i++, j++)
				histog[((rowb[i] & 0xFF) - ((rowbprev[i] & 0xFF) + (rowb[j] & 0xFF)) / 2) & 0xFF]++;
			break;
		default:
			throw new PngjExceptionInternal("Bad filter:" + filterType);
		}
	}

	public void computeHistogram(byte[] rowff) {
		Arrays.fill(histog, 0);
		for (int i = 1; i < iminfo.bytesPerRow; i++)
			histog[rowff[i] & 0xFF]++;
	}

	public double computeAbsFromHistogram() {
		int s = 0;
		for (int i = 1; i < 128; i++)
			s += histog[i] * i;
		for (int i = 128, j = 128; j > 0; i++, j--)
			s += histog[i] * j;
		return s / (double) iminfo.bytesPerRow;
	}

	private final static double LOG2NI = -1.0 / Math.log(2.0);

	public final double computeEntropyFromHistogram() {
		double s = 1.0 / iminfo.bytesPerRow;
		double ls = Math.log(s);

		double h = 0;
		for (int x : histog) {
			if (x > 0)
				h += (Math.log(x) + ls) * x;
		}
		h *= s * LOG2NI;
		if (h < 0.0)
			h = 0.0;
		return h;
	}

}
