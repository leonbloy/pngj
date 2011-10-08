package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngFilterType;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunksToWrite;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.nosandbox.FileHelper;

/**
 * reencodes a png image with a given filter and compression level
 */
public class Png8to4 {
	
	public static void png8to4(String orig, String dest) {
		PngReader png1 = FileHelper.createPngReader(new File(orig));
		ImageInfo pnginfo1 = png1.imgInfo;
		System.out.println(pnginfo1);
		ImageInfo pnginfo2 = new ImageInfo(pnginfo1.cols, pnginfo1.rows, 4, false,false,true);  
		PngWriter png2 = FileHelper.createPngWriter(new File(dest), pnginfo2, false);
		/*PngChunkPLTE paletteorig = (PngChunkPLTE) png1.chunks.getChunk(png1.chunks.firstOcurrence(ChunkHelper.PLTE_TEXT));
		PngChunkPLTE palettenew = new PngChunkPLTE(pnginfo2);
		int[] rgb=new int[]{3};
		for(int pi =0;pi<16;pi++) {
			paletteorig.getEntryRgb(pi, rgb);
		}*/
		png2.copyChunksFirst(png1, ChunksToWrite.COPY_ALL);
		ImageLine l2 = new ImageLine(pnginfo2);
		for (int row = 0; row < pnginfo1.rows; row++) {
			ImageLine l1 = png1.readRow(row);
			l2.tf_pack(l1.scanline, false);
			l2.setRown(row);
			png2.writeRow(l2);
		}
		png1.end();
		png2.copyChunksLast(png1, ChunksToWrite.COPY_ALL);
		png2.end();
		System.out.println("Done");
	}

	public static void png4to8(String orig, String dest) {
		PngReader png1 = FileHelper.createPngReader(new File(orig));
		ImageInfo pnginfo1 = png1.imgInfo;
		ImageInfo pnginfo2 = new ImageInfo(pnginfo1.cols, pnginfo1.rows, 8, false,false,true);  
		PngWriter png2 = FileHelper.createPngWriter(new File(dest), pnginfo2, false);
		png2.copyChunksFirst(png1, ChunksToWrite.COPY_ALL);
		ImageLine l2 = new ImageLine(pnginfo2);
		for (int row = 0; row < pnginfo1.rows; row++) {
			ImageLine l1 = png1.readRow(row);
			l1.tf_unpack(l2.scanline, false);
			l2.setRown(row);
			png2.writeRow(l2);
		}
		png1.end();
		png2.copyChunksLast(png1, ChunksToWrite.COPY_ALL);
		png2.end();
		System.out.println("Done");
	}

	
	public static void main(String[] args) throws Exception {
		//png4to8("C:\\temp\\x4.png", "C:\\temp\\x8.png");
		png8to4("C:\\temp\\x8.png", "C:\\temp\\x4b.png");
	}
}
