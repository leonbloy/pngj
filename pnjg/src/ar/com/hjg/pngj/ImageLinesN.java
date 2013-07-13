package ar.com.hjg.pngj;

import java.util.ArrayList;
import java.util.List;

public class ImageLinesN<T extends IImageLine> {
	
	protected final List<T>  lines;
	protected final int nlines;
	protected final int offset;
	protected final int step;
	
	public ImageLinesN(int nlines,int noffset,int step,IImageLineFactory<T> imageLineFactory,ImageInfo iminfo){
		this.nlines=nlines;
		this.offset=noffset;
		this.step=step;
		lines = new ArrayList<T>();
		for(int i=0;i<nlines;i++)
			lines.add(imageLineFactory.createImageLine(iminfo));
	}
	
	public T getImageLine(int n) {
		return lines.get(n);
	}
	
	/**
	 * How many lines does this object contain
	 * @return
	 */
	public int size() {
		return nlines;
	}
	
	/**
	 * Same as imageRowToMatrixRow, but returns negative if invalid
	 */
	public int imageRowToMatrixRowStrict(int imrow) {
		imrow -= offset;
		int mrow = imrow >= 0 && imrow % step== 0 ? imrow / step : -1;
		return mrow < nlines ? mrow : -1;
	}

	/**
	 * Converts from matrix row number (0 : nRows-1) to image row number
	 * 
	 * @param mrow
	 *            Matrix row number
	 * @return Image row number. Invalid only if mrow is invalid
	 */
	public int matrixRowToImageRow(int mrow) {
		return mrow * step + offset;
	}

	/**
	 * Warning: this always returns a valid matrix row (clamping on 0 : nrows-1,
	 * and rounding down) Eg: rowOffset=4,rowStep=2 imageRowToMatrixRow(17)
	 * returns 6 , imageRowToMatrixRow(1) returns 0
	 */
	public int imageRowToMatrixRow(int imrow) {
		int r = (imrow - offset) / step;
		return r < 0 ? 0 : (r < nlines ? r : nlines - 1);
	}
}
