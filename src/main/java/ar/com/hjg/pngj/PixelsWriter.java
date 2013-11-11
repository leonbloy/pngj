package ar.com.hjg.pngj;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Writes a set of rows (pixels) as a continuous deflated stream (does not know
 * about IDAT chunk segmentation)
 * 
 * This includes the filter strategy
 * 
 * This can (could) be used in APGN
 */
public abstract class PixelsWriter {

	protected final ImageInfo imgInfo;
	protected OutputStream os;
	private Deflater def;

	private double compressionFactor = 5.0;

	protected final int buflen; // including filter byte
	protected byte[] rowbprev; // previous raw row, including filter byte
	protected byte[] rowb; // raw (unfiltered) row
	protected byte[] rowbfilter; // filtered row

	private DeflaterOutputStream datStreamDeflated;
	int rown; // last writen row

	private boolean initdone = false;
	private boolean filterPreserve=false;


	public PixelsWriter(ImageInfo imgInfo) {
		this.imgInfo = imgInfo;
		buflen = imgInfo.bytesPerRow + 1;
		rown = -1;
	}

	public void filterAndWrite() {
		rown++;
		if (rown == 0)
			init();
		filterRow();
		try {
			datStreamDeflated.write(rowbfilter, 0, buflen);
			if (rown == imgInfo.rows - 1) {
				datStreamDeflated.finish();
			}
		} catch (Exception e) {
			throw new PngjOutputException(e);
		}
		// swap
		byte[] aux = rowbprev;
		rowbprev = rowb;
		rowb = aux;
	}

	/**
	 * 
	 */
	protected abstract void filterRow();

	protected final void filterRowWithFilterType(FilterType filterType) {
		// warning: filters operation rely on: "previous row" (rowbprev) is
		// initialized to 0 the first time
		rowbfilter[0] = (byte) filterType.val;
		switch (filterType) {
		case FILTER_NONE:
			filterRowNone();
			break;
		case FILTER_SUB:
			filterRowSub();
			break;
		case FILTER_UP:
			filterRowUp();
			break;
		case FILTER_AVERAGE:
			filterRowAverage();
			break;
		case FILTER_PAETH:
			filterRowPaeth();
			break;
		default:
			throw new PngjUnsupportedException("Filter type " + filterType
					+ " not recognized");
		}
	}

	private void filterRowNone() {
		System.arraycopy(rowb, 1, rowbfilter, 1, imgInfo.bytesPerRow);
	}

	private void filterRowPaeth() {
		int i, j, imax;
		imax = imgInfo.bytesPerRow;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imax; i++, j++) {
			rowbfilter[i] = (byte) PngHelperInternal.filterRowPaeth(rowb[i],
					j > 0 ? (rowb[j] & 0xFF) : 0, rowbprev[i] & 0xFF,
					j > 0 ? (rowbprev[j] & 0xFF) : 0);
		}
	}

	private void filterRowSub() {
		int i, j;
		for (i = 1; i <= imgInfo.bytesPixel; i++)
			rowbfilter[i] = (byte) rowb[i];
		for (j = 1, i = imgInfo.bytesPixel + 1; i <= imgInfo.bytesPerRow; i++, j++) {
			rowbfilter[i] = (byte) (rowb[i] - rowb[j]);
		}
	}

	private void filterRowUp() {
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			rowbfilter[i] = (byte) (rowb[i] - rowbprev[i]);
		}
	}

	private void filterRowAverage() {
		int i, j, imax;
		imax = imgInfo.bytesPerRow;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imax; i++, j++) {
			rowbfilter[i] = (byte) (rowb[i] - ((rowbprev[i] & 0xFF) + (j > 0 ? (rowb[j] & 0xFF)
					: 0)) / 2);
		}
	}

	/**
	 * This will be called lazily just before writing row 0
	 */
	protected void init() {
		if (initdone)
			return;
		if (def == null) {
			def = new Deflater();
		} else
			def.reset();
		def.setLevel(getDeflaterCompLevel());
		def.setStrategy(deflaterStrategy()); // todo : see this
		datStreamDeflated = new DeflaterOutputStream(os, def);
		if (rowb == null || rowb.length < buflen)
			rowb = new byte[buflen];
		if (rowbfilter == null || rowbfilter.length < buflen)
			rowbfilter = new byte[buflen];
		if (rowbprev == null || rowbprev.length < buflen)
			rowbprev = new byte[buflen];
		else
			Arrays.fill(rowbprev, (byte) 0);
		initdone = true;
	}

	/** cleanup. This should be called explicitly */
	public void end() {
		try {
			datStreamDeflated.finish();
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	/**
	 * Deflater (ZLIB) strategy, see Deflater.DEFAULT_STRATEGY Deflater.FILTERED
	 * Deflater.HUFFMAN_ONLY
	 * 
	 * @return
	 */
	protected int deflaterStrategy() {
		return Deflater.DEFAULT_STRATEGY; // TODO: fix this
	}

	public byte[] getRowb() {
		init();
		return rowb;
	}

	public byte[] getRowbfilter() {
		init();
		return rowbfilter;
	}

	/**
	 * Double value between 0 (no compression) to 10 (maximum compression, worst
	 * speed) Default: 5.0 <br>
	 * This is a general compression factor that tells how much effort is spent
	 * for better compression, it influences the ZLIB copmression level and the
	 * Filter selection heuristic <br>
	 * WARNING: This is not the ZLIB compresion level (though it's roguhly
	 * related)
	 * */
	public void setCompressionFactor(double compressionFactor) {
		this.compressionFactor = compressionFactor;
	}

	/**
	 * @see #setCompressionFactor(double)
	 * @return
	 */
	public double getCompressionFactor() {
		return compressionFactor;
	}

	static double compressionLevelToCompressionFactor(int clevel) {
		double v = clevel * 7.5 / 9.0;
		return v < 0 ? 0.0 : (v > 10.0 ? 10.0 : v);
	}

	static int compressionFactorToCompressionLevel(double cfactor) {
		int level = (int) (cfactor * 9.0 / 7.5 + 0.5); // cfactor=compLevel*7.5/9
		return level < 0 ? 0 : (level > 9 ? 9 : level);
	}

	/**
	 * Deflater (ZLIB) compression level, between 0 (no compression) and 9
	 */
	protected int getDeflaterCompLevel() {
		return compressionFactorToCompressionLevel(compressionFactor);
	}

	final void setOs(OutputStream datStream) {
		this.os= datStream;
	}

	/**
	 * If set to true, then the filter type passed in the raw row is respected
	 * @param filterPreserve (default: false)
	 */
	public void setFilterPreserve(boolean filterPreserve) {
		this.filterPreserve = true;
		
	}

	public boolean isFilterPreserve() {
		return filterPreserve;
	}

}
