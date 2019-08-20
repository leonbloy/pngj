package ar.com.hjg.pngj.samples;

import java.io.File;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineSet;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;

/**
 * 
 */
public class SampleReadEvenLines {

	public static void convert(String origFilename, String destFilename) {
		PngReader pngr = new PngReader(new File(origFilename));
		ImageInfo imr = pngr.imgInfo;

		ImageInfo imw = new ImageInfo(imr.cols, imr.rows / 2, imr.bitDepth, imr.alpha, imr.greyscale, imr.indexed);
		IImageLineSet<? extends IImageLine> imlines = pngr.readRows(0, imw.rows, 2); // half of the
		// lines
		PngWriter pngw = new PngWriter(new File(destFilename), imw, true);
		pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL); // all chunks are queued
		pngw.writeRows(imlines);
		pngw.end();
		pngr.end();
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2 || args[0].equals(args[1])) {
			System.err.println("Arguments: [pngsrc] [pngdest]");
			System.exit(1);
		}
		convert(args[0], args[1]);
		System.out.println("Done. Result in " + args[1]);
	}
}
