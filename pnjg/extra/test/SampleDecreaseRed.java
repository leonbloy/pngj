package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLineHelperOld;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;

/**
 * Example: decreases the red channel by half, increase the green by 20.
 * 
 * Only for RGB
 */
public class SampleDecreaseRed {

	public static void convert(String origFilename, String destFilename) {
		PngReader pngr = FileHelper.createPngReader(new File(origFilename));
		PngWriter pngw = FileHelper.createPngWriter(new File(destFilename), pngr.imgInfo, true);
		int channels = pngr.imgInfo.channels;
		if (channels < 3)
			throw new RuntimeException("This method is for RGB/RGBA images");
		System.out.println(pngr.toString());
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL_SAFE);
		pngw.getMetadata().setText(PngChunkTextVar.KEY_Description, "Decreased red and increased green");
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			for (int j = 0; j < pngr.imgInfo.cols; j++) {
				l1.scanline[j * channels] /= 2;
				l1.scanline[j * channels + 1] = ImageLineHelperOld.clampTo_0_255(l1.scanline[j * channels + 1] + 20);
			}
			pngw.writeRow(l1, row);
		}
		pngw.copyChunksLast(pngr, ChunkCopyBehaviour.COPY_ALL_SAFE);
		pngw.end();
		pngr.end();
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2 || args[0].equals(args[1])) {
			System.err.println("Arguments: [pngsrc] [pngdest]");
			System.exit(1);
		}
		convert(args[0], args[1]);
		System.out.println("Done: " + args[1]);
	}
}
