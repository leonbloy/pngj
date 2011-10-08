package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.nosandbox.FileHelper;

/**
 */
public class PngCreateSin {
	private static void makeTestImage(PngWriter png,double t1){
		int t1i = (int)(t1+0.5);
		int cols = png.imgInfo.cols;
		int rows = png.imgInfo.rows;
		ImageLine iline = new ImageLine(png.imgInfo);
		iline.setRown(0);
		for (int i=0;i<rows;i++) {
			double fase = Math.PI*(i%t1i)/t1; // 0:2pi 
				
			png.writeRow(iline);
		}
	}

	

	private static double clamp(double d, double d0, double d1) {
		return d > d1 ? d1 : (d < d0 ? d0 : d);
	}

	public static void createTestSin(String name, int cols, int rows, double t1)
  {
		PngWriter i2 = FileHelper.createPngWriter(new File(name), 
				new ImageInfo(cols, rows, 16,false,true,false),true);
		makeTestImage(i2,t1);
		System.out.println("Done: " + i2.getFilename());
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.err.println("Arguments: [pngdest] [cols] [rows] [t1]");
			System.exit(1);
		}
		int type = args.length == 6 ? Integer.parseInt(args[5]) : 1;
		int cols = Integer.parseInt(args[1]);
		int rows = Integer.parseInt(args[2]);
		double t1 = Double.parseDouble(args[3]);
		createTestSin(args[0], cols, rows, t1);
	}
}
