package ar.com.hjg.pngj.test;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;

import junit.framework.TestCase;
import ar.com.hjg.pngj.ChunkReaderFilter;
import ar.com.hjg.pngj.chunks.PngChunk;

/** this shows how to use the Callback mode*/
public class TestPngFilterStream {
	
	@Test
	public void testBufferedImage() throws Exception{
		ChunkReaderFilter reader=new ChunkReaderFilter(TestSupport.istream("test/stripes.png"));
		BufferedImage image1 = ImageIO.read(reader);
		reader.readUntilEndAndClose();
		List<PngChunk> chunks = reader.getChunksList();
	/*	System.out.println(chunks.size());
		System.out.println(reader.getChunksList());
		System.out.println(TestSupport.showChunks(reader.getChunksList()));
		System.out.println(chunks.size());*/
		
		TestCase.assertTrue(reader.getChunkseq().isDone());
		TestCase.assertEquals("IHDR[13] pHYs[9] tIME[7] iTXt[30] IEND[0] ", TestSupport.showChunks(chunks));
		TestCase.assertEquals(reader.getChunkseq().getImageInfo().cols,image1.getWidth());
	}

}