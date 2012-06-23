package ar.com.hjg.pngj.lossy;

import java.util.Arrays;

import ar.com.hjg.pngj.ImageInfo;

public class ErrorDifussionTrivial implements IErrorDifussion {

	private boolean leftToright = true;
	private final int groups;
	private final int alphachannel; 
	private int[] currentErr;
	private int currentRow = 0;
	
	/**
	 * warning: here (and in the methods) 'col' is measure not in pixels but in samples ! = cols x channels x
	 * bytespsample
	 */
	public ErrorDifussionTrivial(ImageInfo imginfo) {
		alphachannel = imginfo.alpha ? ( imginfo.greyscale ? 1 : 3) : -1;
		this.groups = imginfo.alpha ? 2 : 1;
		currentRow = 0;
		currentErr = new int[this.groups];
		Arrays.fill(currentErr, 0);
	}

	public int getTotalErr(int row, int col,int channel) {
		if (row != currentRow) {
			currentRow = row;
			Arrays.fill(currentErr, 0);
		}
		return currentErr[channel==alphachannel? 1 : 0];
	}

	public void addErr(int row, int col, int channel,int err) {
		if (row != currentRow) {
			currentRow = row;
			Arrays.fill(currentErr, 0);
		}
		currentErr[channel==alphachannel? 1 : 0] = err;
	}

	public boolean isLeftToright() {
		return leftToright;
	}

	public void setLeftToright(boolean leftToright) {
		this.leftToright = leftToright;
	}

	public void reset() {
		Arrays.fill(currentErr, 0);
	}
}
