package ar.com.hjg.pngj.samples;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.pixels.PixelsWriterDefault;
import ar.com.hjg.pngj.test.TestSupport;

/**
 * Creates a huge image
 * <p>
 * This is mainly for profiling
 */
public class CreateHuge {

	/**
	 * if filename==null, the image is writen to a black hole (like a /dev/null
	 */
	public static void createHuge(String filename, final int cols, final int rows) throws Exception {
		OutputStream os = filename == null ? TestSupport.createNullOutputStream()
				:new BufferedOutputStream( new FileOutputStream(new File(filename)));
		PngWriter png = new PngWriter(os, new ImageInfo(cols, rows, 8, false));
		((PixelsWriterDefault) png.getPixelsWriter()).setFilterType(FilterType.FILTER_AVERAGE);
		png.setIdatMaxSize(0x10000);
		png.setCompLevel(6);
		ImageLineInt iline1 = new ImageLineInt(png.imgInfo);
		ImageLineInt iline2 = new ImageLineInt(png.imgInfo);
		ImageLineInt iline = iline1;
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
		System.out.println("Created: " + png.imgInfo.toString());
		System.out.printf("%d msecs, %.1f msecs/MPixel \n", dt, dt * 1000000.0 / (cols * rows));
	}

	public static void run3(int cols, int rows) throws Exception {
		createHuge(null, cols, rows);
		createHuge(null, cols, rows);
		createHuge(null, cols, rows);
	}

	public static void main(String[] args) throws Exception {
		run3(5000, 5000);
	}

}
