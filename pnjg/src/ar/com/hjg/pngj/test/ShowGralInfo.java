package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.nosandbox.FileHelper;

/**
 * 
 */
public class ShowGralInfo {
	/*
	 * public static void show(File file, boolean includeChunksSkel) { PngReader
	 * pngr = FileHelper.createPngReader(file);
	 * System.out.println(pngr.toString()); for (int row = 0; row <
	 * pngr.imgInfo.rows; row++) { pngr.readRow(row); } pngr.end();
	 * if(includeChunksSkel) pngr.showChunks(); }
	 */
	public static void show2(File file,boolean includeChunksSkel) {
		PngReader pngr = FileHelper.createPngReader(file);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			pngr.readRow(row);
		}
		pngr.end();
		System.out.println(pngr.toString());
		pngr.printFoundChunks();
		if (includeChunksSkel)
			System.out.println(pngr.chunks.toString());
	}

	public static void main(String[] args) throws Exception {
		/*
		 * if (args.length != 1) { System.err.println("Arguments: [pngsrc]");
		 * System.exit(1); }
		 */
		show2(new File("c:\\temp\\test\\pp0n2c16.png"), true);
		// show2(new File("c:\\temp\\test\\tbbn3p08z.png"), true);
	}

	public static void print(int x) {
		System.out.println(x);
	}
}
