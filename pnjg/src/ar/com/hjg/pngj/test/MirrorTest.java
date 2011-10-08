package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngFilterType;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunksToWrite;
import ar.com.hjg.pngj.nosandbox.FileHelper;

/**
 * To test all images in PNG test suite (except interlaced) doing a horizontal
 * mirror on all them
 */
public class MirrorTest {
	private static boolean showInfo = false;

	public static void reencode(File orig, File dest) throws Exception {
		PngReader pngr = FileHelper.createPngReader(orig);
		if (showInfo)
			System.out.println(pngr.toString());
		// at this point we have loaded al chucks before IDAT
		//PngWriter pngw = FileHelper.createPngWriter(dest, pngr.imgInfo, true);
		PngWriter pngw = new PngWriter(new FileOutputStream(dest), pngr.imgInfo);
		pngw.setFilterType(PngFilterType.FILTER_PAETH);
		pngw.copyChunksFirst(pngr, ChunksToWrite.COPY_ALL_SAFE | ChunksToWrite.COPY_PALETTE
				| ChunksToWrite.COPY_TRANSPARENCY);
		ImageLine lout = new ImageLine(pngw.imgInfo);
		int[] line = null;
		int cols = pngr.imgInfo.cols;
		int channels = pngr.imgInfo.channels;
		int aux;
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			line = l1.tf_unpack(line, false);
			for (int c1 = 0, c2 = cols - 1; c1 < c2; c1++, c2--) {
				for (int i = 0; i < channels; i++) {
					aux = line[c1 * channels + i];
					line[c1 * channels + i] = line[c2 * channels + i];
					line[c2 * channels + i] = aux;
				}
			}
			lout.tf_pack(line, false);
			lout.setRown(l1.getRown());
			pngw.writeRow(lout);
		}
		pngr.end();
		pngw.copyChunksLast(pngr, ChunksToWrite.COPY_ALL_SAFE
				| ChunksToWrite.COPY_TRANSPARENCY);
		pngw.end();
	}

	public static void testAll(File dirsrc, File dirdest) {
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
			File newFile=new File(dirdest, name.replace(".png", "z.png"));
			File fileCopy=new File(dirdest, name);
			try {
				cont++;
				reencode(im1, newFile);
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
				}
			} finally {
				if (name.startsWith("x")) { // suppposed to fail: remove it
					try {
						  newFile.delete();
					} catch(Exception e) {}
				} else {
					copyFile(im1,fileCopy);
				}
			}
		}
		System.out.println("Errors: " + conterr + "/" + cont);
	}

	private static void copyFile(File sourceFile, File destFile) {
		try {
		 if(!destFile.exists()) {
		  destFile.createNewFile();
		 }

		 FileChannel source = null;
		 FileChannel destination = null;
		 try {
		  source = new FileInputStream(sourceFile).getChannel();
		  destination = new FileOutputStream(destFile).getChannel();
		  destination.transferFrom(source, 0, source.size());
		 }
		 finally {
		  if(source != null) {
		   source.close();
		  }
		  if(destination != null) {
		   destination.close();
		  }
		} }
		catch(Exception e) {
			throw new RuntimeException(e);
		}	
	}
	
	public static void test1() throws Exception {
		// reencode("resources/testsuite1/basn0g01.png", "C:/temp/x.png");
		// reencode(new File("resources/testsuite1/basn0g02.png"), new
		// File("C:/temp/x2.png"));
		reencode(new File("resources/testsuite1/basn0g08.png"), new File("C:/temp/test/xxx.png"));
		System.out.println("done: ");
	}

	public static void main(String[] args) throws Exception {
		String outdir = "C:/temp/test";
		testAll(new File("resources/testsuite1/"), new File(outdir));
		System.out.println("output dir: " + outdir);
	 
	}
}
