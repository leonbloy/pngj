package ar.com.hjg.pngj.lossy;

import java.util.Arrays;

import ar.com.hjg.pngj.ImageInfo;

/**
 * EXPERIMENTAL
 */
public class ErrorDifussionFloydSteinberg implements IErrorDifussion {

	final int rows, cols, channels;
	private int currentRow;
	private short[] row0, row1;
	private boolean leftToright = true;
	protected ImageInfo imginfo;
	private final int groups;
	private final int alphachannel;
	private final boolean useLuminance;
	private final int channelsNoAlpha;

	/**
	 */
	public ErrorDifussionFloydSteinberg(ImageInfo imginfo,boolean useLuminance) {
		this.useLuminance = useLuminance;
		this.imginfo = imginfo;
		this.rows = imginfo.rows;
		this.cols = imginfo.cols;
		this.channels = imginfo.channels;
		this.groups = useLuminance ? 1 + (imginfo.alpha? 1: 0) : imginfo.channels;
		this.alphachannel = imginfo.alpha ? imginfo.channels-1 : -1;
		channelsNoAlpha = imginfo.alpha ? channels -1 : channels;
		row0 = new short[(cols + 2) * this.groups];
		row1 = new short[(cols + 2) * this.groups];
		currentRow = 0;
	}

	public int getTotalErr(int row, int col, int channel) {
		if (row > currentRow + 1)
			incrementRow();
		int c = groups*col + groups + ( useLuminance ? (channel==alphachannel?1:0) : channel) ;
		int factor = 16;
		if(useLuminance && channel!= alphachannel)
			factor *= channelsNoAlpha;
		if (row == currentRow)
			return row0[c] / factor;
		else if (row == currentRow + 1)
			return row1[c] / factor;
		else
			throw new RuntimeException("bad coordinates");
	}

	/**
	 * you must respect the order! leftoright or righttoleft err = exact - writen warning: here (and in the methods)
	 * 'col' is measure not in pixels but in samples ! = cols x channels x bytespsample
	 **/
	public void addErr(int row, int col, int channel, int err) {
		if (row == currentRow + 1)
			incrementRow();
		if (row != currentRow)
			throw new RuntimeException("bad coordinates!");
		int c = groups*col + groups + ( useLuminance ? (channel==alphachannel?1:0) : channel) ;
		if (leftToright) {
			row0[c + groups] += (7 * err);
			row1[c - groups] += (3 * err);
			row1[c] += (5 * err);
			row1[c + groups] += (err);
		} else {
			row0[c - groups] += (7 * err);
			row1[c + groups] += (3 * err);
			row1[c] += (5 * err);
			row1[c - groups] += (err);
		}
	}

	private void incrementRow() {
		short[] aux = row0;
		row0 = row1;
		row1 = aux;
		Arrays.fill(row1, (short) 0);
		currentRow++;
	}

	public boolean isLeftToright() {
		return leftToright;
	}

	public void setLeftToright(boolean leftToright) {
		this.leftToright = leftToright;
	}

	public void reset() {
		Arrays.fill(row1, (short) 0);
		Arrays.fill(row0, (short) 0);
	}
}
