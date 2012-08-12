package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * To test all images in PNG test suite (except interlaced) doing a horizontal mirror on all them
 * 
 * Instructions: Original images from PNG test suite is supposed to be in local dir resources/testsuite1/ (images
 * supposed to fail, because are erroneous or because are interlaced, must start with 'x') Output dir is hardcoded in
 * static "outdir" field - it should be empty After running main, no error should be thrown Errors: 0/141 Result images
 * are mirrored, with a 'z' appended to their names, and the originals are laso copied. Suggestion: sort by name, and
 * watch them in sequence
 * 
 */
public class TestPngSuite {
	static final String outdir = "C:/temp/test";

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
			File newFile = new File(dirdest, name.replace(".png", "z.png"));
			File fileCopy = new File(dirdest, name);
			try {
				cont++;
				SampleMirrorImage.mirror(im1, newFile,true);
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
						newFile.delete();
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
