package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.ImageLine.SampleType;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkPredicate;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

/**
 * To test all images in PNG test suite doing a horizontal mirror on all them
 * 
 * Instructions: Original images from PNG test suite is supposed to be in local dir resources/testsuite1/ (images
 * supposed to fail, because are erroneous, must start with 'x') Output dir is hardcoded in static "outdir" field - it
 * should be empty After running main, no error should be thrown Errors: 0/141 Result images are mirrored, with a 'z'
 * appended to their names, and the originals are laso copied. Suggestion: sort by name, and watch them in sequence
 * 
 */
public class TestPngSuite {
	static final String outdir = "C:/temp/test";

	/**
	 * Takes a image, mirrors it using row-per-row int reading, mirror it again using byte (if possible) and compares
	 * 
	 * IF the original was interlaced, compares with origni
	 * */
	public static void testmirror(File orig, File origni,File truecolor) {
		File mirror = TestHelper.addSuffixToName(orig, "_mirror");
		File recov = TestHelper.addSuffixToName(orig, "_recov");
		long crc0 = 0;
		boolean interlaced;
		boolean palete;
		{
			PngReader pngr = FileHelper.createPngReader(orig);
			palete = pngr.imgInfo.indexed;
			PngHelperInternal.initCrcForTests(pngr);
			pngr.setUnpackedMode(true);
			interlaced = pngr.isInterlaced();
			PngWriter pngw = FileHelper.createPngWriter(mirror, pngr.imgInfo, true);
			pngw.setFilterType(FilterType.FILTER_CYCLIC); // just to test all filters
			pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL);
			pngw.setUseUnPackedMode(true);
			for (int row = 0; row < pngr.imgInfo.rows; row++) {
				ImageLine line = pngr.readRowInt(row);
				mirrorLineInt(pngr.imgInfo, line.scanline);
				pngw.writeRow(line, row);
			}
			pngr.end();
			crc0 = PngHelperInternal.getCrctestVal(pngr);
			pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL);
			pngw.end();
			// mirror again, now with BYTE (if depth<16) and loading all
		}
		{
			PngReader pngr2 = FileHelper.createPngReader(mirror);
			pngr2.setUnpackedMode(true);
			PngWriter pngw = FileHelper.createPngWriter(recov, pngr2.imgInfo, true);
			pngw.setFilterType(FilterType.FILTER_AGGRESSIVE);
			pngw.copyChunksFirst(pngr2, ChunkCopyBehaviour.COPY_ALL);
			pngw.setUseUnPackedMode(true);
			if (pngr2.imgInfo.bitDepth < 16) {
				byte[][] im = pngr2.readRowsByte();
				for (int row = 0; row < pngr2.imgInfo.rows; row++) {
					mirrorLineByte(pngr2.imgInfo, im[row]);
					pngw.writeRowByte(im[row], row);
				}
			} else {
				int[][] im = pngr2.readRowsInt();
				for (int row = 0; row < pngr2.imgInfo.rows; row++) {
					mirrorLineInt(pngr2.imgInfo, im[row]);
					pngw.writeRowInt(im[row], row);
				}
			}
			pngr2.end();
			pngw.end();
		}
		// now check
		if (!interlaced)
			TestHelper.testCrcEquals(recov, crc0);
		else
			TestHelper.testEqual(recov, origni);

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
		File copy = TestHelper.addSuffixToName(orig, "_tccopy");
		boolean alpha = trns != null;
		ImageInfo im2 = new ImageInfo(pngr.imgInfo.cols, pngr.imgInfo.rows, 8, alpha);
		PngWriter pngw = FileHelper.createPngWriter(copy, im2, true);
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL_SAFE);
		int[] buf = null;
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine line = pngr.readRowInt(row);
			buf = ImageLineHelper.palette2rgb(line, plte, trns,buf);
			pngw.writeRowInt(buf, row);
		}
		pngr.end();
		pngw.end();
		TestHelper.testEqual(copy, truecolor);
		copy.delete();

	}

	private static void additionalTestInterlaced(File orig, File origni) {
		// tests also read/write in packed format
		PngReader pngr = FileHelper.createPngReader(orig);
		File copy = TestHelper.addSuffixToName(orig, "_icopy");
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
		TestHelper.testEqual(copy, origni);
		copy.delete();
	}

	public static void mirrorLineInt(ImageInfo imgInfo, int[] line) { // unpacked line
		int aux;
		int channels = imgInfo.channels;
		for (int c1 = 0, c2 = imgInfo.cols - 1; c1 < c2; c1++, c2--) {
			for (int i = 0; i < channels; i++) {
				aux = line[c1 * channels + i];
				line[c1 * channels + i] = line[c2 * channels + i];
				line[c2 * channels + i] = aux;
			}
		}
	}

	public static void mirrorLineByte(ImageInfo imgInfo, byte[] line) { // unpacked line
		byte aux;
		int channels = imgInfo.channels;
		for (int c1 = 0, c2 = imgInfo.cols - 1; c1 < c2; c1++, c2--) {
			for (int i = 0; i < channels; i++) {
				aux = line[c1 * channels + i];
				line[c1 * channels + i] = line[c2 * channels + i];
				line[c2 * channels + i] = aux;
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
			if (cont >= maxfiles)
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
				testmirror(orig, TestHelper.addSuffixToName(im1, "_ni"),TestHelper.addSuffixToName(im1, "_tc"));
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
					// throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
				}
			} finally {
			}
		}
		System.out.println("Errors: " + conterr + "/" + cont + " images");
		if(conterr==0) System.out.println("=========== SUCCESS ! ================");
		else System.out.println("---- THERE WERE ERRORS!  :-((( ");
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
		testAllSuite(new File("resources/testsuite1/"), new File(outdir), 1005);
		// testmirror(new File(outdir,"basi0g01.png"),null);
		System.out.println("Lines starting with 'ok error' are expected errors, they are ok.");
		System.out.println("Output dir: " + outdir);

	}
}
