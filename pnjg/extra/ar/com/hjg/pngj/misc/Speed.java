package ar.com.hjg.pngj.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.TestSupport;

/**
 * Creates a huge image
 * <p>
 * This is mainly for profiling
 */
public class Speed {

	/**
	 * return msecs
	 */
	public static int createHuge(String filename, final int cols, final int rows, FilterType filtertype, int compLevel)
			throws Exception {
		OutputStream os = filename == null ? TestSupport.createNullOutputStream() : new FileOutputStream(new File(
				filename));
		PngWriter png = new PngWriter(os, new ImageInfo(cols, rows, 8, false));
		png.setFilterType(filtertype);
		png.setCompLevel(compLevel);
		png.setDeflaterStrategy(Deflater.FILTERED);
		ImageLine iline1 = new ImageLine(png.imgInfo);
		ImageLine iline2 = new ImageLine(png.imgInfo);
		ImageLine iline = iline1;
		for (int j = 0; j < cols; j++) {
			ImageLineHelper.setPixelRGB8(iline1, j, ((j & 0xFF) << 16) | (((j * 3) & 0xFF) << 8) | (j * 2) & 0xFF);
			ImageLineHelper.setPixelRGB8(iline2, j, (j * 13) & 0xFFFFFF);
		}
		long t0 = System.currentTimeMillis();
		for (int row = 0; row < rows; row++) {
			iline = row % 4 == 0 ? iline2 : iline1;
			png.writeRow(iline, row);
		}
		png.end();
		int dt = (int) (System.currentTimeMillis() - t0);
		return dt;
	}

	public static int read(String filename, int ntimes) throws Exception {
		long t0 = System.currentTimeMillis();
		for (int i = 0; i < ntimes; i++) {
			PngReader pngr = new PngReader(new File(filename));
			for (int row = 0; row < pngr.imgInfo.rows; row++)
				pngr.readRow(row);
			pngr.end();
		}
		int dt = (int) (System.currentTimeMillis() - t0);
		return dt;
	}

	// dic 2012 (lev 6; java 6,AMD Phenom(tm) II X4 955) [5000 x 5000] 6352 (VA) 1588 1607 (A) 2635 2682 (C) read(x10) 11685
	// jul 2013 (idem                                    [5000 x 5000] 6398 (VA) 1662 1669 (A) 2695 2681 (C) read(x10) 10045 

	public static void run3(int cols, int rows) throws Exception {
		String f = "C:/temp/huge.png";
		int dt1 = createHuge(f, cols, rows, FilterType.FILTER_VERYAGGRESSIVE, 6);
		int dt3 = createHuge(f, cols, rows, FilterType.FILTER_AGGRESSIVE, 6);
		int dt4 = createHuge(f, cols, rows, FilterType.FILTER_AGGRESSIVE, 6);
		int dt5 = createHuge(f, cols, rows, FilterType.FILTER_CYCLIC, 6);
		int dt6 = createHuge(f, cols, rows, FilterType.FILTER_CYCLIC, 6);
		int dtr = read(f, 10);
		System.out.printf("write [%d x %d] %d (VA) %d %d (A) %d %d (C) read(x10) %d \n", cols, rows, dt1, dt3, dt4,
				dt5, dt6, dtr);
	}

	public static void main(String[] args) throws Exception {
		run3(5000, 5000);
	}

}
