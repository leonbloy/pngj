package ar.com.hjg.pngj.lossy;

import java.io.File;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;

/**
 * Example: posterizes a image to n bits.
 */
public class Posterize {

	static int complevel = 9;
	static FilterType filter = FilterType.FILTER_NONE;

	public static void posterize(String origFilename, String destFilename, int nbits, boolean errorDiffusion) {
		PngReader pngr = FileHelper.createPngReader(new File(origFilename));
		PngWriter pngw = FileHelper.createPngWriter(new File(destFilename), pngr.imgInfo, true);

		IErrorDifussion floys = null;
		if (errorDiffusion) {
			floys = new ErrorDifussionFloydSteinberg(pngr.imgInfo, 0);
			// floys = new ErrorDifussionTrivial(pngr.imgInfo,0);
		}
		if (origFilename.equals(destFilename))
			throw new RuntimeException("files are the same!");
		pngw.setCompLevel(complevel);
		pngw.setFilterType(filter);
		System.out.println(pngr.toString());
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALMOSTALL);
		int channels = pngr.imgInfo.channels;
		if (nbits > pngr.imgInfo.bitDepth || nbits < 2)
			throw new RuntimeException("Invalid nbits ");
		if (pngr.imgInfo.indexed)
			throw new RuntimeException("This method is not apt for indexed (pallete) images");
		if (pngr.imgInfo.bitDepth < 8)
			throw new RuntimeException("This method is for 8/16 bitdepth");
		int bitstotrim = pngr.imgInfo.bitDepth - nbits;
		System.out.println("trimming " + bitstotrim + " bits");
		int err = 0;
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			for (int j = 0; j < pngr.imgInfo.cols * channels; j++) {
				if (floys != null)
					err = floys.getTotalErr(row, j);
				int orig = l1.scanline[j];
				int desired = ImageLineHelper.clampTo_0_255(orig + err);
				int newval = trimnBits(desired, bitstotrim);
				if (floys != null)
					floys.addErr(row, j, desired - newval);
				l1.scanline[j] = newval;
			}
			pngw.writeRow(l1, row);
		}
		pngr.end();
		pngw.copyChunksLast(pngr, ChunkCopyBehaviour.COPY_ALMOSTALL);
		pngw.end();
	}

	private static int trimnBits(int v, int bitstotrim) {
		if (bitstotrim == 0)
			return v;
		if (bitstotrim == 1)
			return v & 0xFFFE;
		if (bitstotrim == 2)
			return v & 0xFFFC;
		if (bitstotrim == 3)
			return v & 0xFFF8;
		v = (v >> bitstotrim);
		return (v << bitstotrim);
	}

	public static void mainFromArgs(String[] args) throws Exception {
		if (args.length != 3) {
			System.err.println("Arguments: [pngsrc] [pngdest] [nbits]");
			System.exit(1);
		}
		posterize(args[0], args[1], Integer.parseInt(args[2]), false);
		System.out.println("Done");
	}

	public static void main(String[] args) throws Exception {
		// mainFromArgs(args);
		posterize("/temp/balcony.png", "/temp/balcony5b.png", 5, false);
	}
}
