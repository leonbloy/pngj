package ar.com.hjg.pngj;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkTIME;
import ar.com.hjg.pngj.test.PngjTest;
import ar.com.hjg.pngj.test.TestSupport;

/**
 * CAllback
 */
public class ChunkSeqPngTest extends PngjTest {

	public static class ChunkSeqPngCb extends ChunkSeqReaderPng {
		StringBuilder summary = new StringBuilder();

		public ChunkSeqPngCb() {
			super(true); // Call back mode
		}

		@Override
		protected DeflatedChunksSet createIdatSet(String id) {
			IdatSet ids = new IdatSet(id, callbackMode, imageInfo, deinterlacer) {
				@Override
				protected int processRowCallback() {
					summary.append(TestSupport.showRow(getUnfilteredRow(), getRowFilled(), getRown()));
					return super.processRowCallback();
				}

				@Override
				protected void processDoneCallback() {
				}

			};
			return ids;
		}

	}

	String stripesChunks = "IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] iTXt[30] IEND[0] ";

	/**
	 * just reads the chunks
	 */
	@Test
	public void testReadCallback1() {
		ChunkSeqPngCb c = new ChunkSeqPngCb();
		c.setIncludeNonBufferedChunks(true);
		TestSupport.feedFromStreamTest(c, TestSupport.PNG_TEST_STRIPES);
		String chunksSummary = TestSupport.showChunks(c.getChunks());
		Assertions.assertEquals(6785, c.getBytesCount());
		Assertions.assertEquals("IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] iTXt[30] IEND[0] ",
				chunksSummary);
		List<PngChunk> timechunks = TestSupport.getChunkById(PngChunkTIME.ID, c.getChunks());
		Assertions.assertEquals(1, timechunks.size(), "there should be one  time chunk");
		PngChunkTIME t = (PngChunkTIME) timechunks.get(0);
		Assertions.assertEquals("[2013, 6, 8, 19, 56, 57]", Arrays.toString(t.getYMDHMS()));

	}

	/**
	 * just reads the chuns skipping tIME
	 */
	@Test
	public void testReadSkip1() {
		ChunkSeqPngCb c = new ChunkSeqPngCb();
		c.setIncludeNonBufferedChunks(true);
		c.addChunkToSkip(PngChunkTIME.ID);
		TestSupport.feedFromStreamTest(c, TestSupport.PNG_TEST_STRIPES);
		String chunksSummary = TestSupport.showChunks(c.getChunks());
		Assertions.assertEquals(6785, c.getBytesCount());
		Assertions.assertEquals("IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] iTXt[30] IEND[0] ",
				chunksSummary);
		List<PngChunk> timechunks = TestSupport.getChunkById(PngChunkTIME.ID, c.getChunks());
		Assertions.assertEquals(1, timechunks.size(), "there should be one (empty) time chunk");
		PngChunkTIME t = (PngChunkTIME) timechunks.get(0);
		Assertions.assertNull(t.getRaw().data, "empty time chunk");

	}

	/**
	 * just reads the chuns skipping tIME and not loading it
	 */
	@Test
	public void testReadSkip2() {
		ChunkSeqPngCb c = new ChunkSeqPngCb();
		c.addChunkToSkip(PngChunkTIME.ID);
		c.setIncludeNonBufferedChunks(false); // <<----
		TestSupport.feedFromStreamTest(c, TestSupport.PNG_TEST_STRIPES);
		String chunksSummary = TestSupport.showChunks(c.getChunks());
		Assertions.assertEquals(6785, c.getBytesCount());
		Assertions.assertEquals("IHDR[13] pHYs[9] iTXt[30] IEND[0] ", chunksSummary);
		List<PngChunk> timechunks = TestSupport.getChunkById(PngChunkTIME.ID, c.getChunks());
		Assertions.assertEquals(0, timechunks.size(), "there should no time chunk");
	}

	@Test
	public void testReadCallback2() {
		ChunkSeqPngCb c = new ChunkSeqPngCb();
		c.setIncludeNonBufferedChunks(true);
		TestSupport.feedFromStreamTest(c, TestSupport.PNG_TEST_TESTG2);
		Assertions.assertEquals("r=0[  1|  0   1   2]r=1[  3|112 192 105]r=2[  1|255 238 220]", c.summary.toString());
	}

	private static String readRowsPoll(ChunkSeqReaderPng c, String file) {
		StringBuilder sb = new StringBuilder();
		try (BufferedStreamFeeder bf = new BufferedStreamFeeder(TestSupport.istream(file), TestSupport.randBufSize())) {
			while (c.firstChunksNotYetRead()) {
				if (bf.feed(c) < 1)
					throw new PngjInputException("Premature ending?");
			}
			// byte[][] im = new byte[c.getImageInfo().rows][c.getImageInfo().cols];
			RowInfo rowInfo = c.getIdatSet().rowinfo;
			boolean morerows = true;
			while (morerows) {
				while (!c.getIdatSet().isRowReady())
					bf.feed(c);
				int nbytes = c.getIdatSet().getRowFilled();
				// System.out.println("nbytes: "+nbytes + " row:"+rowInfo.rowNseq);
				if (nbytes != rowInfo.colsSubImg + 1)
					throw new PngjInputException("bad bytes count");
				sb.append(TestSupport.showRow(c.getIdatSet().getUnfilteredRow(), nbytes, rowInfo.rowNseq)).append(" ");
				c.getIdatSet().advanceToNextRow();
				morerows = !c.getIdatSet().isDone();
			}
			while (!c.isDone()) {
				if (bf.feed(c) < 1)
					throw new PngjInputException("Premature ending?");
			}
		}
		return sb.toString();
	}

	@Test
	public void testReadPoll1() {
		ChunkSeqReaderPng c = new ChunkSeqReaderPng(false);
		c.setIncludeNonBufferedChunks(true);
		String res = readRowsPoll(c, TestSupport.PNG_TEST_TESTG2);
		Assertions.assertEquals("r=0[  1|  0   1   2] r=1[  3|112 192 105] r=2[  1|255 238 220] ", res);
		Assertions.assertEquals("IHDR[13] pHYs[9] tEXt[59] IDAT[3] IDAT[17] IEND[0] ",
				TestSupport.showChunks(c.getChunks()));
		Assertions.assertEquals(181, c.getBytesCount());
	}

	@Test
	public void testReadPollInt1() {// (interlaced
		ChunkSeqReaderPng c = new ChunkSeqReaderPng(false);
		c.setIncludeNonBufferedChunks(true);
		String res = readRowsPoll(c, TestSupport.PNG_TEST_TESTG2I);
		Assertions.assertEquals(
				"r=0[  0|  0] r=1[  0|  2] r=2[  1|255 220] r=3[  0|  1] r=4[  0|238] r=5[  3|112 192 105] ", res);
		Assertions.assertEquals("IHDR[13] pHYs[9] tEXt[70] IDAT[23] IEND[0] ", TestSupport.showChunks(c.getChunks()));
		Assertions.assertEquals(183, c.getBytesCount());
	}

}
