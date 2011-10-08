package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngFilterType;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunksToWrite;
import ar.com.hjg.pngj.nosandbox.FileHelper;

/**
 * reencodes a png image with a given filter and compression level
 */
public class PngReencode {
	public static void reencode(String orig, String dest, PngFilterType filterType,
			int cLevel) {
		PngReader pngr = FileHelper.createPngReader(new File(orig));
		PngWriter pngw = FileHelper.createPngWriter(new File(dest), pngr.imgInfo, true);
		System.out.println(pngr.toString());
		pngw.setFilterType(filterType);
		pngw.setCompLevel(cLevel);
		pngw.copyChunksFirst(pngr, ChunksToWrite.COPY_ALL);
		System.out.printf("Creating Image %s  filter=%s compLevel=%d \n", pngw.getFilename(),
				filterType.toString(), cLevel);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			// pngw.writeRow(l1.vals, row);
			pngw.writeRow(l1);
		}
		pngr.end();
		pngw.copyChunksLast(pngr, ChunksToWrite.COPY_ALL);
		pngw.end();
		System.out.println("Done");
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.err.println("Arguments: [pngsrc] [pngdest] [filter] [compressionlevel]");
			System.err.println(" Where filter = 0..4  , compressionLevel = 0 .. 9");
			System.exit(1);
		}
		long t0 = System.currentTimeMillis();
		reencode(args[0], args[1], PngFilterType.getByVal(Integer.parseInt(args[2])),
				Integer.parseInt(args[3]));
		long t1 = System.currentTimeMillis();
		System.out.println("Listo: " + (t1-t0) + " msecs");
	}
}
