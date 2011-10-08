package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunksToWrite;
import ar.com.hjg.pngj.nosandbox.FileHelper;

/**
 * Example: cuts the red channel by two. Only for RGB
 */
public class DecreaseRed {
	public static void decreaseRed(String origFilename, String destFilename) {
		PngReader pngr = FileHelper.createPngReader(new File(origFilename));
		PngWriter pngw = FileHelper.createPngWriter(new File(destFilename), pngr.imgInfo, true);
		System.out.println(pngr.toString());
		// this can copy some metadata from reader
		pngw.copyChunksFirst(pngr, ChunksToWrite.COPY_ALL_SAFE);
		int channels = pngr.imgInfo.channels;
		if (channels < 3) throw new RuntimeException("This method is for RGB/RGBA images");
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			for (int j = 0; j < pngr.imgInfo.cols; j++)
				l1.scanline[j * channels] /= 2;
			pngw.writeRow(l1);
		}
		pngr.end();
		// just in case some new metadata has been read
		pngw.copyChunksLast(pngr, ChunksToWrite.COPY_ALL_SAFE);
		pngw.end();
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Arguments: [pngsrc] [pngdest]");
			System.exit(1);
		}
		decreaseRed(args[0], args[1]);
		System.out.println("Done");
	}
}
