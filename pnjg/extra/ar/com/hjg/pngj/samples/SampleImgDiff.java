package ar.com.hjg.pngj.samples;

import java.io.File;

import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngReader;

/**
 * Utility (and example) to verify that two images are identical. Computes the
 * maximum difference.
 * 
 * TODO: Add alpha
 */
public class SampleImgDiff {
	/**
	 * asume que las imagenes son equivalentes res[0]=maxima diferencia (suma
	 * valores absoluto sobre todos los canales) res[1]=columna en que se
	 * produce
	 * 
	 * ignores alpha
	 */
	private static int[] computeDiffLine(ImageLine l1, ImageLine l2) {
		double maxDif = -1;
		int maxDifCol = -1;
		double dif;
		int channels = l1.imgInfo.channels;
		int[] scanline1 = l1.getScanline();
		int[] scanline2 = l2.getScanline();
		for (int col = 0; col < l1.imgInfo.cols; col++) {
			dif = Math.abs(scanline1[col * channels] - scanline2[col * channels])
					+ Math.abs(scanline1[col * channels + 1] - scanline2[col * channels + 1])
					+ Math.abs(scanline1[col * channels + 2] - scanline2[col * channels + 2]);
			if (channels == 4)
				dif += Math.abs(scanline1[col * channels + 3] - scanline2[col * channels + 3]);
			if (dif > maxDif) {
				maxDif = dif;
				maxDifCol = col;
			}
		}
		return new int[] { (int) (maxDif + 0.5), maxDifCol };
	}

	public static void showDiff(String f1, String f2) {
		int maxDif = -1;
		int maxDifCol = -1;
		int maxDifRow = -1;
		PngReader i1 = new PngReader(new File(f1));
		PngReader i2 = new PngReader(new File(f2));
		System.out.println(i1.toString());
		System.out.println(i2.toString());
		if (i1.imgInfo.channels < 3)
			throw new RuntimeException("Images must be RGB or RGBA");
		if (!i1.imgInfo.equals(i2.imgInfo))
			throw new RuntimeException("Images must be comparable (same sizes and type) ");
		int rows = i1.imgInfo.rows;
		for (int row = 0; row < rows; row++) {
			ImageLine l1 = (ImageLine) i1.readRow(row);
			ImageLine l2 = (ImageLine) i2.readRow(row);
			int[] res = computeDiffLine(l1, l2);
			if (res[0] > maxDif) {
				maxDif = res[0];
				maxDifCol = res[1];
				maxDifRow = row;
			}
		}
		i1.end();
		i2.end();
		if (maxDif == 0)
			System.out.println("No difference");
		else
			System.out.printf("Images differ. MaxDif=%d [%d %d]\n", maxDif, maxDifCol, maxDifRow);
	}

	public static void main(String[] args) {
		if (args.length != 2 || args[0].equals(args[1])) {
			System.err.println("Enter two filenames (images to be compared)");
			System.exit(1);
		}
		showDiff(args[0], args[1]);
	}
}
