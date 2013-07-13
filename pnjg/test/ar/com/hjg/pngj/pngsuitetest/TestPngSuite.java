package ar.com.hjg.pngj.pngsuitetest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import test.TestsHelper;
import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.ImageLines;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngReaderInt;
import ar.com.hjg.pngj.PngReaderNg;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.TestSupport;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

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

	/**
	 * Takes a image, mirrors it using row-per-row int reading, mirror it again
	 * using byte (if possible) and compares
	 * 
	 * IF the original was interlaced, compares with origni
	 * */
	public static void testmirror(File orig, File origni, File truecolor) {
		File mirror = TestSupport.addSuffixToName(orig, "_mirror");
		File recov = TestSupport.addSuffixToName(orig, "_recov");
		long crc0 = 0;
		boolean interlaced;
		boolean palete;
		boolean useByteFirst = 
		{
			PngReaderInt pngr = new PngReaderInt(orig);
			palete = pngr.imgInfo.indexed;
			PngHelperInternal.initCrcForTests(pngr);
			interlaced = pngr.isInterlaced();
			PngWriter pngw = new PngWriter(mirror, pngr.imgInfo, true);
			pngw.setFilterType(FilterType.FILTER_CYCLIC); // just to test all filters
			pngr.readFirstChunks();
			pngw.copyChunksFirst(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
			pngw.setUseUnPackedMode(true);
			for (int row = 0; row < pngr.imgInfo.rows; row++) {
				ImageLine line = pngr.readRow(row);
				mirrorLine(line,pngr.imgInfo);
				pngw.writeRow(line, row);
			}
			pngr.end();
			crc0 = PngHelperInternal.getCrctestVal(pngr);
			pngw.copyChunksLast(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
			pngw.end();
		}
		// mirror again, now with BYTE (if depth<16) and loading all rows
		{
			PngReaderNg pngr2 = new PngReaderByte(mirror);
			pngr2.setUnpackedMode(true);
			PngWriter pngw = new PngWriter(recov, pngr2.imgInfo, true);
			pngw.setFilterType(FilterType.FILTER_AGGRESSIVE);
			pngr2.readFirstChunks();
			pngw.copyChunksFirst(pngr2.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
			pngw.setUseUnPackedMode(true);
			ImageLines lines = pngr2.imgInfo.bitDepth < 16 ? pngr2.readRowsByte() : pngr2.readRowsInt();
			for (int row = 0; row < pngr2.imgInfo.rows; row++) {
				ImageLine line = lines.getImageLineAtMatrixRow(row);
				mirrorLine(line);
				pngw.writeRow(line, row);
			}
			pngr2.end();
			pngw.end();
		}
		// now check
		if (!interlaced)
			TestsHelper.testCrcEquals(recov, crc0);
		else
			TestsHelper.testEqual(recov, origni);

		if (interlaced)
			additionalTestInterlaced(orig, origni);

		if (palete && truecolor.exists())
			additionalTestPalette(orig, truecolor);
	}

	private static void additionalTestPalette(File orig, File truecolor) {
		// covnert to true color 8 bits and check equality
		PngReader pngr = FileHelper.createPngReader(orig);
		PngChunkPLTE plte = pngr.getMetadata().getPLTE();
		PngChunkTRNS trns = pngr.getMetadata().getTRNS();
		File copy = TestsHelper.addSuffixToName(orig, "_tccopy");
		boolean alpha = trns != null;
		ImageInfo im2 = new ImageInfo(pngr.imgInfo.cols, pngr.imgInfo.rows, 8, alpha);
		PngWriter pngw = FileHelper.createPngWriter(copy, im2, true);
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL_SAFE);
		int[] buf = null;
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine line = pngr.readRowInt(row);
			buf = ImageLineHelper.palette2rgb(line, plte, trns, buf);
			pngw.writeRowInt(buf, row);
		}
		pngr.end();
		pngw.end();
		TestsHelper.testEqual(copy, truecolor);
		copy.delete();

	}

	private static void additionalTestInterlaced(File orig, File origni) {
		// tests also read/write in packed format
		PngReader pngr = FileHelper.createPngReader(orig);
		File copy = TestsHelper.addSuffixToName(orig, "_icopy");
		pngr.setUnpackedMode(false);
		PngWriter pngw = FileHelper.createPngWriter(copy, pngr.imgInfo, true);
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL);
		pngw.setUseUnPackedMode(false);
		boolean useByte = Math.random() > 0.5 && pngr.imgInfo.bitDepth < 16;
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			if (useByte) {
				ImageLine line = pngr.readRowByte(row);
				pngw.writeRow(line, row);
			} else {
				ImageLine line = pngr.readRowInt(row);
				pngw.writeRow(line, row);
			}
		}
		pngr.end();
		pngw.end();
		TestsHelper.testEqual(copy, origni);
		copy.delete();
	}

	public static void mirrorLine(IImageLine imline,ImageInfo iminfo) { // unpacked line
		int channels = iminfo.channels;
		int[] imlinei=null;
		byte[] imlineb=null;
		if (imline instanceof ImageLine) { // INT
			imlinei = ((ImageLine)imline).getScanline();
		}else if (imline instanceof ImageLineByte) { // BYTE
			imlineb =((ImageLineByte)imline).getScanline();
		} 
		for (int c1 = 0, c2 = iminfo.cols - 1; c1 < c2; c1++, c2--) {
			for (int i = 0; i < channels; i++) {
				int s1 = c1 * channels + i; // sample left
				int s2 = c2 * channels + i; // sample right
				if (imlinei!=null) { // INT
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

	public static void testAllSuite(File dirsrc, File dirdest, int maxfiles) {
		if (!dirdest.isDirectory())
			throw new RuntimeException(dirdest + " not a directory");
		int cont = 0;
		int conterr = 0;
		for (File im1 : dirsrc.listFiles()) {
			String name = im1.getName();
			if (maxfiles > 1 && cont >= maxfiles)
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
				testmirror(orig, TestsHelper.addSuffixToName(im1, "_ni"), TestsHelper.addSuffixToName(im1, "_tc"));
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

	public static void main(String[] args) throws Exception {
		testAllSuite(new File("resources/testsuite1/"), new File(outdir), -1);
		// testmirror(new File(outdir,"basi0g01.png"),new File("resources/testsuite1/","basi0g01_ni.png"),null);
		System.out.println("Lines starting with 'ok error' are expected errors, they are ok.");
		System.out.println("Output dir: " + outdir);

	}
}
