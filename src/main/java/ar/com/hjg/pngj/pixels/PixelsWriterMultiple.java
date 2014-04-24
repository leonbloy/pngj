package ar.com.hjg.pngj.pixels;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;

// TODO: check that filter for row 0 is NONE/SUB
public class PixelsWriterMultiple extends PixelsWriter {

	protected LinkedList<byte[]> rows; // unfiltered rowsperband elements [0] is the current (rowb). This should include all rows of current band, plus one
	protected List<CompressorStream> filterBank = new ArrayList<CompressorStream>(); // rowsperband elements [0] is the current
	protected byte[][] filteredRows = new byte[5][]; // one for each filter (0=none is not allocated)
	protected byte[] filteredRowTmp; // 
	private FiltersPerformance fPerformance;
	protected int rowsPerBand = 0;
	protected int rowInBand = -1;
	protected int bandNum = -1;
	protected int firstRowInThisBand, lastRowInThisBand;
	protected int rowPerBandCurrent = 0; // lastRowInThisBand-firstRowInThisBand +1 : might be smaller than rowsPerBand

	protected static final int HINT_MEMORY_DEFAULT_KB = 100;
	protected int hintMemoryKb = HINT_MEMORY_DEFAULT_KB; // we will consume about (not more than) this memory (in buffers, not couting the deflaters)
	private int hintRowsPerBand = -1;

	private boolean useLz4 = true;

	public PixelsWriterMultiple(ImageInfo imgInfo) {
		super(imgInfo);
		fPerformance = new FiltersPerformance(imgInfo);
		rows = new LinkedList<byte[]>();
		for (int i = 0; i < 2; i++)
			rows.add(new byte[buflen]); // we preallocate 2 rows (rowb and rowbprev)
		filteredRowTmp = new byte[buflen];
	}

	@Override
	protected void filterAndWrite(byte[] rowb) {
		if (!initdone)
			init();
		if (rowb != rows.get(0))
			throw new RuntimeException("?");
		setBandFromNewRown();
		byte[] rowbprev = rows.get(1);
		for (FilterType ftype : FilterType.getAllStandardNoneLast()) {
			// this has a special behaviour for NONE: filteredRows[0] is null, and the returned value is  rowb
			byte[] filtered = filterRowWithFilterType(ftype, rowb, rowbprev, filteredRows[ftype.val]);
			filterBank.get(ftype.val).write(filtered);
			// adptive: report each filterted
			fPerformance.updateFromFiltered(ftype, filtered, currentRow);
		}
		filteredRows[0] = rowb;
		FilterType preferredAdaptive = fPerformance.getPreferred();
		filterBank.get(5).write(filteredRows[preferredAdaptive.val]);

		if (currentRow == lastRowInThisBand) {
			int best = getBestCompressor();
			//System.out.println("bes comp=" + best + " rows=" + firstRowInThisBand + ":" + lastRowInThisBand);
			byte[] filtersAdapt = filterBank.get(5).getFirstBytes();
			for (int r = firstRowInThisBand, i = 0, j = lastRowInThisBand - firstRowInThisBand; r <= lastRowInThisBand; r++, j--, i++) {
				int fti = best <= 4 ? best : filtersAdapt[i];
				byte[] filtered = null;
				if (r != lastRowInThisBand) {
					filtered = filterRowWithFilterType(FilterType.getByVal(fti), rows.get(j), rows.get(j + 1),
							filteredRowTmp);
				} else { // no need to do this filtering, we already have it
					filtered = filteredRows[fti];
				}
				sendToCompressedStream(filtered);
			}
		}
		// rotate
		if (rows.size() > rowPerBandCurrent) {
			rows.addFirst(rows.removeLast());
		} else
			rows.addFirst(new byte[buflen]);
	}

	@Override
	public byte[] getRowb() {
		return rows.get(0);
	}

	@Override
	protected void init() {
		if (!initdone) {
			super.init();
			for (int i = 1; i <= 4; i++) { // element 0 is not allocated
				if (filteredRows[i] == null || filteredRows[i].length < buflen)
					filteredRows[i] = new byte[buflen];
			}
			if (rowsPerBand == 0)
				rowsPerBand = computeInitialRowsPerBand();
		}
	}

	private void setBandFromNewRown() {
		boolean newBand = currentRow == 0 || currentRow > lastRowInThisBand;
		if (currentRow == 0)
			bandNum = -1;
		if (newBand) {
			bandNum++;
			rowInBand = 0;
		} else {
			rowInBand++;
		}
		if (newBand) {
			firstRowInThisBand = currentRow;
			lastRowInThisBand = firstRowInThisBand + rowsPerBand - 1;
			int lastRowInNextBand = firstRowInThisBand + 2 * rowsPerBand - 1;
			if (lastRowInNextBand >= imgInfo.rows) // hack:make this band bigger, so we don't have a small last band
				lastRowInThisBand = imgInfo.rows - 1;
			rowPerBandCurrent = 1 + lastRowInThisBand - firstRowInThisBand;
			// rebuild bank 
			rebuildFiltersBank();
		}
	}

	protected void rebuildFiltersBank() {
		for (CompressorStream c : filterBank)
			c.close();
		filterBank.clear();
		for (int i = 0; i <= 5; i++) {// one for each filter plus one adaptive
			CompressorStream cp = null;
			if (useLz4)
				cp = new CompressorStreamLz4(null, rowPerBandCurrent, buflen);
			else
				cp = new CompressorStreamDeflater(null, rowPerBandCurrent, buflen);
			if (i == 5)
				cp.setStoreFirstByte(true);
			filterBank.add(cp);
		}
	}

	private int computeInitialRowsPerBand() {
		int r = (int) ((hintMemoryKb * 1024.0) / (imgInfo.bytesPerRow + 1) - 5);
		if (hintRowsPerBand > 0 && r > hintRowsPerBand)
			r = hintRowsPerBand;
		if (r > imgInfo.rows)
			r = imgInfo.rows;
		if (r > 2 && r > imgInfo.rows / 8) { // redistribute more evenly
			int k = (imgInfo.rows + (r - 1)) / r;
			r = (imgInfo.rows + k / 2) / k;
		}
		return r;
	}

	protected int getBestCompressor() {
		double bestcr = Double.MAX_VALUE;
		int bestb = -1;
		for (int i = 0; i < filterBank.size(); i++) {
			CompressorStream fb = filterBank.get(i);
			double cr = fb.getCompressionRatio();
			if (cr < bestcr) {
				bestb = i;
				bestcr = cr;
			}
		}
		return bestb;
	}

	@Override
	protected void initParams() {
		super.initParams();
	}

	@Override
	public void close() {
		super.close();
		rows.clear();
		for (CompressorStream f : filterBank) {
			f.close();
		}
		filterBank.clear();
	}

	public void setHintMemoryKb(int hintMemoryKb) {
		this.hintMemoryKb = hintMemoryKb <= 0 ? HINT_MEMORY_DEFAULT_KB
				: (hintMemoryKb > 100000 ? 100000 : hintMemoryKb);
	}

	public void setHintRowsPerBand(int hintRowsPerBand) {
		this.hintRowsPerBand = hintRowsPerBand;
	}

	public void setUseLz4(boolean lz4) {
		this.useLz4 = lz4;
	}

}
