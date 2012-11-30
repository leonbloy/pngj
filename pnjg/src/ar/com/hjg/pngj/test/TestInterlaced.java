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

	public static void convert(String origFilename, String destFilename, boolean usebyte, boolean useProgressive) {
		PngReader pngr = FileHelper.createPngReader(new File(origFilename));
		PngWriter pngw = FileHelper.createPngWriter(new File(destFilename), pngr.imgInfo, true);
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL_SAFE | ChunkCopyBehaviour.COPY_PALETTE);
		if (useProgressive) {
			if (usebyte) {
				byte[] im=null;
				for (int row = 0; row < pngr.imgInfo.rows; row++){
					im = pngr.readRowByte(im,row);
					pngw.writeRowByte(im, row);
				}
			
			} else {
				int[] im=null;
				for (int row = 0; row < pngr.imgInfo.rows; row++){
					im = pngr.readRowInt(im,row);
					pngw.writeRow(im, row);
				}
			}

		} else {
			if (usebyte) {
				byte[][] im = pngr.readRowsByte();
				for (int row = 0; row < pngr.imgInfo.rows; row++)
					pngw.writeRowByte(im[row], row);
			
			} else {
				int[][] im = pngr.readRowsInt();
				for (int row = 0; row < pngr.imgInfo.rows; row++)
					pngw.writeRow(im[row], row);
			}
		}
		pngw.copyChunksLast(pngr, ChunkCopyBehaviour.COPY_ALL_SAFE);
		pngw.end();
		pngr.end();
		System.out.println(origFilename + " -> " + destFilename + " \n " + pngr.imgInfo);
	}

	public static void main(String[] args) throws Exception {
		/*
		 * if (args.length != 2 || args[0].equals(args[1])) { System.err.println("Arguments: [pngsrc] [pngdest]");
		 * System.exit(1); }
		 */
		// convert(args[0], args[1]);
		long t0 = System.currentTimeMillis();
		convert("/temp/sis9.png", "/temp/sis91.png",true,true);
		t0 = System.currentTimeMillis()-t0;
		System.out.println(t0);
	}
}
