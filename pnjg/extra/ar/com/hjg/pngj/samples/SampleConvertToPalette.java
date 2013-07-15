package ar.com.hjg.pngj.samples;

import java.io.File;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

/**
 * Example: convert a RGB8/RGBA8 image to palette using Kohonen quantizer
 * Supports Alpha Does not support dithering yet
 */
public class SampleConvertToPalette {

	public static void convertPal(File origFilename, File destFilename) {
		if (origFilename.equals(destFilename))
			throw new RuntimeException("source same as target!");
		// first pass
		PngReader pngr = new PngReader(origFilename);
		int channels = pngr.imgInfo.channels;
		if (channels < 3 || pngr.imgInfo.bitDepth != 8)
			throw new RuntimeException("This method is for RGB8/RGBA8 images");
		NeuQuant cuant = new NeuQuant(pngr.imgInfo.cols, pngr.imgInfo.rows,
				NeuQuant.createPixelGetterFromPngReader(pngr));
		boolean useTransparency = pngr.imgInfo.alpha;
		cuant.setParReserveAlphaColor(useTransparency);
		cuant.run();
		pngr.end();
		pngr =new PngReader(origFilename);
		ImageInfo imiw = new ImageInfo(pngr.imgInfo.cols, pngr.imgInfo.rows, 8, false, false, true);
		// second pass
		PngWriter pngw = new PngWriter(destFilename, imiw, true);

		PngChunkPLTE palette = pngw.getMetadata().createPLTEChunk();
		int ncolors = cuant.getColorCount();
		palette.setNentries(ncolors);
		for (int i = 0; i < ncolors; i++) {
			int[] col = cuant.getColor(i);
			palette.setEntry(i, col[0], col[1], col[2]);
		}
		int transparentIndex = cuant.getTransparentIndex();
		if (transparentIndex >= 0) {
			PngChunkTRNS transparent = new PngChunkTRNS(imiw);
			transparent.setIndexEntryAsTransparent(transparentIndex);
			pngw.getChunksList().queue(transparent);
		}
		pngw.queueChunksBeforeIdat(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL_SAFE);
		ImageLine linew = new ImageLine(imiw);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = (ImageLine) pngr.readRow(row);
			int index;
			int[] scanline = l1.getScanline();
			for (int j = 0, k = 0; j < pngr.imgInfo.cols; j++, k += channels) {
				if (!useTransparency)
					index = cuant.lookup(scanline[k], scanline[k + 1], scanline[k + 2]);
				else
					index = cuant.lookup(scanline[k], scanline[k + 1], scanline[k + 2], scanline[k + 3]);
				linew.getScanline()[j] = index;
			}
			pngw.writeRow(linew, row);

		}
		pngw.queueChunksAfterIdat(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL_SAFE);
		pngw.end();
		pngr.end();
		System.out.println("colours: " + ncolors);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2 || args[0].equals(args[1])) {
			System.err.println("Arguments: [pngsrc] [pngdest]");
			System.exit(1);
		}
		convertPal(new File(args[0]), new File(args[1]));
		System.out.println("Done: " + args[1]);
	}
}
