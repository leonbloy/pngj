package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.PngReader;

/**
 * prints chunks list (remember that IDAT is shown as only one pseudo zero-length chunk)
 */
public class SampleShowChunks {
	public static void showChunks(String file) {
		PngReader pngr = FileHelper.createPngReader(new File(file));
		pngr.setMaxTotalBytesRead(1024 * 1024 * 1024L * 3); // 3Gb!
		pngr.readSkippingAllRows();
		System.out.println(pngr.toString());
		System.out.println(pngr.getChunksList().toStringFull());
	}

	public static void main(String[] args) throws Exception {
		showChunks(args.length > 0 ? args[0] : "/temp/buttonred.png");
	}
}
