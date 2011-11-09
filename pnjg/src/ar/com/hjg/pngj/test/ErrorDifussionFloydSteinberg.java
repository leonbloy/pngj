package ar.com.hjg.pngj.test;

import java.util.Arrays;

public class ErrorDifussionFloydSteinberg {
	final int rows, cols;
	private int currentRow = 0;
	private int[] row0, row1, aux;
	private boolean leftToright=true;
	
	public ErrorDifussionFloydSteinberg(int rows, int cols) {
		this.rows = rows;
		this.cols = cols;
		row0 = new int[cols+2]; // we number from 1, leaving 1 dummy entry on each side
		row1 = new int[cols+2];
	}

	public int getTotalErr(int row, int col) {
		col++;
		if (row == currentRow)
			return row0[col]/16;
		else if (row == currentRow + 1)
			return row1[col]/16;
		else
			throw new RuntimeException("bad coordinates");
	}

	/** you must respect the order! leftoright or righttoleft 
	 *  err = exact - writen
	 * */
	public void addErr(int row, int col, int err) { 
		col++;
		if (row == currentRow + 1) {
			aux = row0;
			row0 = row1;
			row1 = aux;
			Arrays.fill(row1, 0);
			currentRow++;
		}
		if (row != currentRow)
			throw new RuntimeException("bad coordinates!");
		if(leftToright) {
			row0[col+1] += (7*err);
			row1[col-1] += (3*err);
			row1[col]   += (5*err);
			row1[col+1] += (err);
		} else {
			row0[col-1] += (7*err);
			row1[col+1] += (3*err);
			row1[col]   += (5*err);
			row1[col-1] += (err);
		}
	}

	public boolean isLeftToright() {
		return leftToright;
	}

	public void setLeftToright(boolean leftToright) {
		this.leftToright = leftToright;
	}


}
