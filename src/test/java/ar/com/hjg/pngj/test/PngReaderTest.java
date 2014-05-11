package ar.com.hjg.pngj.test;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineSet;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngReaderByte;

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
    sb.append(TestSupport.showLine(lines.getImageLine(1)));
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
    sb.append(TestSupport.showLine(lines.getImageLine(1)));
    TestCase.assertEquals("[112 192 105]", sb.toString());
    pngr.end();
    TestCase.assertEquals(183, pngr.getChunkseq().getBytesCount());
    String chunks = TestSupport.showChunks(pngr.getChunksList().getChunks());
    // PngHelperInternal.LOGGER.info("chunks: " + chunks);
    TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[70] IDAT[23] IEND[0] ", chunks);
  }

  @Test
  public void testReadSkipping() { // reads only two lines
    String line3 = "";
    String line7 = "";
    String pngfile = "test/testg1.png";
    {
      PngReader pngr = new PngReader(TestSupport.istream(pngfile));
      TestCase.assertFalse(pngr.interlaced);
      IImageLineSet<? extends IImageLine> lines = pngr.readRows();
      pngr.end();
      line3 = TestSupport.showLine(lines.getImageLine(3));
      line7 = TestSupport.showLine(lines.getImageLine(7));
    }
    {
      PngReaderByte pngr = new PngReaderByte(TestSupport.istream(pngfile));
      String line3b = null, line7b = null;
      for (int i = 0; i < pngr.imgInfo.rows; i++) {
        IImageLine line = pngr.readRow(i);
        if (i == 3)
          line3b = TestSupport.showLine(line);
        if (i == 7)
          line7b = TestSupport.showLine(line);
      }
      pngr.end();
      TestCase.assertEquals(line3, line3b);
      TestCase.assertEquals(line7, line7b);

    }
    {
      PngReaderByte pngr2 = new PngReaderByte(TestSupport.istream(pngfile));
      IImageLineSet<? extends IImageLine> lines = pngr2.readRows(2, 3, 4);
      pngr2.end();
      String line3b = TestSupport.showLine(lines.getImageLine(3));
      String line7b = TestSupport.showLine(lines.getImageLine(7));
      TestCase.assertEquals(line3, line3b);
      TestCase.assertEquals(line7, line7b);
      TestCase.assertEquals(2, lines.size());
      try {
        TestSupport.showLine(lines.getImageLine(6));
        TestCase.fail("should not get here");
      } catch (Exception e) {
        TestCase.assertEquals("Invalid row number", e.getMessage());
      }
    }
    pngfile = "test/testg1i.png";
    {
      PngReader pngr = new PngReader(TestSupport.istream(pngfile));
      TestCase.assertTrue(pngr.interlaced);
      IImageLineSet<? extends IImageLine> lines = pngr.readRows();
      pngr.end();
      String line3b = TestSupport.showLine(lines.getImageLine(3));
      String line7b = TestSupport.showLine(lines.getImageLine(7));
      TestCase.assertEquals(line3, line3b);
      TestCase.assertEquals(line7, line7b);
    }
    {
      PngReaderByte pngr = new PngReaderByte(TestSupport.istream(pngfile));
      String line3b = null, line7b = null;
      for (int i = 0; i < pngr.imgInfo.rows; i++) {
        IImageLine line = pngr.readRow(i);
        if (i == 3)
          line3b = TestSupport.showLine(line);
        if (i == 7)
          line7b = TestSupport.showLine(line);
      }
      pngr.end();
      TestCase.assertEquals(line3, line3b);
      TestCase.assertEquals(line7, line7b);

    }
    {
      PngReaderByte pngr2 = new PngReaderByte(TestSupport.istream(pngfile));
      IImageLineSet<? extends IImageLine> lines = pngr2.readRows(2, 3, 4);
      pngr2.end();
      String line3b = TestSupport.showLine(lines.getImageLine(3));
      String line7b = TestSupport.showLine(lines.getImageLine(7));
      TestCase.assertEquals(line3, line3b);
      TestCase.assertEquals(line7, line7b);
      TestCase.assertEquals(2, lines.size());
      try {
        TestSupport.showLine(lines.getImageLine(6));
        TestCase.fail("should not get here");
      } catch (Exception e) {
        TestCase.assertEquals("Invalid row number", e.getMessage());
      }
    }
  }

  @Before
  public void setUp() {
    sb.setLength(0);
  }

}
