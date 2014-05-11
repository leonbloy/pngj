package ar.com.hjg.pngj.test;

import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineSet;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.PngReaderApng;
import ar.com.hjg.pngj.PngReaderByte;
import ar.com.hjg.pngj.chunks.PngChunkFCTL;

public class ApngTests {

  @Test
  public void notApng() {
    String line1 = null;
    String line1b = null;
    {
      PngReaderApng ap = new PngReaderApng(TestSupport.absFile("test/testg2.png"));
      TestCase.assertFalse("is not apng", ap.isApng());
      TestCase.assertTrue(ap.hasMoreRows());
      for (int i = 0; i < ap.getCurImgInfo().rows; i++) {
        ImageLineByte imline = ap.readRowByte();
        if (i == 1)
          line1 = TestSupport.showLine(imline);
      }
      TestCase.assertFalse(ap.hasMoreRows());
      ap.end();
      System.out.println(line1);
      TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[59] IEND[0] ",
          TestSupport.showChunks(ap.getChunksList().getChunks()));
    }
    // now with PngReaderByte
    {
      PngReaderByte ap0 = new PngReaderByte(TestSupport.absFile("test/testg2.png"));
      for (int i = 0; i < ap0.getCurImgInfo().rows; i++) {
        ImageLineByte imline = ap0.readRowByte();
        if (i == 1)
          line1b = TestSupport.showLine(imline);
      }
      ap0.end();
    }
    TestCase.assertEquals(line1b, line1);
  }

  @Test
  public void readAsPng() {
    PngReaderByte ap = new PngReaderByte(TestSupport.absFile("test/clock_apng.png"));
    // default: skips fdAT and fctl chunks
    while (ap.hasMoreRows()) {
      ap.readRow();
    }
    ap.end();
    TestCase.assertEquals("IHDR[13] acTL[8] PLTE[708] tRNS[17] IEND[0] ",
        TestSupport.showChunks(ap.getChunksList().getChunks()));
  }

  @Test
  public void readAsPngWithStill() {
    PngReaderByte ap = new PngReaderByte(TestSupport.absFile("test/withstill_apng.png"));
    // we restore fctl, just for trying
    ap.dontSkipChunk(PngChunkFCTL.ID);
    while (ap.hasMoreRows())
      ap.readRow();
    ap.end();
    TestCase.assertEquals("IHDR[13] acTL[8] fcTL[26] IEND[0] ",
        TestSupport.showChunks(ap.getChunksList().getChunks()));
  }


  @Test
  public void readBasicEqualSize() { // frames with equal size
    PngReaderApng ap = new PngReaderApng(TestSupport.absFile("test/loading_apng.png"));
    TestCase.assertTrue("is apng", ap.isApng());
    TestCase.assertFalse("no still image", ap.hasExtraStillImage());
    int numf = ap.getApngNumFrames();
    TestCase.assertEquals(12, numf);
    int nump = ap.getApngNumPlays();
    TestCase.assertEquals(0, nump);
    for (int frame = 0; frame < numf; frame++) {
      ap.advanceToFrame(frame);
      for (int r = 0; r < ap.getCurImgInfo().rows; r++) {
        ImageLineByte line = ap.readRowByte();
        if (frame == 1 && r == 7) {
          TestCase.assertEquals(line.getScanlineByte()[0], 0);
          TestCase.assertEquals(line.getScanlineByte()[3], 50);
          TestCase.assertEquals(line.getScanlineByte()[7], 58);
          TestCase.assertEquals(line.getFilterType(), FilterType.FILTER_PAETH);
        }
      }
    }
    ap.end();
  }

  @Test
  public void readBasicDifferentSize() {
    PngReaderApng ap = new PngReaderApng(TestSupport.absFile("test/clock_apng.png"));
    TestCase.assertTrue("is apng", ap.isApng());
    TestCase.assertFalse("no still image", ap.hasExtraStillImage());
    int numf = ap.getApngNumFrames();
    TestCase.assertEquals(40, numf);
    int nump = ap.getApngNumPlays();
    TestCase.assertEquals(0, nump);
    String row15_f12, row15_f13, row15_f14;
    {
      // loads row 15 from frames 12 23 14
      PngReaderByte png12 = new PngReaderByte(TestSupport.absFile("test/apngf_012_clock_apng.png"));
      row15_f12 = TestSupport.showLine(png12.readRow(15));
      png12.end();
      PngReaderByte png13 = new PngReaderByte(TestSupport.absFile("test/apngf_013_clock_apng.png"));
      row15_f13 = TestSupport.showLine(png13.readRow(15));
      png13.end();
      PngReaderByte png14 = new PngReaderByte(TestSupport.absFile("test/apngf_014_clock_apng.png"));
      row15_f14 = TestSupport.showLine(png14.readRow(15));
      png14.end();
    }

    for (int frame = 0; frame < numf; frame++) {
      ap.advanceToFrame(frame);
      TestCase.assertEquals(ap.getCurImgInfo().cols, ap.getFctl().getWidth());
      TestCase.assertEquals(ap.getCurImgInfo().rows, ap.getFctl().getHeight());
      if (frame == 0) { // 150 150 , 0 0 4 100 none source
        TestCase.assertEquals(150, ap.getCurImgInfo().rows);
        TestCase.assertEquals(150, ap.getCurImgInfo().cols);
        TestCase.assertEquals(0, ap.getFctl().getxOff());
        TestCase.assertEquals(0, ap.getFctl().getyOff());
        TestCase.assertEquals(PngChunkFCTL.APNG_DISPOSE_OP_NONE, ap.getFctl().getDisposeOp());
        TestCase.assertEquals(PngChunkFCTL.APNG_BLEND_OP_SOURCE, ap.getFctl().getBlendOp());
      } else if (frame == 5) { // 68x48, 51 38 4 100 none over
        TestCase.assertEquals(68, ap.getCurImgInfo().cols);
        TestCase.assertEquals(48, ap.getCurImgInfo().rows);
        TestCase.assertEquals(51, ap.getFctl().getxOff());
        TestCase.assertEquals(38, ap.getFctl().getyOff());
        TestCase.assertEquals(4, ap.getFctl().getDelayNum());
        TestCase.assertEquals(100, ap.getFctl().getDelayDen());
        TestCase.assertEquals(38, ap.getFctl().getyOff());
        TestCase.assertEquals(PngChunkFCTL.APNG_DISPOSE_OP_NONE, ap.getFctl().getDisposeOp());
        TestCase.assertEquals(PngChunkFCTL.APNG_BLEND_OP_OVER, ap.getFctl().getBlendOp());
      }
      ImageLineByte line;
      if (frame % 3 == 0) {
        for (int r = 0; r < ap.getCurImgInfo().rows; r++) {
          line = ap.readRowByte();
          if (frame == 12 && r == 15)
            TestCase.assertEquals(row15_f12, TestSupport.showLine(line));
        }
      } else if (frame % 3 == 1) {
        IImageLineSet<? extends IImageLine> lineset = ap.readRows();
        TestCase.assertEquals(ap.getCurImgInfo().rows, lineset.size());
        if (frame == 13)
          TestCase.assertEquals(row15_f13, TestSupport.showLine(lineset.getImageLine(15)));
      } else {
        IImageLineSet<? extends IImageLine> lineset =
            ap.readRows(ap.getCurImgInfo().rows / 5, 3, 4);
        TestCase.assertEquals(ap.getCurImgInfo().rows / 5, lineset.size());
        if (frame == 14)
          TestCase.assertEquals(row15_f14, TestSupport.showLine(lineset.getImageLine(15)));
      }
    }
    ap.end();
  }

  @Test
  public void readBasicDifferentSizeWrongFrame() {
    PngReaderApng ap = new PngReaderApng(TestSupport.absFile("test/clock_apng.png"));
    TestCase.assertTrue("is apng", ap.isApng());
    TestCase.assertFalse("no still image", ap.hasExtraStillImage());
    int numf = ap.getApngNumFrames();
    TestCase.assertEquals(40, numf);
    int nump = ap.getApngNumPlays();
    TestCase.assertEquals(0, nump);
    try {
      for (int frame = 0; frame <= numf; frame++) {
        ap.advanceToFrame(frame);
        for (int r = 0; r < ap.getCurImgInfo().rows; r++) {
          ImageLineByte line = ap.readRowByte();
        }
      }
      TestCase.fail("you should not get here");
    } catch (Exception e) {
      TestCase.assertEquals("Frame out of range 40", e.getMessage());
    } finally {
      ap.end();
    }
  }

  @Test
  public void readBasicDifferentSizeSkipping() {
    PngReaderApng ap = new PngReaderApng(TestSupport.absFile("test/clock_apng.png"));
    TestCase.assertTrue("is apng", ap.isApng());
    TestCase.assertFalse("no still image", ap.hasExtraStillImage());
    int numf = ap.getApngNumFrames();
    TestCase.assertEquals(40, numf);
    int nump = ap.getApngNumPlays();
    TestCase.assertEquals(0, nump);
    try {
      for (int frame = 0; frame <= numf; frame++) {
        ap.advanceToFrame(frame);
        for (int r = 0; r < ap.getCurImgInfo().rows; r++) {
          ImageLineByte line = ap.readRowByte();
        }
      }
      TestCase.fail("you should not get here");
    } catch (Exception e) {
      TestCase.assertEquals("Frame out of range 40", e.getMessage());
    } finally {
      ap.end();
    }
  }


  @Test
  public void readBasic() {
    PngReaderApng ap = new PngReaderApng(TestSupport.absFile("test/clock_apng.png"));
    TestCase.assertTrue("is apng", ap.isApng());
    TestCase.assertFalse("no still image", ap.hasExtraStillImage());
    int numf = ap.getApngNumFrames();
    TestCase.assertEquals(40, numf);
    int nump = ap.getApngNumPlays();
    TestCase.assertEquals(0, nump);
    for (int frame = 0; frame < numf; frame++) {
      ap.advanceToFrame(frame);
      System.out.printf("f=%d im=%s\n", frame, ap.getCurImgInfo().toStringBrief());
      for (int r = 0; r < ap.getCurImgInfo().rows; r++) {
        ImageLineByte line = ap.readRowByte();
      }
    }
    ap.end();
    System.out.printf("numframes: %d\n", numf);
  }

  @Test
  public void readBasicWithStill() {
    PngReaderApng ap = new PngReaderApng(TestSupport.absFile("test/withstill_apng.png"));
    TestCase.assertTrue("is apng", ap.isApng());
    TestCase.assertTrue("still image", ap.hasExtraStillImage());
    int numf = ap.getApngNumFrames();
    TestCase.assertEquals(1, numf);
    int nump = ap.getApngNumPlays();
    TestCase.assertEquals(0, nump);
    ap.end();
    System.out.printf("numframes: %d\n", numf);
  }
}
