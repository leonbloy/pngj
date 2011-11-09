package ar.com.hjg.pngj.test;

import java.io.File;
import java.util.Arrays;

import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngFilterType;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter2;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunksToWrite;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkTEXT;
import ar.com.hjg.pngj.nosandbox.FileHelper;

/**
 * reencodes a png image with a given filter and compression level
 */
public class TestLossy {


	public static void encode(String orig, int lossy, PngFilterType filterType,boolean showStat,String descr) {
		if(descr==null) descr = "";
		descr = "***hjg lossy="+ lossy + " " + filterType.toString() + " " + descr+ "***";
		long t0 = System.currentTimeMillis();
		String suffix = "lossy" + lossy;
		String dest = orig.replaceAll("\\.png$", "") + "_" + suffix + ".png";
		PngReader pngr = FileHelper.createPngReader(new File(orig));
		System.out.println(pngr.imgInfo);
		File destf = new File(dest);
		PngWriter2 pngw = new PngWriter2(FileHelper.openFileForWriting(destf, true), pngr.imgInfo, destf.getName());
		PngChunkTEXT txtChunk = (PngChunkTEXT) PngChunk.factoryFromId(ChunkHelper.tEXt_TEXT, pngw.imgInfo);
		txtChunk.setKeyVal("description",descr);
		pngw.chunks.cloneAndAdd(txtChunk, true);
		pngw.setFilterType(filterType);
		pngw.setCompLevel(9);
		pngw.setLossyness(lossy);
		pngw.copyChunksFirst(pngr, ChunksToWrite.COPY_ALL_SAFE | ChunksToWrite.COPY_PALETTE);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			pngw.writeRow(l1);
		}
		pngr.end();
		pngw.copyChunksLast(pngr, ChunksToWrite.COPY_ALL_SAFE);
		pngw.end();
		long t1 = System.currentTimeMillis();
		long size0 = (new File(orig)).length();
		long size1 = (new File(dest)).length();
		double sizerel = (size1 * 1000.0) / size0;
		System.out.printf("%s\t%d\t%.2f\n", dest, (t1 - t0), sizerel);
		if(showStat)  pngw.showStatR();
	}


		public static void main(String[] args) throws Exception {
			encode("/temp/gradbn.png",8,PngFilterType.FILTER_AVERAGE,true,"");
	}
}
