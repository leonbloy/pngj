package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngHelper;
import ar.com.hjg.pngj.PngWriter;

/**
 * grayscale image - distorted diagonal stripes
 */
public class PngCreateStripes {

	public static void makeTestImage(PngWriter png) {
		int cols = png.imgInfo.cols;
		int rows = png.imgInfo.rows;
		double t1 = (cols + rows) / 16.0; // typical period
		ImageLine iline = new ImageLine(png.imgInfo);
		iline.setRown(0);
		for (int i = 0; i < rows; i++) {
			double fase = Math.sin(1.3 * i / t1);
			for (int j = 0; j < cols; j++) {
				double sin = Math.sin((i + j) * Math.PI / t1 + fase);
				iline.scanline[j] = PngHelper.clampTo_0_255((int) ((sin + 1) * 127 + 0.5));
			}
			png.writeRow(iline);
		}
		png.end();
	}


	public static void createTest(String name, int cols, int rows) {
		PngWriter i2 = FileHelper.createPngWriter(new File(name), new ImageInfo(cols, rows, 8, false, true, false),
				true);
		makeTestImage(i2);
		System.out.println("Done: " + name);
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.err.println("Arguments: [pngdest] [cols] [rows]");
			System.exit(1);
		}
		int cols = Integer.parseInt(args[1]);
		int rows = Integer.parseInt(args[2]);
		createTest(args[0], cols, rows);
	}
}
