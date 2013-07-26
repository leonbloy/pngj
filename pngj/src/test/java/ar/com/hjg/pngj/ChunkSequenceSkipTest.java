package ar.com.hjg.pngj;

import junit.framework.TestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.test.PngjTest;
import ar.com.hjg.pngj.test.TestSupport;

/**
 * Very basic reading
 */
public class ChunkSequenceSkipTest extends PngjTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	String stripesChunks = "IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] iTXt[30] IEND[0] ";

	public static String getChunksSummary(String f) {
		ChunkSequenceSkip c = new ChunkSequenceSkip();
		TestSupport.feedFromStreamTest(c, f);
		return TestSupport.showChunksRaw(c.getChunks());
	}

	/**
	 * just reads the chunks, all skipped
	 */
	@Test
	public void testReadChunksSkip() {
		ChunkSequenceSkip c = new ChunkSequenceSkip();
		TestSupport.feedFromStreamTest(c, "test/stripes.png");
		String chunksSummary = TestSupport.showChunksRaw(c.getChunks());
		TestCase.assertEquals(6785, c.getBytesCount());
		//test that the offsets are consistent with lengths
		long offset = 8;
		for (ChunkRaw cr : c.getChunks()) {
			TestCase.assertEquals(offset, cr.getOffset());
			offset += cr.len + 12;
		}
		TestCase.assertEquals(stripesChunks, chunksSummary);
	}

	/**
	 * valid png, identical wish stripes.png, but with extra trailing bytes,
	 */
	@Test
	public void testReadWithExtraTrailing() throws Exception {
		ChunkSequenceSkip c = new ChunkSequenceSkip();
		TestSupport.feedFromStreamTest(c, "test/stripes_extratrailing.png", -1);
		TestCase.assertEquals(6785, c.getBytesCount());
		TestCase.assertEquals(stripesChunks, TestSupport.showChunksRaw(c.getChunks()));
	}

	/**
	 * invalid png (missing bytes)
	 */
	@Test
	public void testReadIncomplete() throws Exception {
		expectedEx.expect(PngjInputException.class);
		expectedEx.expectMessage("premature ending");
		ChunkSequenceSkip c = new ChunkSequenceSkip();
		TestSupport.feedFromStreamTest(c, "test/bad_truncated.png");
		throw new RuntimeException("should not reach here");
	}

}
