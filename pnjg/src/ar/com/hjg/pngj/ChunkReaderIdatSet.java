package ar.com.hjg.pngj;

import java.util.Arrays;

public class ChunkReaderIdatSet extends ChunkReaderDeflatedSet {

	protected byte rowUnfiltered[];
	protected byte rowUnfilteredPrev[];
	protected final ImageInfo imgInfo;
	protected final PngDeinterlacer deinterlacer;

	public ChunkReaderIdatSet(ImageInfo iminfo, PngDeinterlacer deinterlacer) {
		super(deinterlacer != null ? deinterlacer.getBytesToRead() + 1 : iminfo.bytesPerRow + 1, iminfo.bytesPerRow + 1);
		this.imgInfo = iminfo;
		this.deinterlacer = deinterlacer;
	}

	protected void postProcessRow() {
	}

	@Override
	protected boolean isAsyncMode() {
		return false; // sync mode
	}

	public byte[] getUnfilteredRow() {
		if (rowUnfiltered == null || rowUnfiltered.length < row.length) {
			rowUnfiltered = new byte[row.length];
			rowUnfilteredPrev = new byte[row.length];
		}
		if (currentRow() == 0)
			Arrays.fill(rowUnfiltered, (byte) 0); // see swap that follows
		// swap
		byte[] tmp = rowUnfiltered;
		rowUnfiltered = rowUnfilteredPrev;
		rowUnfilteredPrev = tmp;
		unfilterRow(currentBytes());
		return rowUnfiltered;
	}

	public void advanceToNextRow() {
		int bytesNextRow = imgInfo.bytesPerRow + 1;
		if (deinterlacer != null) {
			deinterlacer.nextRow();
			bytesNextRow = deinterlacer.getBytesToRead() + 1;
		}
		setNextRowLen(bytesNextRow);
	}

	public int currentRow() { // 
		return deinterlacer == null ? getRown() : deinterlacer.getCurrRowReal();
	}

	public int currentOffsetX() { // if interlaced this is in pixels
		return deinterlacer == null ? 0 : deinterlacer.getoX();
	}

	public int currentdX() { // in pixels
		return deinterlacer == null ? 1 : deinterlacer.getdX();
	}

	/**
	 * not including first byte! (filter)
	 * 
	 * @return
	 */
	public int currentBytes() {
		return deinterlacer == null ? imgInfo.bytesPerRow : deinterlacer.getBytesToRead();
	}

	// nbytes: NOT including the filter byte. leaves result in rowb
	protected void unfilterRow(int nbytes) {
		int ftn = row[0];
		FilterType ft = FilterType.getByVal(ftn);
		if (ft == null)
			throw new PngjInputException("Filter type " + ftn + " invalid");
		switch (ft) {
		case FILTER_NONE:
			unfilterRowNone(nbytes);
			break;
		case FILTER_SUB:
			unfilterRowSub(nbytes);
			break;
		case FILTER_UP:
			unfilterRowUp(nbytes);
			break;
		case FILTER_AVERAGE:
			unfilterRowAverage(nbytes);
			break;
		case FILTER_PAETH:
			unfilterRowPaeth(nbytes);
			break;
		default:
			throw new PngjInputException("Filter type " + ftn + " not implemented");
		}
	}

	private void unfilterRowAverage(final int nbytes) {
		int i, j, x;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= nbytes; i++, j++) {
			x = j > 0 ? (rowUnfiltered[j] & 0xff) : 0;
			rowUnfiltered[i] = (byte) (row[i] + (x + (rowUnfilteredPrev[i] & 0xFF)) / 2);
		}
	}

	private void unfilterRowNone(final int nbytes) {
		for (int i = 1; i <= nbytes; i++) {
			rowUnfiltered[i] = (byte) (row[i]);
		}
	}

	private void unfilterRowPaeth(final int nbytes) {
		int i, j, x, y;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= nbytes; i++, j++) {
			x = j > 0 ? (rowUnfiltered[j] & 0xFF) : 0;
			y = j > 0 ? (rowUnfilteredPrev[j] & 0xFF) : 0;
			rowUnfiltered[i] = (byte) (row[i] + PngHelperInternal.filterPaethPredictor(x, rowUnfilteredPrev[i] & 0xFF,
					y));
		}
	}

	private void unfilterRowSub(final int nbytes) {
		int i, j;
		for (i = 1; i <= imgInfo.bytesPixel; i++) {
			rowUnfiltered[i] = (byte) (row[i]);
		}
		for (j = 1, i = imgInfo.bytesPixel + 1; i <= nbytes; i++, j++) {
			rowUnfiltered[i] = (byte) (row[i] + rowUnfiltered[j]);
		}
	}

	private void unfilterRowUp(final int nbytes) {
		for (int i = 1; i <= nbytes; i++) {
			rowUnfiltered[i] = (byte) (row[i] + rowUnfilteredPrev[i]);
		}
	}

	public PngDeinterlacer getDeinterlacer() {
		return deinterlacer;
	}

}