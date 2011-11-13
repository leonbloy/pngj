package ar.com.hjg.pngj.lossy;

import java.util.Arrays;

import ar.com.hjg.pngj.ImageInfo;

public class ErrorDifussionFloydSteinberg implements IErrorDifussion {

	final int rows, cols,channels;
	private int currentRow ;
	private short[] row0, row1, aux;
	private boolean leftToright=true;
	private ImageInfo imginfo;
	private final int stride;
	
	/** warning: here (and in the methods) 'col' is measure not in pixels but in samples !  = cols x channels x bytespsample*/ 
	public ErrorDifussionFloydSteinberg(ImageInfo imginfo,int stride) {
		this.rows = imginfo.rows;
		this.cols = imginfo.bytesPerRow;
		this.channels = imginfo.channels;
		this.imginfo = imginfo;
		this.stride = stride>0? stride : imginfo.channels * imginfo.bitDepth/8;
		row0 = new short[cols+2*this.stride]; // we number from stride, leaving 'stride' dummy entries on each side
		row1 = new short[cols+2*this.stride];
		currentRow = 0;
	}

	/** warning: here (and in the methods) 'col' is measure not in pixels but in samples !  = cols x channels x bytespsample*/ 
	public int getTotalErr(int row, int col) {
		col+=stride;
		if (row > currentRow+1)
			incrementeRow();
		if (row == currentRow)
			return row0[col]/16;
		else if (row == currentRow + 1)
			return row1[col]/16;
		else
			throw new RuntimeException("bad coordinates");
	}

	private void incrementeRow() {
		aux = row0;
		row0 = row1;
		row1 = aux;
		Arrays.fill(row1, (short)0);
		currentRow++;
	}
	
	/** you must respect the order! leftoright or righttoleft 
	 *  err = exact - writen
	 * 	warning: here (and in the methods) 'col' is measure not in pixels but in samples !  = cols x channels x bytespsample 
	 **/
	public void addErr(int row, int col, int err) { 
		col+=stride;
		if (row == currentRow + 1) incrementeRow();
		if (row != currentRow)
			throw new RuntimeException("bad coordinates!");
		if(leftToright) {
			row0[col+stride] += (7*err);
			row1[col-stride] += (3*err);
			row1[col]   += (5*err);
			row1[col+stride] += (err);
		} else {
			row0[col-stride] += (7*err);
			row1[col+stride] += (3*err);
			row1[col]   += (5*err);
			row1[col-stride] += (err);
		}
	}

	public boolean isLeftToright() {
		return leftToright;
	}

	public void setLeftToright(boolean leftToright) {
		this.leftToright = leftToright;
	}

	public void reset() {
		Arrays.fill(row1, (short)0);
		Arrays.fill(row0, (short)0);
	}
}
