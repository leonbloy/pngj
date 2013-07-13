package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLine.SampleType;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;

public class TestsHelper {

	public static final String tempDir; // either /tmp or the property "java.io.tmpdir"

	static {
		String tempDirX = "/tmp";
		if (!(new File(tempDirX)).isDirectory())
			tempDirX = System.getProperty("java.io.tmpdir");
		tempDir = tempDirX;
		if (!(new File(tempDir)).isDirectory())
			throw new RuntimeException("Could not set valid temp dir " + tempDir);
	}

	public static void testEqual(File image1, File image2) {
		PngReader png1 = FileHelper.createPngReader(image1);
		PngHelperInternal.initCrcForTests(png1);
		PngReader png2 = FileHelper.createPngReader(image2);
		PngHelperInternal.initCrcForTests(png2);
		if (png1.isInterlaced() != png2.isInterlaced())
			fatalError("Cannot compare, one is interlaced, the other not:" + png1 + " " + png2, png1, png2);
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

	public static File getTmpFile(String suffix) {
		return new File(tempDir, "temp" + suffix + ".png");
	}

	/**
	 * Creates a dummy temp png You should call endFileTmp after adding chunks,
	 * etc
	 * */
	public static PngWriter prepareFileTmp(String suffix, ImageInfo imi) {
		PngWriter png = FileHelper.createPngWriter(getTmpFile(suffix), imi, true);
		return png;
	}

	public static PngWriter prepareFileTmp(String suffix, boolean palette) {
		return prepareFileTmp(suffix, new ImageInfo(32, 32, 8, false, false, palette));
	}

	public static ImageLine generateNoiseLine(ImageInfo imi) { // byte format!
		ImageLine line = new ImageLine(imi, SampleType.BYTE, true);
		Random r = new Random();
		r.nextBytes(line.scanlineb);
		return line;
	}

	public static PngWriter prepareFileTmp(String suffix) {
		return prepareFileTmp(suffix, false);
	}

	public static void endFileTmp(PngWriter png) {
		ImageLine imline = new ImageLine(png.imgInfo);
		for (int i = 0; i < png.imgInfo.rows; i++)
			png.writeRow(imline, i);
		png.end();
	}

	public static PngReader getReaderTmp(String suffix) {
		PngReader p = FileHelper.createPngReader(getTmpFile(suffix));
		return p;
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

	public static File createWaves(String suffix, double scale, ImageInfo imi) {
		File f = getTmpFile(suffix);
		// open image for writing to a output stream
		PngWriter png = FileHelper.createPngWriter(f, imi, true);
		png.getMetadata().setText("key1", "val1");
		ImageLine iline = new ImageLine(imi, SampleType.BYTE, true);
		for (int row = 0; row < png.imgInfo.rows; row++) {

			for (int x = 0; x < imi.cols; x++) {
				int r = (int) ((Math.sin((row + x) * 0.073 * scale) + 1) * 128);
				int g = (int) ((Math.sin((row + x * 0.22) * 0.08 * scale) + 1) * 128);
				int b = (int) ((Math.sin((row * 0.52 - x * 0.2) * 0.21 * scale) + 1) * 128);
				iline.scanlineb[x * imi.channels] = (byte) r;
				iline.scanlineb[x * imi.channels + 1] = (byte) g;
				iline.scanlineb[x * imi.channels + 2] = (byte) b;
				if (imi.channels == 4)
					iline.scanlineb[x * imi.channels + 3] = (byte) ((b + g) / 2);
			}
			png.writeRow(iline, row);
		}
		png.end();
		return f;
	}

	public static class NullOutputStream extends OutputStream {
		private int cont = 0;

		@Override
		public void write(int arg0) throws IOException {
			// nothing!
			cont++;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			cont += len;
		}

		@Override
		public void write(byte[] b) throws IOException {
			cont += b.length;
		}

		public int getCont() {
			return cont;
		}

	}

	public static NullOutputStream createNullOutputStream() {
		return new NullOutputStream();
	}

	public static void main(String[] args) {
		System.out.println(tempDir);
	}
}
