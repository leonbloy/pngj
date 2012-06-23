package ar.com.hjg.pngj.lossy;

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
public class TestLossy {

	public static void reencode(String orig, String dest,  int lossy) {
		PngReader pngr = FileHelper.createPngReader(new File(orig));
		PngWriterLossy pngw = new PngWriterLossy(FileHelper.openFileForWriting(new File(dest),true), pngr.imgInfo);
		System.out.println(pngr.toString());
		pngw.setLossy(lossy);
		pngw.setFilterType(FilterType.FILTER_AVERAGE);
		pngw.setCompLevel(9);
		pngw.setLossy(50);
		pngw.getLossyHelper().setParTolerance(6);
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			pngw.writeRow(l1, row);
		}
		pngw.copyChunksLast(pngr, ChunkCopyBehaviour.COPY_ALL);
		pngw.end();
		System.out.println("Done");
	}

	public static void fromCmdLineArgs(String[] args) {
		if (args.length != 2 || args[0].equals(args[1])) {
			System.err.println("Arguments: [pngsrc] [pngdest] ");
			System.exit(1);
		}
		long t0 = System.currentTimeMillis();
		reencode(args[0], args[1], 37);
		long t1 = System.currentTimeMillis();
		System.out.println("Listo: " + (t1 - t0) + " msecs");
	}

	public static void main(String[] args) throws Exception {
		fromCmdLineArgs(args);
	}
}
