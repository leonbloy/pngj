package ar.com.hjg.pngj.lossy;

import java.util.Arrays;

import ar.com.hjg.pngj.ImageInfo;

public class ErrorDifussionTrivial implements IErrorDifussion {

	private boolean leftToright=true;
	private final int stride;
	private int[] currentErr; 
	private int currentRow =0;
	/** warning: here (and in the methods) 'col' is measure not in pixels but in samples !  = cols x channels x bytespsample*/ 
	public ErrorDifussionTrivial(ImageInfo imginfo,int stride) {
		this.stride = stride>0? stride : imginfo.channels * imginfo.bitDepth/8;
		currentRow = 0;
		currentErr = new int[this.stride];
		Arrays.fill(currentErr, 0);
	}

	/** warning: here (and in the methods) 'col' is measure not in pixels but in samples !  = cols x channels x bytespsample*/ 
	public int getTotalErr(int row, int col) {
		if(row!=currentRow) {
			currentRow=row;
			Arrays.fill(currentErr, 0);
		}
		return currentErr[col%stride];
	}

	public void addErr(int row, int col, int err) { 
		if(row!=currentRow) {
			currentRow=row;
			Arrays.fill(currentErr, 0);
		}
		int i =(col)%stride;
		currentErr[i]=err;
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
