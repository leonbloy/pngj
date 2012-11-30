package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;

public class TestHelper {

	public static void testEqual(File image1, File image2) {
		PngReader png1 = FileHelper.createPngReader(image1);
		PngHelperInternal.initCrcForTests(png1);
		PngReader png2 = FileHelper.createPngReader(image2);
		PngHelperInternal.initCrcForTests(png2);
		if (png1.isInterlaced() != png2.isInterlaced())
			fatalError("Cannot compare, one is interlaced, the other not:" + png1 + " " +png2, png1, png2);
		if (!png1.imgInfo.equals(png2.imgInfo))
			fatalError("Image are of different type", png1, png2);
		png1.readRow(png1.imgInfo.rows - 1);
		png2.readRow(png2.imgInfo.rows - 1);
		png1.end();
		png2.end();
		long crc1 = PngHelperInternal.getCrctestVal(png1);
		long crc2 = PngHelperInternal.getCrctestVal(png2);
		if (crc1 != crc2)
			fatalError("different crcs " + image1 + "=" + crc1 + " " + image2 + "=" + crc2, png1, png2);
	}
	
	public static void testCrcEquals(File image1, long crc) {
		PngReader png1 = FileHelper.createPngReader(image1);
		PngHelperInternal.initCrcForTests(png1);
		png1.readRow(png1.imgInfo.rows - 1);
		png1.end();
		long crc1 = PngHelperInternal.getCrctestVal(png1);
		if (crc1 != crc)
			fatalError("different crcs", png1);
	}
	

	public static File addSuffixToName(File orig, String suffix) {
		String x = orig.getPath();
		x = x.replaceAll("\\.png$", "");
		return new File(x + suffix + ".png");
	}

	public static void fatalError(String string, PngReader png1) {
		try {
			png1.end();
		} catch (Exception e) {
		}
		throw new PngjException(string);
	}

	public static void fatalError(String string, PngReader png1, PngReader png2) {
		try {
			png1.end();
			png2.end();
		} catch (Exception e) {
		}
		throw new PngjException(string);
	}

	public static void fatalError(String string, PngReader png1, PngWriter png2) {
		try {
			png1.end();
			png2.end();
		} catch (Exception e) {
		}
		throw new PngjException(string);
	}

	public static void main(String[] args) {
		File f1 = new File("C:/temp/zonas.png");
		File f2 = addSuffixToName(f1, "1");
		testEqual(f1, f2);
	}
}
