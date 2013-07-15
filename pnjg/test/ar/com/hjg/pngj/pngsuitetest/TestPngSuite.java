package ar.com.hjg.pngj.pngsuitetest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLines;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;
import ar.com.hjg.pngj.test.TestSupport;

/**
 * To test all images in PNG test suite doing a horizontal mirror on all them
 * 
 * Instructions: Original images from PNG test suite is supposed to be in local
 * dir resources/testsuite1/ (images supposed to fail, because are erroneous,
 * must start with 'x') Output dir is hardcoded in static "outdir" field - it
 * should be empty After running main, no error should be thrown Errors: 0/141
 * Result images are mirrored, with a 'z' appended to their names, and the
 * originals are laso copied. Suggestion: sort by name, and watch them in
 * sequence
 * 
 */
public class TestPngSuite {

	Random rand = new Random();

	private boolean clearTempFiles = true; // change to false so that the images are not removed, and you can check them visually 
	private int imagesToCheck = -1; // -1 to check all

	/**
	 * Takes a image, mirrors it using row-per-row int reading, mirror it again
	 * using byte (if possible) and compares
	 * 
	 * IF the original was interlaced, compares with origni
	 * */
	public void testmirror(File orig, File origni, File truecolor) {
		File mirror = TestSupport.addSuffixToName(orig, "_mirror");
		File recov = TestSupport.addSuffixToName(orig, "_recov");
		long crc0 = 0;
		boolean interlaced;
		boolean palete;
		{
			PngReader pngr = new PngReader(orig);
			PngWriter pngw = null;
			try {
				if (pngr.imgInfo.bitDepth < 16 && rand.nextBoolean())
					pngr.setImageLineFactory(ImageLineByte.getFactory(pngr.imgInfo));
				palete = pngr.imgInfo.indexed;
				PngHelperInternal.initCrcForTests(pngr);
				interlaced = pngr.isInterlaced();
				pngw = new PngWriter(mirror, pngr.imgInfo, true);
				pngw.setFilterType(FilterType.FILTER_CYCLIC); // just to test all filters
				pngw.copyChunksFrom(pngr.getChunksList());
				ImageLines lines = pngr.readRows();
				for (int row = 0; row < pngr.imgInfo.rows; row++) {
					mirrorLine(lines.getImageLine(row), pngr.imgInfo);
					pngw.writeRow(lines.getImageLine(row), row);
				}
				pngr.end();
				crc0 = PngHelperInternal.getCrctestVal(pngr);
				pngw.end();
			} finally {
				pngr.close();
				if (pngw != null)
					pngw.close();
			}
		}
		// mirror again (now line by line)
		{
			PngReader pngr2 = new PngReader(mirror);
			PngWriter pngw = null;
			try {
				if (pngr2.imgInfo.bitDepth < 16 && rand.nextBoolean())
					pngr2.setImageLineFactory(ImageLineByte.getFactory(pngr2.imgInfo));
				pngw = new PngWriter(recov, pngr2.imgInfo, true);
				pngw.setFilterType(FilterType.FILTER_AGGRESSIVE);
				pngw.copyChunksFrom(pngr2.getChunksList());
				for (int row = 0; row < pngr2.imgInfo.rows; row++) {
					IImageLine line = pngr2.readRow();
					mirrorLine(line, pngr2.imgInfo);
					pngw.writeRow(line, row);
				}
				pngr2.end();
				pngw.end();
			} finally {
				pngr2.close();
				if (pngw != null)
					pngw.close();
			}
		}
		// now check
		if (!interlaced)
			TestSupport.testCrcEquals(recov, crc0);
		else
			TestSupport.testSameCrc(recov, origni);

		if (interlaced)
			additionalTestInterlaced(orig, origni);

		if (palete && truecolor.exists())
			additionalTestPalette(orig, truecolor);

	}

	private static void additionalTestPalette(File orig, File truecolor) {
		// covnert to true color 8 bits and check equality
		PngReader pngr = new PngReader(orig);
		PngWriter pngw = null;
		File copy = TestSupport.addSuffixToName(orig, "_tccopy");
		try {
			PngChunkPLTE plte = pngr.getMetadata().getPLTE();
			PngChunkTRNS trns = pngr.getMetadata().getTRNS();
			boolean alpha = trns != null;
			ImageInfo im2 = new ImageInfo(pngr.imgInfo.cols, pngr.imgInfo.rows, 8, alpha);
			pngw = new PngWriter(copy, im2, true);
			pngw.copyChunksFrom(pngr.getChunksList(),ChunkCopyBehaviour.COPY_ALL_SAFE);
			int[] buf = null;
			for (int row = 0; row < pngr.imgInfo.rows; row++) {
				ImageLine line = (ImageLine) pngr.readRow();
				buf = ImageLineHelper.palette2rgb(line, plte, trns, buf);
				pngw.writeRowInt(buf);
			}
			pngr.end();
			pngw.end();
			TestSupport.testSameCrc(copy, truecolor);
		} finally {
			pngr.close();
			if (pngw != null)
				pngw.close();
		}
		copy.delete();

	}

	private void additionalTestInterlaced(File orig, File origni) {
		// tests also read/write in packed format
		File copy = TestSupport.addSuffixToName(orig, "_icopy");
		PngReader pngr = new PngReader(orig);
		PngWriter pngw = new PngWriter(copy, pngr.imgInfo, true);
		try {
			pngw.copyChunksFrom(pngr.getChunksList());
			boolean useByte = rand.nextBoolean() && pngr.imgInfo.bitDepth < 16;
			if (useByte)
				pngr.setImageLineFactory(ImageLineByte.getFactory(pngr.imgInfo));
			for (int row = 0; row < pngr.imgInfo.rows; row++) {
				IImageLine line = pngr.readRow();
				pngw.writeRow(line);
			}
			pngr.end();
			pngw.end();
			TestSupport.testSameCrc(copy, origni);
		} finally {
			pngr.close();
			if (pngw != null)
				pngw.close();
		}
		copy.delete();
	}

	public static void mirrorLine(IImageLine imline, ImageInfo iminfo) { // unpacked line
		int channels = iminfo.channels;
		int[] imlinei = null;
		byte[] imlineb = null;
		if (imline instanceof ImageLine) { // INT
			imlinei = ((ImageLine) imline).getScanline();
		} else if (imline instanceof ImageLineByte) { // BYTE
			imlineb = ((ImageLineByte) imline).getScanline();
		}
		for (int c1 = 0, c2 = iminfo.cols - 1; c1 < c2; c1++, c2--) {
			for (int i = 0; i < channels; i++) {
				int s1 = c1 * channels + i; // sample left
				int s2 = c2 * channels + i; // sample right
				if (imlinei != null) { // INT
					int aux = imlinei[s1];
					imlinei[s1] = imlinei[s2];
					imlinei[s2] = aux;
				} else {
					byte aux = imlineb[s1];
					imlineb[s1] = imlineb[s2];
					imlineb[s2] = aux;
				}
			}
		}
	}

	/** return number of unexpected errors */
	public int testAllSuite(File dirsrc, File dirdest, int maxfiles) {
		if (!dirdest.isDirectory())
			throw new RuntimeException(dirdest + " not a directory");
		int cont = 0;
		int conterr = 0;
		for (File im1 : dirsrc.listFiles()) {
			String name = im1.getName();
			if (maxfiles > 0 && cont >= maxfiles)
				break;
			if (!im1.isFile())
				continue;
			if (!name.endsWith(".png"))
				continue;
			if (name.contains("_ni.png") || name.contains("_tc.png"))
				continue; // non-interlaced version of interlaced or true color version
			try {
				File orig = new File(dirdest, name);
				copyFile(im1, orig);
				cont++;
				testmirror(orig, TestSupport.addSuffixToName(im1, "_ni"), TestSupport.addSuffixToName(im1, "_tc"));
				if (name.startsWith("x")) {
					System.err.println("this should have failed! " + name);
					conterr++;
				}
			} catch (Exception e) {
				if (name.startsWith("x")) { // suppposed to fail
					System.out.println("ok error with " + name + " " + e.getMessage());
				} else { // real error
					System.err.println("error with " + name + " " + e.getMessage());
					conterr++;
					throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
				}
			} finally {
			}
		}
		System.out.println("Errors: " + conterr + "/" + cont + " images");
		if (conterr == 0)
			System.out.println("=========== SUCCESS ! ================");
		else
			System.out.println("---- THERE WERE ERRORS!  :-((( ");
		return conterr;
	}

	private static void copyFile(File sourceFile, File destFile) {
		try {
			if (!destFile.exists()) {
				destFile.createNewFile();
			}
			FileChannel source = null;
			FileChannel destination = null;
			try {
				source = new FileInputStream(sourceFile).getChannel();
				destination = new FileOutputStream(destFile).getChannel();
				destination.transferFrom(source, 0, source.size());
			} finally {
				if (source != null) {
					source.close();
				}
				if (destination != null) {
					destination.close();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAll() {
		long t0 = System.currentTimeMillis();
		File dir = TestSupport.getPngTestSuiteDir();
		File outdir = TestSupport.getTempDir();
		System.out.println("Lines starting with 'ok error' are expected errors, they are ok.");
		if(clearTempFiles)
			System.out.print("Output files removed, to see them set clearTempFiles=false. ");
		System.out.println("Output dir: " + outdir);
		int err = testAllSuite(dir, outdir, imagesToCheck);
		TestCase.assertEquals("The suite returner " + err + " unexpected errors", 0, err);
		long t1 = System.currentTimeMillis();
		
		System.out.println("Time: " +(t1-t0) + " msecs");
		
	}

	@Before
	public void setUp() {

	}

	/**
	 * Tears down the test fixture. (Called after every test case method.)
	 */
	@After
	public void tearDown() {
		if (clearTempFiles) {
			TestSupport.cleanAll();
		}
	}

	public static void main(String[] args) throws Exception {

	}
}
