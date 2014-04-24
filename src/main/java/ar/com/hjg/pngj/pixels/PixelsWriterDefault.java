package ar.com.hjg.pngj.pixels;

import java.util.Arrays;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngjOutputException;

/**
 */
public class PixelsWriterDefault extends PixelsWriter {

	protected int adaptMaxSkip = 8; // set in initParams, does not change   
	protected int adaptSkipIncreaseSinceRow = 8; // set in initParams, does not change 
	protected int adaptCurSkip = 0;
	protected int adaptNextRow = 0;

	private FiltersPerformance filtersPerformance;
	protected FilterType curfilterType;
	private byte[] rowb;
	private byte[] rowbfilter;
	private byte[] rowbprev;

	public PixelsWriterDefault(ImageInfo imgInfo) {
		super(imgInfo);
	}

	@Override
	protected void initParams() {
		super.initParams();
		filtersPerformance = new FiltersPerformance(imgInfo); // TODO: tweak parameters
		if (imgInfo.getTotalPixels() <= 1024 && !FilterType.isValidStandard(filterType))
			filterType = getDefaultFilter();
		if (FilterType.isAdaptive(filterType)) {
			adaptCurSkip = 0;
			adaptNextRow = 0;
			if (filterType == FilterType.FILTER_ADAPTIVE_FAST) {
				adaptMaxSkip = 1024;
				adaptSkipIncreaseSinceRow = 1;
			} else if (filterType == FilterType.FILTER_ADAPTIVE_MEDIUM) {
				adaptMaxSkip = 15;
				adaptSkipIncreaseSinceRow = 8;
			} else if (filterType == FilterType.FILTER_ADAPTIVE_FULL) {
				adaptMaxSkip = 0;
				adaptSkipIncreaseSinceRow = 32;
			} else
				throw new PngjOutputException("bad filter " + filterType);
		}
	}

	@Override
	protected void filterAndWrite(final byte[] rowb) {
		if (rowb != this.rowb)
			throw new RuntimeException("??"); //we rely on this
		decideCurFilterType();
		byte[] filtered = filterRowWithFilterType(curfilterType, rowb, rowbprev, rowbfilter);
		sendToCompressedStream(filtered);
		// swap rowb <-> rowbprev
		byte[] aux = this.rowb;
		this.rowb = rowbprev;
		rowbprev = aux;
	}

	protected void decideCurFilterType() {
		// decide the real filter and store in curfilterType
		if (FilterType.isValidStandard(getFilterType())) {
			curfilterType = getFilterType();
		} else if (getFilterType() == FilterType.FILTER_PRESERVE) {
			curfilterType = FilterType.getByVal(rowb[0]);
		} else if (getFilterType() == FilterType.FILTER_CYCLIC) {
			curfilterType = FilterType.getByVal(currentRow % 5);
		} else if (getFilterType() == FilterType.FILTER_DEFAULT) {
			setFilterType(getDefaultFilter());
			curfilterType = getFilterType(); // this could be done once 
		} else if (FilterType.isAdaptive(getFilterType())) {// adaptive
			if (currentRow == adaptNextRow) {
				if (currentRow >= adaptSkipIncreaseSinceRow && adaptCurSkip < adaptMaxSkip)
					adaptCurSkip++;
				adaptNextRow = currentRow + 1 + adaptCurSkip;
				for (FilterType ftype : FilterType.getAllStandard())
					filtersPerformance.updateFromRaw(ftype, rowb, rowbprev, currentRow);
				curfilterType = filtersPerformance.getPreferred();
			}
		} else {
			throw new PngjOutputException("not implemented filter: " + getFilterType());
		}
	}

	@Override
	public byte[] getRowb() {
		if (!initdone)
			init();
		return rowb;
	}

	@Override
	protected void init() {
		super.init();
		if (rowb == null || rowb.length < buflen)
			rowb = new byte[buflen];
		if (rowbfilter == null || rowbfilter.length < buflen)
			rowbfilter = new byte[buflen];
		if (rowbprev == null || rowbprev.length < buflen)
			rowbprev = new byte[buflen];
		else
			Arrays.fill(rowbprev, (byte) 0);

	}

	@Override
	public void close() {
		super.close();
	}

}
