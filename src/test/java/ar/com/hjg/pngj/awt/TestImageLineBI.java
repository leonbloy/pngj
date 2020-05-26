package ar.com.hjg.pngj.awt;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Test;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReaderByte;
import ar.com.hjg.pngj.awt.ImageLineBI.BufferedImage2PngAdapter;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;
import ar.com.hjg.pngj.cli.CliArgs;
import ar.com.hjg.pngj.test.TestSupport;

public class TestImageLineBI {

	private static final boolean removeTmpFiles = false;
	private static final boolean verbose = false;
	private Set<File> tmpFilesToDelete = new HashSet<File>();

	public static List<File> getImagesBank(int bank) {
		List<File> pngs = null;
		if (bank == 1)
			pngs = CliArgs.listPngFromDir(TestSupport.absFile("colormodels"), true);
		if (bank == 2)
			pngs = CliArgs.listPngFromDir(TestSupport.absFile("grays"), true);
		return pngs;
	}

	private void delOnExit(File f) {
		tmpFilesToDelete.add(f);
	}

	Random rand = new Random(1);

	/** Reads via our BIreader, writes via ImageIO, and compares pixel by pixel */
	private void testRead(File ori, boolean preferCustom) {
		PngReaderBI png = new PngReaderBI(ori);
		if (verbose)
			PngHelperInternal.debug(String.format("====testing with values %s cust=%s==",
					ori.getName() + " " + png.imgInfo.toStringBrief(), preferCustom));
		png.setPreferCustomInsteadOfBGR(preferCustom);
		File dest = TestSupport.absFile("test/__test.tmp.png");
		delOnExit(dest);
		BufferedImage img = png.readAll();
		if (verbose)
			PngHelperInternal.debug(ImageIoUtils.imageTypeName(img.getType()));
		ImageIoUtils.writePng(dest, img);
		TestSupport.testSameValues(ori, dest);
	}

	private void testWrite1(File f, int convToType, boolean forceRgb) {
		testWrite1(f, convToType, forceRgb, 1);
	}

	/**
	 * Reads via via ImageIO, (optionally does a BI conversion), writes via our
	 * PngWriterBI and compares pixel by pixel
	 * 
	 * @param f          Png file
	 * @param convToType one of the BufferedImage types (negative if not conversion)
	 * @param forceRgb   If true, we force resort to getRGB in our internal logic
	 */
	private void testWrite1(File f, int convToType, boolean forceRgb, int tolerance) {
		try {
			File dest = TestSupport.absFile("test/__test.tmp.png");
			delOnExit(dest);
			BufferedImage bi1 = ImageIoUtils.readPng(f);
			if (verbose)
				PngHelperInternal.debug(f + " type=" + ImageIoUtils.imageTypeName(bi1.getType()) + " conv to "
						+ (convToType > -1 ? ImageIoUtils.imageTypeName(convToType) : "-") + " force RGB=" + forceRgb);
			BufferedImage bi2 = null;
			if (convToType > 0 && convToType != bi1.getType()) {
				bi2 = new BufferedImage(bi1.getWidth(), bi1.getHeight(), convToType);
				bi2.getGraphics().drawImage(bi1, 0, 0, null);
			}
			BufferedImage2PngAdapter adap = new BufferedImage2PngAdapter(bi2 != null ? bi2 : bi1);
			adap.forceresortToGetRGB = forceRgb;
			PngWriterBI pngw = PngWriterBI.createInstance(adap, dest);
			pngw.writeAll();
			TestSupport.testSameValues(f, dest, tolerance);
		} catch (AssertionFailedError e) {
			System.err.println("Error with " + f + " typeconv=" + ImageIoUtils.imageTypeNameShort(convToType)
					+ " forceRgb=" + forceRgb);
			throw e;
		}
	}

	@Test
	public void testRead1() {
		for (File png : getImagesBank(1)) {
			testRead(png, rand.nextBoolean());
		}
	}

	@Test
	public void testRead2() {
		for (File png : getImagesBank(2)) {
			testRead(png, rand.nextBoolean());
		}
	}

	@Test
	public void testReadPartialRgb8() {
		List<File> pngs = getImagesBank(1);
		File ori = pngs.get(0);
		File dest = TestSupport.absFile("test/__test.tmp.png");
		File dest2 = TestSupport.absFile("test/__test2.tmp.png");
		delOnExit(dest);
		delOnExit(dest2);
		{
			System.out.println(ori);
			PngReaderBI png = new PngReaderBI(ori);
			System.out.println(png.imgInfo.rows);
			int offset = 1, step = 2, nlines = (png.imgInfo.rows - offset - 1) / step + 1;
			// dest.deleteOnExit();
			BufferedImage img = png.readAll(nlines, offset, step); // 10 lines, starting from 1, skipping 1
			// System.err.println(ImageIoUtils.imageTypeName(img.getType()));
			ImageIoUtils.writePng(dest, img);
			TestSupport.copyPartial(ori, dest2, nlines, step, offset, false);
		}
		TestSupport.testSameValues(dest, dest2);
	}

	@Test
	public void testWrite1() {
		for (File png : getImagesBank(1)) {
			PngReaderByte p = new PngReaderByte(png);
			ImageInfo imi = p.imgInfo;
			boolean hastrns = p.getChunksList().getById1(PngChunkTRNS.ID) != null;
			boolean isalphaortrns = hastrns || imi.alpha;
			p.close();
			// dont try conversions that loose info
			if (imi.bitDepth <= 8 && !isalphaortrns) {
				testWrite1(png, BufferedImage.TYPE_3BYTE_BGR, rand.nextBoolean());
				testWrite1(png, BufferedImage.TYPE_INT_RGB, rand.nextBoolean());
				testWrite1(png, BufferedImage.TYPE_INT_BGR, rand.nextBoolean());
			}
			if (imi.bitDepth <= 8) {
				// testWrite1(png, BufferedImage.TYPE_4BYTE_ABGR, rand.nextBoolean());
				// THIS FAILS testWrite1(png, BufferedImage.TYPE_INT_ARGB, rand.nextBoolean());
				// testWrite1(png, BufferedImage.TYPE_4BYTE_ABGR_PRE, rand.nextBoolean());
			}
			// testWrite1(png, BufferedImage.TYPE_CUSTOM, rand.nextBoolean());

		}
	}

	@Test
	public void testWrite2() { // needs 1 tolerance http://stackoverflow.com/questions/23707736/
		File png = TestSupport.absFile("colormodels/04pt.png");
		testWrite1(png, BufferedImage.TYPE_4BYTE_ABGR, false, 1);
	}

	@Test
	public void testWrite3() { // needs
		File png = TestSupport.absFile("colormodels/08ax.png");
		testWrite1(png, BufferedImage.TYPE_4BYTE_ABGR, false, 5);
	}

	@Test
	public void testWriteLinePerLine() {
		File f = TestSupport.absFile("colormodels/08.png");
		File dest = TestSupport.absFile("test/__test.tmp.png");
		delOnExit(dest);
		BufferedImage bi1 = ImageIoUtils.readPng(f);
		PngWriterBI pngw = PngWriterBI.createInstance(bi1, dest);
		// you'll rarely want to do this
		for (int r = 0; r < pngw.imgInfo.rows; r++) {
			pngw.writeRow(pngw.getIlineset().getImageLine(r));
		}
		pngw.end(); // Dont forget this.
		TestSupport.testSameValues(f, dest);
	}

	@Test
	public void testWritePartial() {
		File f = TestSupport.absFile("colormodels/08.png");
		File dest = TestSupport.absFile("test/__test.tmp.png");
		delOnExit(dest);
		File dest2 = TestSupport.absFile("test/__test.tmp.png");
		delOnExit(dest2);
		int nlines = 10, offset = 1, step = 2;
		BufferedImage bi1 = ImageIoUtils.readPng(f);
		PngWriterBI pngw = PngWriterBI.createInstance(bi1, nlines, offset, step, dest);
		pngw.writeAll();
		pngw.end(); // not necessary here
		TestSupport.copyPartial(f, dest2, nlines, step, offset, false);
		TestSupport.testSameValues(dest2, dest);
	}

	@After
	public void endTest() {
		if (removeTmpFiles)
			for (File f : tmpFilesToDelete) {
				f.delete();
			}

	}

}
