package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;

/**
 * reencodes a png image with a given filter and compression level
 */
public class SamplePngReencode {
	public static void reencode(String orig, String dest, FilterType filterType, int cLevel) {
		PngReader pngr = FileHelper.createPngReader(new File(orig));
		PngWriter pngw = FileHelper.createPngWriter(new File(dest), pngr.imgInfo, true);
		System.out.println(pngr.toString());
		System.out.printf("Creating Image %s  filter=%s compLevel=%d \n", pngw.getFilename(), filterType.toString(),
				cLevel);
		pngw.setFilterType(filterType);
		pngw.setCompLevel(cLevel);
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			pngw.writeRow(l1, row);
		}
		pngw.copyChunksLast(pngr, ChunkCopyBehaviour.COPY_ALL);
		pngr.end();
		pngw.end();
		System.out.printf("Done. Compression: %.3f \n", pngw.computeCompressionRatio());
	}

	public static void fromCmdLineArgs(String[] args) {
		if (args.length != 4 || args[0].equals(args[1])) {
			System.err.println("Arguments: [pngsrc] [pngdest] [filter] [compressionlevel]");
			System.err.println(" Where filter = 0..4  , compressionLevel = 0 .. 9");
			System.exit(1);
		}
		long t0 = System.currentTimeMillis();
		reencode(args[0], args[1], FilterType.getByVal(Integer.parseInt(args[2])), Integer.parseInt(args[3]));
		long t1 = System.currentTimeMillis();
		System.out.println("Listo: " + (t1 - t0) + " msecs");
	}

	public static void main(String[] args) throws Exception {
		fromCmdLineArgs(args);
	}
}
