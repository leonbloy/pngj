package ar.com.hjg.pngj.samples;

import java.io.File;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;

/**
 * Mirrors an image, along the rows. This works for ALL image types, see also
 * TestPngSuite
 */
public class SampleMirrorImage {

    public static void mirror(File orig, File dest, boolean overwrite) {
	PngReader pngr = new PngReader(orig);
	PngWriter pngw = new PngWriter(dest, pngr.imgInfo, overwrite);
	pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL_SAFE);
	for (int row = 0; row < pngr.imgInfo.rows; row++) {
	    ImageLineInt line = (ImageLineInt) pngr.readRow(row);
	    mirrorLineInt(pngr.imgInfo, line.getScanline());
	    pngw.writeRow(line, row);
	}
	pngr.end();
	pngw.end();
    }

    private static void mirrorLineInt(ImageInfo imgInfo, int[] line) { // unpacked line
	int channels = imgInfo.channels;
	for (int c1 = 0, c2 = imgInfo.cols - 1; c1 < c2; c1++, c2--) { // swap pixels (not samples!)
	    for (int i = 0; i < channels; i++) {
		int aux = line[c1 * channels + i];
		line[c1 * channels + i] = line[c2 * channels + i];
		line[c2 * channels + i] = aux;
	    }
	}
    }

    public static void main(String[] args) throws Exception {
	if (args.length != 2 || args[0].equals(args[1])) {
	    System.err.println("Arguments: [pngsrc] [pngdest]");
	    System.exit(1);
	}
	File file1 = new File(args[0]);
	File file2 = new File(args[1]);
	mirror(file1, file2, false);
	System.out.printf("Done: %s -> %s\n", file1.toString(), file2.toString());
    }
}
