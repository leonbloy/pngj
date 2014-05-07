package ar.com.hjg.pngj;

import java.io.InputStream;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import ar.com.hjg.pngj.test.PngjTest;
import ar.com.hjg.pngj.test.TestSupport;

/**
 * Test the DeflatedChunkSet using a ChunkSetReader2 that reads the IDATs with no knowledge of filters, etc
 */
public class DeflatedChunkSetTest extends PngjTest {

  public static class ChunkSeqReaderIdatRawCb extends ChunkSeqReader { // callback
    final int rowsize;
    private int nrows;
    private int rown;
    private StringBuilder summary = new StringBuilder(); // debug/test

    public ChunkSeqReaderIdatRawCb(int rowsize, int nrows) {
      super();
      this.rowsize = rowsize;
      this.nrows = nrows;
      rown = 0;
    }

    @Override
    protected DeflatedChunksSet createIdatSet(String id) {
      DeflatedChunksSet cc = new DeflatedChunksSet(id, rowsize, rowsize) {
        @Override
        protected int processRowCallback() {
          summary.append(TestSupport.showRow(getInflatedRow(), getRowFilled(), rown)).append(" ");
          rown++;
          return rown >= nrows ? -1 : rowsize;
        }
      };
      cc.setCallbackMode(true);
      return cc;
    }

    @Override
    protected boolean isIdatKind(String id) {
      return id.equals("IDAT");
    }
  }

  public static class ChunkSeqReaderIdatRawP extends ChunkSeqReader {// normal (polled,sync)
    final int rowsize;
    private int nrows;
    private int rown;
    private boolean idatFound = false;
    private boolean idatEnded = false;
    private StringBuilder summary = new StringBuilder(); // for debug/tests

    public ChunkSeqReaderIdatRawP(int rowsize, int nrows) {
      super();
      this.rowsize = rowsize;
      this.nrows = nrows;
      rown = 0;
    }

    @Override
    protected DeflatedChunksSet createIdatSet(String id) {
      DeflatedChunksSet cc = new DeflatedChunksSet(id, rowsize, rowsize) {};
      cc.setCallbackMode(false);
      return cc;
    }

    @Override
    protected boolean isIdatKind(String id) {
      return id.equals("IDAT");
    }



    public void readFrom(InputStream is) {
      BufferedStreamFeeder bf = new BufferedStreamFeeder(is, TestSupport.randBufSize());
      while (!idatFound) {
        if (bf.feed(this) < 1)
          break;
      }
      for (rown = 0; rown < nrows; rown++) {
        while (getCurReaderDeflatedSet() != null
            && getCurReaderDeflatedSet().isWaitingForMoreInput()) {
          if (bf.feed(this) < 1)
            break;
        }
        if (getCurReaderDeflatedSet().isRowReady()) {
          summary.append(
              TestSupport.showRow(getCurReaderDeflatedSet().getInflatedRow(),
                  getCurReaderDeflatedSet().getRowFilled(), getCurReaderDeflatedSet().getRown()))
              .append(" ");
          getCurReaderDeflatedSet().prepareForNextRow(rowsize);
        } else
          break;
      }
      if (getCurReaderDeflatedSet() != null && !getCurReaderDeflatedSet().isDone())
        getCurReaderDeflatedSet().done();
      while (!isDone()) {
        if (bf.feed(this) < 1)
          break;
      }
      bf.close();
    }

    @Override
    protected void startNewChunk(int len, String id, long offset) {
      super.startNewChunk(len, id, offset);
      if (isIdatKind(id) && !idatFound)
        idatFound = true;
      if (idatFound && !isIdatKind(id))
        idatEnded = true;
    }
  }

  @Test
  public void read1CbExact() {
    ChunkSeqReaderIdatRawCb c = new ChunkSeqReaderIdatRawCb(4, 3); // "true" values
    TestSupport.feedFromStreamTest(c, "test/testg2.png");
    TestCase.assertEquals(181, c.getBytesCount());
    // warning: this is unfiltered
    TestCase.assertEquals("r=0[  1|  0   1   1] r=1[  3|112 136   8] r=2[  1|255 239 238] ",
        c.summary.toString());
  }

  @Test
  public void read1CbLessBytes() {
    ChunkSeqReaderIdatRawCb c = new ChunkSeqReaderIdatRawCb(3, 2);
    TestSupport.feedFromStreamTest(c, "test/testg2.png");
    TestCase.assertEquals(181, c.getBytesCount());
    TestCase.assertEquals("r=0[  1|  0   1] r=1[  1|  3 112] ", c.summary.toString());
  }

  @Test
  public void read1CbMoreBytes() {
    ChunkSeqReaderIdatRawCb c = new ChunkSeqReaderIdatRawCb(5, 9);
    TestSupport.feedFromStreamTest(c, "test/testg2.png");
    TestCase.assertEquals(181, c.getBytesCount());
    TestCase.assertEquals(6, c.getChunkCount());
    TestCase.assertEquals("r=0[  1|  0   1   1   3] r=1[112|136   8   1 255] r=2[239|238] ",
        c.summary.toString());
  }

  @Test
  public void read1PollExact() {
    ChunkSeqReaderIdatRawP c = new ChunkSeqReaderIdatRawP(4, 3); // "true" values
    c.readFrom(TestSupport.istream(TestSupport.PNG_TEST_TESTG2));
    TestCase.assertEquals(181, c.getBytesCount());
    // System.out.println(c.summary);
    TestCase.assertEquals("r=0[  1|  0   1   1] r=1[  3|112 136   8] r=2[  1|255 239 238] ",
        c.summary.toString());
  }

  @Test
  public void read1PollLessBytes() {
    ChunkSeqReaderIdatRawP c = new ChunkSeqReaderIdatRawP(3, 2); // we expect less byes that existent
    c.readFrom(TestSupport.istream(TestSupport.PNG_TEST_TESTG2));
    TestCase.assertEquals(181, c.getBytesCount());
    // System.out.println(c.summary);
    TestCase.assertEquals("r=0[  1|  0   1] r=1[  1|  3 112] ", c.summary.toString());
  }

  @Test
  public void read1PollMoreBytes() {
    ChunkSeqReaderIdatRawP c = new ChunkSeqReaderIdatRawP(5, 9);// we expect more byes that existent
    c.readFrom(TestSupport.istream(TestSupport.PNG_TEST_TESTG2));
    TestCase.assertEquals(181, c.getBytesCount());
    // System.out.println(c.summary);
    TestCase.assertEquals("r=0[  1|  0   1   1   3] r=1[112|136   8   1 255] r=2[239|238] ",
        c.summary.toString());
  }

  @Test(expected = PngjInputException.class)
  public void read1PollBad() { // file has missing IDAT
    ChunkSeqReaderIdatRawP c = new ChunkSeqReaderIdatRawP(81, 300); // "true" values
    c.readFrom(TestSupport.istream(TestSupport.PNG_TEST_BAD_MISSINGIDAT));
  }

  @Before
  public void setUp() {

  }

  /*
   * public static void main(String[] args) throws Exception{ byte[] data= new byte[]{0,42,43,0,44,41}; byte[] compressed = new byte[20]; Deflater def1 = new
   * Deflater(Deflater.DEFAULT_COMPRESSION); def1.setInput(data); def1.finish(); int n = def1.deflate(compressed); System.out.println(n);
   * System.out.println(Arrays.toString(compressed)); }
   */
}
