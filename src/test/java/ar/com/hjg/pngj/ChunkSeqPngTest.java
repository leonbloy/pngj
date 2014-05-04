package ar.com.hjg.pngj;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkTIME;
import ar.com.hjg.pngj.test.PngjTest;
import ar.com.hjg.pngj.test.TestSupport;

/**
 *   
 */
public class ChunkSeqPngTest extends PngjTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  public static class ChunkSeqPngCb extends ChunkSeqReaderPng {
    StringBuilder summary = new StringBuilder();

    public ChunkSeqPngCb() {
      super(true); // CB
    }

    @Override
    protected DeflatedChunksSet createIdatSet(String id) {
      IdatSet ids = new IdatSet(id, imageInfo, deinterlacer) {
        @Override
        protected int processRowCallback() {
          summary.append(TestSupport.showRow(getUnfilteredRow(), getRowFilled(), getRown()));
          return super.processRowCallback();
        }

        @Override
        protected void processDoneCallback() {}

      };
      ids.setCallbackMode(callbackMode);
      return ids;
    }

  }

  String stripesChunks =
      "IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] iTXt[30] IEND[0] ";

  /**
   * just reads the chunks
   */
  @Test
  public void testReadCallback1() {
    ChunkSeqPngCb c = new ChunkSeqPngCb();
    c.setIncludeNonBufferedChunks(true);
    TestSupport.feedFromStreamTest(c, TestSupport.PNG_TEST_STRIPES);
    String chunksSummary = TestSupport.showChunks(c.getChunks());
    TestCase.assertEquals(6785, c.getBytesCount());
    TestCase.assertEquals(
        "IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] iTXt[30] IEND[0] ",
        chunksSummary);
    List<PngChunk> timechunks = TestSupport.getChunkById(PngChunkTIME.ID, c.getChunks());
    TestCase.assertEquals("there should be one  time chunk", 1, timechunks.size());
    PngChunkTIME t = (PngChunkTIME) timechunks.get(0);
    TestCase.assertEquals("[2013, 6, 8, 19, 56, 57]", Arrays.toString(t.getYMDHMS()));

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
    TestCase.assertEquals(6785, c.getBytesCount());
    TestCase.assertEquals(
        "IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] iTXt[30] IEND[0] ",
        chunksSummary);
    List<PngChunk> timechunks = TestSupport.getChunkById(PngChunkTIME.ID, c.getChunks());
    TestCase.assertEquals("there should be one (empty) time chunk", 1, timechunks.size());
    PngChunkTIME t = (PngChunkTIME) timechunks.get(0);
    TestCase.assertNull("empty time chunk", t.getRaw().data);

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
    TestCase.assertEquals(6785, c.getBytesCount());
    TestCase.assertEquals("IHDR[13] pHYs[9] iTXt[30] IEND[0] ", chunksSummary);
    List<PngChunk> timechunks = TestSupport.getChunkById(PngChunkTIME.ID, c.getChunks());
    TestCase.assertEquals("there should no time chunk", 0, timechunks.size());
  }

  @Test
  public void testReadCallback2() {
    ChunkSeqPngCb c = new ChunkSeqPngCb();
    c.setIncludeNonBufferedChunks(true);
    TestSupport.feedFromStreamTest(c, TestSupport.PNG_TEST_TESTG2);
    TestCase.assertEquals("r=0[  1|  0   1   2]r=1[  3|112 192 105]r=2[  1|255 238 220]",
        c.summary.toString());
  }

  private static String readRowsPoll(ChunkSeqReaderPng c, String file) {
    StringBuilder sb = new StringBuilder();
    BufferedStreamFeeder bf =
        new BufferedStreamFeeder(TestSupport.istream(file), TestSupport.randBufSize());
    bf.setFailIfNoFeed(true);
    while (c.firstChunksNotYetRead()) {
      bf.feed(c);
    }
    byte[][] im = new byte[c.getImageInfo().rows][c.getImageInfo().cols];
    RowInfo rowInfo = c.getIdatSet().rowinfo;
    boolean morerows = true;
    while (morerows) {
      while (!c.getIdatSet().isRowReady())
        bf.feed(c);
      int nbytes = c.getIdatSet().getRowFilled();
      // System.out.println("nbytes: "+nbytes + " row:"+rowInfo.rowNseq);
      if (nbytes != rowInfo.colsSubImg + 1)
        throw new PngjInputException("bad bytes count");
      sb.append(TestSupport.showRow(c.getIdatSet().getUnfilteredRow(), nbytes, rowInfo.rowNseq))
          .append(" ");
      c.getIdatSet().advanceToNextRow();
      morerows = !c.getIdatSet().isDone();
    }
    while (!c.isDone()) {
      bf.feed(c);
    }
    return sb.toString();
  }

  @Test
  public void testReadPoll1() {
    ChunkSeqReaderPng c = new ChunkSeqReaderPng(false);
    c.setIncludeNonBufferedChunks(true);
    String res = readRowsPoll(c, TestSupport.PNG_TEST_TESTG2);
    TestCase.assertEquals("r=0[  1|  0   1   2] r=1[  3|112 192 105] r=2[  1|255 238 220] ", res);
    TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[59] IDAT[3] IDAT[17] IEND[0] ",
        TestSupport.showChunks(c.getChunks()));
    TestCase.assertEquals(181, c.getBytesCount());
  }

  @Test
  public void testReadPollInt1() {// (interlaced
    ChunkSeqReaderPng c = new ChunkSeqReaderPng(false);
    c.setIncludeNonBufferedChunks(true);
    String res = readRowsPoll(c, TestSupport.PNG_TEST_TESTG2I);
    TestCase
        .assertEquals(
            "r=0[  0|  0] r=1[  0|  2] r=2[  1|255 220] r=3[  0|  1] r=4[  0|238] r=5[  3|112 192 105] ",
            res);
    TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[70] IDAT[23] IEND[0] ",
        TestSupport.showChunks(c.getChunks()));
    TestCase.assertEquals(183, c.getBytesCount());
  }
}
