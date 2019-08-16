package ar.com.hjg.pngj.samples;

import java.io.File;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

/**
 * This converts a paletted image to a True color image If the image has
 * transparency (tRNS chunk) it generates a RGBA image, elsewhere a RGB
 */
public class SampleConvPalToTrueColor {

    public static void convertToTc(File orig, File copy) {
	PngReader pngr = new PngReader(orig);
	if (!pngr.imgInfo.indexed)
	    throw new RuntimeException("Not indexed image");
	PngChunkPLTE plte = pngr.getMetadata().getPLTE();
	PngChunkTRNS trns = pngr.getMetadata().getTRNS(); // transparency metadata, can be null
	boolean alpha = trns != null;
	ImageInfo im2 = new ImageInfo(pngr.imgInfo.cols, pngr.imgInfo.rows, 8, alpha);
	PngWriter pngw = new PngWriter(copy, im2, false);
	pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL_SAFE);
	int[] buf = null;
	for (int row = 0; row < pngr.imgInfo.rows; row++) {
	    ImageLineInt line1 = (ImageLineInt) pngr.readRow(row);
	    buf = ImageLineHelper.palette2rgb(line1, plte, trns, buf);
	    ImageLineInt line2 = new ImageLineInt(pngw.imgInfo, buf);
	    pngw.writeRow(line2);
	}
	pngr.end();
	pngw.end();
    }

    public static void main(String[] args) throws Exception {
	if (args.length != 2 || args[0].equals(args[1])) {
	    System.err.println("Arguments: [pngsrc] [pngdest]");
	    System.exit(1);
	}
	convertToTc(new File(args[0]), new File(args[1]));
	System.out.println("Done: " + args[1]);
    }
}
