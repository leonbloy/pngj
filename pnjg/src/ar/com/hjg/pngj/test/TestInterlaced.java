package ar.com.hjg.pngj.test;

import java.io.File;

import sun.rmi.transport.proxy.CGIHandler;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;

/**
 * Example: decreases the red channel by half, increase the green by 20.
 * 
 * Only for RGB
 */
public class TestInterlaced {

	public static void convert(String origFilename, String destFilename) {
		PngReader pngr = FileHelper.createPngReader(new File(origFilename));
		PngWriter pngw = FileHelper.createPngWriter(new File(destFilename), pngr.imgInfo, true);
		int channels = pngr.imgInfo.channels;
		System.out.println(pngr.toString());
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL_SAFE | ChunkCopyBehaviour.COPY_PALETTE);
		int[][] im = pngr.readImageInt();
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			pngw.writeRow(im[row],row);
		}
		pngw.copyChunksLast(pngr, ChunkCopyBehaviour.COPY_ALL_SAFE);
		pngw.end();
		pngr.end();
	}

	
	public static void main(String[] args) throws Exception {
		/*if (args.length != 2 || args[0].equals(args[1])) {
			System.err.println("Arguments: [pngsrc] [pngdest]");
			System.exit(1);
		}*/
		//convert(args[0], args[1]);
		convert("resources/campbelli1.png","resources/campbellni1.png");
		System.out.println("Done: ");
	}
}
