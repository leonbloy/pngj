package ar.com.hjg.pngj.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import ar.com.hjg.pngj.ChunkSeqSkipping;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.PngjInputException;
import ar.com.hjg.pngj.chunks.ChunkRaw;

/**
 * Very basic reading
 */
public class ChunkSequenceSkipTest extends PngjTest {

	String stripesChunks = "IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] iTXt[30] IEND[0] ";

	/**
	 * just reads the chunks, all skipped
	 */
	@Test
	public void testReadChunksSkip() {
		ChunkSeqSkipping c = new ChunkSeqSkipping();
		TestSupport.feedFromStreamTest(c, "test/stripes.png");
		String chunksSummary = TestSupport.showChunksRaw(c.getChunks());
		Assertions.assertEquals(6785, c.getBytesCount());
		// test that the offsets are consistent with lengths
		long offset = 8;
		for (ChunkRaw cr : c.getChunks()) {
			Assertions.assertEquals(offset, cr.getOffset());
			offset += cr.len + 12;
		}
		Assertions.assertEquals(stripesChunks, chunksSummary);
	}

	/**
	 * valid png, identical wish stripes.png, but with extra trailing bytes,
	 */
	@Test
	public void testReadWithExtraTrailing() throws Exception {
		ChunkSeqSkipping c = new ChunkSeqSkipping();
		TestSupport.feedFromStreamTest(c, "test/bad_stripes_extratrailing.png", -1);
		Assertions.assertEquals(6785, c.getBytesCount());
		Assertions.assertEquals(stripesChunks, TestSupport.showChunksRaw(c.getChunks()));
	}

	/**
	 * invalid png (missing bytes)
	 */
	@Test
	public void testReadIncomplete() throws PngjException {
		PngjInputException ex = Assertions.assertThrows(PngjInputException.class, () -> {
			ChunkSeqSkipping c = new ChunkSeqSkipping();
			TestSupport.feedFromStreamTest(c, "test/bad_truncated.png");
		});
		Assertions.assertTrue(ex.getMessage().contains("premature ending"));
	}

	@Test
	public void testReadIncomplete2() throws PngjException {
		{
			ChunkSeqSkipping c = new ChunkSeqSkipping();
			InputStream is = TestSupport.istream("test/bad_truncated.png");
			c.feedFromInputStream(is);
			Assertions.assertFalse(c.isDone());
			c.close();
			Assertions.assertFalse(c.isDone());
		}
		{
			ChunkSeqSkipping c = new ChunkSeqSkipping();
			InputStream is = TestSupport.istream("test/stripes.png");
			c.feedFromInputStream(is);
			Assertions.assertTrue(c.isDone());
		}

	}

}
