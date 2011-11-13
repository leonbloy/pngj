package ar.com.hjg.pngj.lossy;

import java.io.OutputStream;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngFilterType;
import ar.com.hjg.pngj.PngWriter;

/**
 * Writes a PNG image, line by line, with lossy
 */
public class PngWriterLossy extends PngWriter {

	protected byte[] rowbrx = null; // rowb as reconstructed in received side
	protected byte[] rowbprevrx = null; // rowb prev as reconstructed in received

	public LossyHelper lossyHelper;

	public PngWriterLossy(OutputStream outputStream, ImageInfo imgInfo) {
		super(outputStream, imgInfo);
		lossyInit();
	}

	public PngWriterLossy(OutputStream outputStream, ImageInfo imgInfo, String filenameOrDescription) {
		super(outputStream, imgInfo, filenameOrDescription);
		lossyInit();
	}

	private void lossyInit() {
		rowbrx = new byte[rowb.length];
		rowbprevrx = new byte[rowb.length];
		lossyHelper = new LossyHelper(imgInfo);
		setFilterType(PngFilterType.FILTER_AVERAGE);
		setCompLevel(9);
	}

	protected void filterRowNone() {
		throw new RuntimeException("Only AVERAGE filter type is accepted in lossy mode");
	}

	protected void filterRowUp() {
		throw new RuntimeException("Only AVERAGE filter type is accepted in lossy mode");
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

	@Override
	protected void filterRowAverage() {
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
				x1 = PngFilterType.unfilterRowAverage(r1, left, up);
				if (!lossyHelper.isacceptable(here, x1, false)) {
					r1 = r0;
					x1 = PngFilterType.unfilterRowAverage(r0, left, up);
				}
			} else {
				x1 = PngFilterType.unfilterRowAverage(r0, left, up);
			}
			rowbrx[i] = (byte) x1;
			rowbfilter[i] = (byte) r1;
			lossyHelper.reportFinalR(r1, rowNum, col);
		}
	}

	public LossyHelper getLossyHelper() {
		return lossyHelper;
	}

}
