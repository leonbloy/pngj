package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLine.SampleType;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkPredicate;
import ar.com.hjg.pngj.chunks.PngChunk;

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

	public static void mirror(File orig, File dest, boolean usebyte, boolean loadFullImage) throws Exception {
		PngReader pngr = FileHelper.createPngReader(orig);
		PngWriter pngw = FileHelper.createPngWriter(dest, pngr.imgInfo, true);
		pngw.setFilterType(FilterType.FILTER_CYCLIC); // just to test all filters
		pngw.setCompLevel(6);
		int copyPolicy = ChunkCopyBehaviour.COPY_ALL;
		pngw.copyChunksFirst(pngr, copyPolicy);
		ImageLine lout = new ImageLine(pngw.imgInfo, usebyte ? SampleType.BYTE : SampleType.INT);
		int cols = pngr.imgInfo.cols;
		int channels = pngr.imgInfo.channels;
		int[] linei = null;
		byte[] lineb = null;
		if (usebyte)
			lineb = new byte[cols * channels];
		else
			linei = new int[cols * channels];
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			if (usebyte) {
				byte aux;
				pngr.readRowByte(lout.scanlineb, row);
				lineb = lout.unpack(lineb, false);
				for (int c1 = 0, c2 = cols - 1; c1 < c2; c1++, c2--) {
					for (int i = 0; i < channels; i++) {
						aux = lineb[c1 * channels + i];
						lineb[c1 * channels + i] = lineb[c2 * channels + i];
						lineb[c2 * channels + i] = aux;
					}
				}
				lout.pack(lineb, false);
				pngw.writeRowByte(lout.scanlineb, row);
			} else {
				int aux;
				ImageLine l1 = pngr.readRow(row);
				linei = l1.unpack(linei, false);
				for (int c1 = 0, c2 = cols - 1; c1 < c2; c1++, c2--) {
					for (int i = 0; i < channels; i++) {
						aux = linei[c1 * channels + i];
						linei[c1 * channels + i] = linei[c2 * channels + i];
						linei[c2 * channels + i] = aux;
					}
				}
				lout.pack(linei, false);
				pngw.writeRow(lout, row);

			}
		}
		pngr.end();
		pngw.copyChunksLast(pngr, copyPolicy);
		pngw.end();
		// // print unknown chunks, just for information
		List<PngChunk> u = ChunkHelper.filterList(pngr.getChunksList().getChunks(), new ChunkPredicate() {
			public boolean match(PngChunk c) {
				return ChunkHelper.isUnknown(c);
			}
		});
		if (!u.isEmpty())
			System.out.println("Unknown chunks:" + u);
	}

	public static void testAllSuite(File dirsrc, File dirdest) {
		if (!dirdest.isDirectory())
			throw new RuntimeException(dirdest + " not a directory");
		int cont = 0;
		int conterr = 0;
		for (File im1 : dirsrc.listFiles()) {
			if (!im1.isFile())
				continue;
			String name = im1.getName();
			if (!name.endsWith(".png"))
				continue;
			File newFile1 = new File(dirdest, name.replace(".png", "z1.png"));
			File newFile2 = new File(dirdest, name.replace(".png", "z2.png"));
			File newFile3 = new File(dirdest, name.replace(".png", "z3.png"));
			File fileCopy = new File(dirdest, name);
			try {
				cont++;
				mirror(im1, newFile1, false,false);
				mirror(im1, newFile2, true,false);
				mirror(im1, newFile3, true,true);
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
				if (name.startsWith("x")) { // suppposed to fail: remove it
					try {
						newFile1.delete();
					} catch (Exception e) {
					}
				} else {
					copyFile(im1, fileCopy);
				}
			}
		}
		System.out.println("Errors: " + conterr + "/" + cont);
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
		testAllSuite(new File("resources/testsuite1/"), new File(outdir));
		System.out.println("Lines starting with 'ok error' are expected errors, they are ok.");
		System.out.println("Output dir: " + outdir);
	}
}
