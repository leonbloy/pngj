package ar.com.hjg.pngj.test;

import java.io.File;

import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkFactory;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkUNKNOWN;
import ar.com.hjg.pngj.samples.SampleCustomChunk.PngChunkPROP;

/**
 * Tests (and shows) how to use a custom (user defined) chunk
 */
public class CustomChunkTest {

	private File origFile() {
		return new File(TestSupport.getResourcesDir(), "test/testg2.png");
	}

	private File newFile() {
		return new File(TestSupport.getTempDir(), "testg2wcc.png");
	}

	@Test
	public void testCustomChunk() {
		addCustomChunk();
		readWithouthFactory();
		readWithFactory();
	}

	private void addCustomChunk() {
		PngReader pngr = new PngReader(origFile());
		PngWriter pngw = new PngWriter(newFile(), pngr.imgInfo);
		pngw.copyChunksFrom(pngr.getChunksList());
		PngChunkPROP c = new PngChunkPROP(pngw.imgInfo);
		c.getProps().setProperty("mykey1", "myval1");
		c.getProps().setProperty("mykey2", "myval2");
		pngw.getChunksList().queue(c);
		for (int i = 0; i < pngr.imgInfo.rows; i++) {
			pngw.writeRow(pngr.readRow());
		}
		pngr.end();
		pngw.end();
	}
	private void readWithouthFactory() {
		PngReader pngr = new PngReader(newFile());
		pngr.readSkippingAllRows();
		pngr.end();
		String chunks=TestSupport.showChunks(pngr.getChunksList().getChunks());
		// no sense in testing this, the length of serialized via storeToXML can vary with version
		//TestCase.assertEquals("IHDR[13] prOp[256] pHYs[9] tEXt[59] IEND[0] ", chunks);
		TestCase.assertEquals("Second chunk should be  prOp","prOp",pngr.getChunksList().getChunks().get(1).getRaw().id);
		TestCase.assertEquals("Next chunk should be  pHYs","pHYs",pngr.getChunksList().getChunks().get(2).getRaw().id);
		PngChunk chunk=pngr.getChunksList().getById1(PngChunkPROP.ID);
		TestCase.assertTrue(chunk instanceof PngChunkUNKNOWN);
	}

	private void readWithFactory() {
		PngReader pngr = new PngReader(newFile());
		pngr.getChunkseq().setChunkFactory(new ChunkFactory() {
			@Override
			protected PngChunk createEmptyChunkExtended(String id, ImageInfo imgInfo) {
				if (id.equals(PngChunkPROP.ID))
					return new PngChunkPROP(imgInfo);
				return super.createEmptyChunkExtended(id, imgInfo);
			}
		});
		pngr.readSkippingAllRows();
		pngr.end();
		String chunks=TestSupport.showChunks(pngr.getChunksList().getChunks());
		// no sense in testing this, the length of serialized via storeToXML can vary with version
		//TestCase.assertEquals("IHDR[13] prOp[256] pHYs[9] tEXt[59] IEND[0] ", chunks);
		TestCase.assertEquals("Second chunk should be  prOp","prOp",pngr.getChunksList().getChunks().get(1).getRaw().id);
		TestCase.assertEquals("Next chunk should be  pHYs","pHYs",pngr.getChunksList().getChunks().get(2).getRaw().id);
		PngChunk chunk=pngr.getChunksList().getById1(PngChunkPROP.ID);
		TestCase.assertTrue(chunk instanceof PngChunkPROP);
		String val2=((PngChunkPROP)chunk).getProps().getProperty("mykey2");
		TestCase.assertEquals("myval2", val2);
	}


}
