package ar.com.hjg.pngj;

import java.util.ArrayList;
import java.util.List;

/**
 * default implementation of ImageLineSetDefault, supports single cursor, full
 * rows, or partial
 */
public abstract class ImageLineSetDefault<T extends IImageLine> implements IImageLineSet<T> {

	protected final ImageInfo imgInfo;
	private final boolean singleCursor;
	private final int nlines, offset, step;
	protected List<T> imageLines; // null if single cursor
	protected T imageLine; // null unless single cursor
	protected int currentRow = -1; // only relevant (and not much) for cursor

	public ImageLineSetDefault(ImageInfo imgInfo, final boolean singleCursor, final int nlines, final int noffset,
			final int step) {
		this.imgInfo = imgInfo;
		this.singleCursor = singleCursor;
		if (singleCursor) {
			this.nlines = 1;
			offset = 0;
			this.step = 1;// don't matter
		} else {
			this.nlines = imgInfo.rows; // note that it can also be 1
			offset = 0;
			this.step = 1;// don't matter
		}
		createImageLines();
	}

	private void createImageLines() {
		if (singleCursor)
			imageLine = createImageLine();
		else {
			imageLines = new ArrayList<T>();
			for (int i = 0; i < nlines; i++)
				imageLines.add(createImageLine());
		}
	}

	protected abstract T createImageLine();

	/**
	 * warning: this is the row number in the original image
	 * 
	 * if this is a cursor, no check is done, always the same row is returned
	 * 
	 */
	public T getImageLine(int n) {
		currentRow = n;
		if (singleCursor)
			return imageLine;
		else
			return imageLines.get(imageRowToMatrixRowStrict(n));
	}

	public boolean hasImageLine(int n) {
		return singleCursor ? currentRow == n : imageRowToMatrixRowStrict(n) >= 0;
	}

	/**
	 * How many lines does this object contain
	 * 
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
		int mrow = imrow >= 0 && imrow % step == 0 ? imrow / step : -1;
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

	/** utility functions, return factories */

	public static IImageLineSetFactory<ImageLineInt> getFactoryInt() {
		return new IImageLineSetFactory<ImageLineInt>() {
			public IImageLineSet<ImageLineInt> create(ImageInfo iminfo, boolean singleCursor, int nlines, int noffset,
					int step) {
				return new ImageLineSetDefault<ImageLineInt>(iminfo, singleCursor, nlines, noffset, step) {
					@Override
					protected ImageLineInt createImageLine() {
						return new ImageLineInt(imgInfo);
					}
				};
			};
		};
	}

	public static IImageLineSetFactory<ImageLineByte> getFactoryByte() {
		return new IImageLineSetFactory<ImageLineByte>() {
			public IImageLineSet<ImageLineByte> create(ImageInfo iminfo, boolean singleCursor, int nlines, int noffset,
					int step) {
				return new ImageLineSetDefault<ImageLineByte>(iminfo, singleCursor, nlines, noffset, step) {
					@Override
					protected ImageLineByte createImageLine() {
						return new ImageLineByte(imgInfo);
					}
				};
			};
		};
	}

}
