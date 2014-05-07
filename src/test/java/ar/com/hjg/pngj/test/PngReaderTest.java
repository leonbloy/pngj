package ar.com.hjg.pngj.test;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import ar.com.hjg.pngj.IImageLineArray;
import ar.com.hjg.pngj.IImageLineSet;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;

public class PngReaderTest extends PngjTest {

  StringBuilder sb = new StringBuilder();

  @Test
  public void testRead1() {
    PngReader pngr = new PngReader(TestSupport.istream(TestSupport.PNG_TEST_TESTG2));
    pngr.getChunkseq().setIncludeNonBufferedChunks(true);
    for (int i = 0; i < pngr.imgInfo.rows; i++) {
      ImageLineInt line = (ImageLineInt) pngr.readRow(i);
      sb.append("r=" + i).append(TestSupport.showLine(line)).append(" ");
    }
    TestCase.assertEquals("r=0[  0   1   2] r=1[112 192 105] r=2[255 238 220] ", sb.toString());
    TestCase.assertTrue(pngr.getChunkseq().getIdatSet().isDone());
    pngr.end();
    TestCase.assertEquals(181, pngr.getChunkseq().getBytesCount());
    String chunks = TestSupport.showChunks(pngr.getChunksList().getChunks());
    // PngHelperInternal.LOGGER.info("chunks: " + chunks);
    TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[59] IDAT[3] IDAT[17] IEND[0] ", chunks);
  }

  @Test
  public void testRead2() { // reads only one line
    PngReader pngr = new PngReader(TestSupport.istream(TestSupport.PNG_TEST_TESTG2));
    pngr.getChunkseq().setIncludeNonBufferedChunks(true);
    IImageLineSet<ImageLineInt> lines = (IImageLineSet<ImageLineInt>) pngr.readRows(1, 1, 1);
    sb.append(TestSupport.showLine((IImageLineArray) lines.getImageLine(1)));
    TestCase.assertEquals("[112 192 105]", sb.toString());
    pngr.end();
    TestCase.assertEquals(181, pngr.getChunkseq().getBytesCount());
    String chunks = TestSupport.showChunks(pngr.getChunksList().getChunks());
    // PngHelperInternal.LOGGER.info("chunks: " + chunks);
    TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[59] IDAT[3] IDAT[17] IEND[0] ", chunks);
  }

  @Test
  public void testReadInt() {
    PngReader pngr = new PngReader(TestSupport.istream(TestSupport.PNG_TEST_TESTG2I));
    pngr.getChunkseq().setIncludeNonBufferedChunks(true);
    for (int i = 0; i < pngr.imgInfo.rows; i++) {
      ImageLineInt line = (ImageLineInt) pngr.readRow(i);
      sb.append("r=" + i).append(TestSupport.showLine(line)).append(" ");
    }
    TestCase.assertEquals("r=0[  0   1   2] r=1[112 192 105] r=2[255 238 220] ", sb.toString());
    TestCase.assertTrue(pngr.getChunkseq().getIdatSet().isDone());
    pngr.end();
    TestCase.assertEquals(183, pngr.getChunkseq().getBytesCount());
    TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[70] IDAT[23] IEND[0] ",
        TestSupport.showChunks(pngr.getChunksList().getChunks()));

  }

  @Test
  public void testReadInt2() { // reads only one line
    PngReader pngr = new PngReader(TestSupport.istream(TestSupport.PNG_TEST_TESTG2I));
    pngr.getChunkseq().setIncludeNonBufferedChunks(true);
    IImageLineSet<ImageLineInt> lines = (IImageLineSet<ImageLineInt>) pngr.readRows(1, 1, 1);
    sb.append(TestSupport.showLine((IImageLineArray) lines.getImageLine(1)));
    TestCase.assertEquals("[112 192 105]", sb.toString());
    pngr.end();
    TestCase.assertEquals(183, pngr.getChunkseq().getBytesCount());
    String chunks = TestSupport.showChunks(pngr.getChunksList().getChunks());
    // PngHelperInternal.LOGGER.info("chunks: " + chunks);
    TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[70] IDAT[23] IEND[0] ", chunks);
  }

  @Before
  public void setUp() {
    sb.setLength(0);
  }

}
