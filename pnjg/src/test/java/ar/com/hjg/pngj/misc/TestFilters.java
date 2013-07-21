package ar.com.hjg.pngj.misc;

import java.io.File;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;

/**
 * reencodes a png image with a given filter and compression level
 */
public class TestFilters {
	public static void reencode(String orig, FilterType filterType, int cLevel) {
		long t0 = System.currentTimeMillis();
		String suffix = filterType.toString().replace("FILTER_", "").toLowerCase() + "_" + String.valueOf(cLevel);
		String dest = orig.replaceAll("\\.png$", "") + "_" + suffix + ".png";
		PngReader pngr = new PngReader (new File(orig));
		PngWriter pngw = new PngWriter(new File(dest), pngr.imgInfo, true);
		pngw.setFilterType(filterType);
		pngw.setCompLevel(cLevel);
		pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLineInt l1 = (ImageLineInt) pngr.readRow(row);
			pngw.writeRow(l1, row);
		}
		pngr.end();
		pngw.end();
		long t1 = System.currentTimeMillis();
		long size0 = (new File(orig)).length();
		long size1 = (new File(dest)).length();
		double sizerel = (size1 * 1000.0) / size0;
		System.out.printf("%s\t%d\t%.2f\n", dest, (t1 - t0), sizerel);
	}

	public static void tryAllFilters(String file, int clevel) {
		for (FilterType filter : FilterType.values()) {
			reencode(file, filter, clevel);
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Arguments: [pngsrc] [compressionlevel]");
			System.exit(1);
		}
		tryAllFilters(args[0], Integer.parseInt(args[1]));
	}
}
