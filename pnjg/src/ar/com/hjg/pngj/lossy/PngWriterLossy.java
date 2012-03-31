package ar.com.hjg.pngj.lossy;

import java.io.OutputStream;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.PngWriter;

/**
 * Writes a PNG image, line by line, with lossy compression
 * 
 * EXPERIMENTAL - not yet recommended for use
 */
public class PngWriterLossy extends PngWriter {

	protected byte[] rowbrx = null; // rowb as reconstructed in received side
	protected byte[] rowbprevrx = null; // rowb prev as reconstructed in received

	public LossyHelper lossyHelper;

	private boolean enabled = true;

	public static boolean PRINT_WARNINGS = true;
	public static final int LOSSY_DEFAULT = 20;

	public PngWriterLossy(OutputStream outputStream, ImageInfo imgInfo) {
		super(outputStream, imgInfo);
		lossyInit();
	}

	public PngWriterLossy(OutputStream outputStream, ImageInfo imgInfo, String filenameOrDescription) {
		super(outputStream, imgInfo, filenameOrDescription);
		lossyInit();
	}

	private void lossyInit() {
		if (imgInfo.alpha || imgInfo.bitDepth < 8) {
			enabled = false;
			if (PRINT_WARNINGS)
				System.err.println("Lossy mode not enabled for this kind of image : " + getFilename());
			return;
		}
		rowbrx = new byte[rowb.length];
		rowbprevrx = new byte[rowb.length];
		lossyHelper = new LossyHelper(imgInfo);
		setFilterType(FilterType.FILTER_AVERAGE);
		setCompLevel(9);
		setLossy(LOSSY_DEFAULT);
	}

	protected void filterRowUp() {
		if (!enabled) {
			super.filterRowUp();
			return;
		}
		int i, up, here;
		int r0, r0s, r0orig, r1, x1, col;
		for (i = 1; i <= imgInfo.bytesPerRow; i++) {
			col = i - 1;
			up = rowbprevrx[i] & 0xff;
			here = rowb[i] & 0xff;
			r0s = (here - up);
			r0orig = here - (rowbprev[i] & 0xff);
			r0 = r0s & 0xFF;
			lossyHelper.reportOriginalR(r0s, r0orig, rowNum, col);
			r1 = lossyHelper.quantize(r0s, rowNum, col);
			if (r1 != r0) {
				x1 = FilterType.unfilterRowUp(r1, up);
				if (!lossyHelper.isacceptable(here, x1, false)) {
					r1 = r0;
					x1 = FilterType.unfilterRowUp(r0, up);
				}
			} else {
				x1 = FilterType.unfilterRowUp(r0, up);
			}
			rowbrx[i] = (byte) x1;
			rowbfilter[i] = (byte) r1;
			lossyHelper.reportFinalR(r1, rowNum, col);
		}
	}

	protected void filterRowPaeth() {
		throw new RuntimeException("Only AVERAGE filter type is accepted in lossy mode");
	}

	protected void filterRowSub() {
		throw new RuntimeException("Only AVERAGE filter type is accepted in lossy mode");
	}

	@Override
	protected void convertRowToBytes() {
		byte[] tmp = rowbrx; // addition swap
		rowbrx = rowbprevrx;
		rowbprevrx = tmp;
		super.convertRowToBytes();
	}

	protected void filterRowNone() {
		int r0, r0s, r0orig, r1, x1, col, here;
		if (!enabled) {
			super.filterRowNone();
			return;
		}
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			col = i - 1;
			rowbfilter[i] = (byte) rowb[i];
			r0s = rowb[i];
			here = rowb[i] & 0xff;
			r0orig = r0s;
			r0 = r0s & 0xFF;
			lossyHelper.reportOriginalR(r0s, r0orig, rowNum, col);
			r1 = lossyHelper.quantize(r0s, rowNum, col);
			if (r1 != r0) {
				x1 = FilterType.unfilterRowNone(r1);
				if (!lossyHelper.isacceptable(here, x1, false)) {
					r1 = r0;
					x1 = FilterType.unfilterRowNone(r1);
				}
			} else {
				x1 = FilterType.unfilterRowNone(r0);
			}
			rowbrx[i] = (byte) x1;
			rowbfilter[i] = (byte) r1;
			lossyHelper.reportFinalR(r1, rowNum, col);
		}
	}

	@Override
	protected void filterRowAverage() {
		if (!enabled) {
			super.filterRowAverage();
			return;
		}
		int i, j, up, left, here;
		int r0, r0s, r0orig, r1, x1, col;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imgInfo.bytesPerRow; i++, j++) {
			col = i - 1;
			up = rowbprevrx[i] & 0xff;
			left = j > 0 ? rowbrx[j] & 0xff : 0;
			here = rowb[i] & 0xff;
			r0s = (here - (up + left) / 2);
			r0orig = (rowb[i] & 0xff) - ((rowbprev[i] & 0xff) + (j > 0 ? (rowb[j] & 0xFF) : 0)) / 2;
			r0 = r0s & 0xFF;
			lossyHelper.reportOriginalR(r0s, r0orig, rowNum, col);
			r1 = lossyHelper.quantize(r0s, rowNum, col);
			if (r1 != r0) {
				x1 = FilterType.unfilterRowAverage(r1, left, up);
				if (!lossyHelper.isacceptable(here, x1, false)) {
					r1 = r0;
					x1 = FilterType.unfilterRowAverage(r0, left, up);
				}
			} else {
				x1 = FilterType.unfilterRowAverage(r0, left, up);
			}
			rowbrx[i] = (byte) x1;
			rowbfilter[i] = (byte) r1;
			lossyHelper.reportFinalR(r1, rowNum, col);
		}
	}

	public LossyHelper getLossyHelper() {
		return lossyHelper;
	}

	public void setLossy(int lossy) {
		if (lossy == 0)
			enabled = false;
		else {
			if (!enabled)
				throw new RuntimeException("Lossy mode not enabled for this image");
			lossyHelper.setLossy(lossy);
		}
	}

}
