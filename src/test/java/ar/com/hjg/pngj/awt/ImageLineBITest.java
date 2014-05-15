package ar.com.hjg.pngj.awt;

import java.awt.image.BufferedImage;
import java.io.File;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.PngReaderByte;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.test.TestSupport;


public class ImageLineBITest {


  private void testRead(File ori, boolean preferCustom) {
    System.err.printf("====testing %s cust=%s==\n", ori.getName(), preferCustom);
    PngReaderBI png = new PngReaderBI(ori);
    png.setPreferCustomInsteadOfBGR(preferCustom);
    File dest = TestSupport.absFile("test/__test.tmp.png");
    // dest.deleteOnExit();
    BufferedImage img = png.readAll();
    System.err.println(ImageIoUtils.imageTypeName(img.getType()));
    ImageIoUtils.writePng(dest, img);
    TestSupport.testSameCrc(ori, dest);
  }

  private void testRead2(File ori, boolean preferCustom) {
    System.err.printf("====testing with values %s cust=%s==\n", ori.getName(), preferCustom);
    PngReaderBI png = new PngReaderBI(ori);
    png.setPreferCustomInsteadOfBGR(preferCustom);
    File dest = TestSupport.absFile("test/__test.tmp.png");
    // dest.deleteOnExit();
    BufferedImage img = png.readAll();
    System.err.println(ImageIoUtils.imageTypeName(img.getType()));
    ImageIoUtils.writePng(dest, img);
    TestSupport.testSameValues(ori, dest);
  }


  @Test
  public void testReadRgb8() {
    testRead(TestSupport.absFile("testsuite1/basn2c08.png"), true);
    testRead(TestSupport.absFile("testsuite1/basn2c08.png"), false);
    testRead(TestSupport.absFile("testsuite1/basi2c08.png"), true);
  }

  @Test
  public void testReadRgba8() {
    testRead(TestSupport.absFile("testsuite1/basn6a08.png"), true);
    testRead(TestSupport.absFile("testsuite1/basn6a08.png"), false);
    testRead(TestSupport.absFile("testsuite1/basi6a08.png"), false);
  }

  @Test
  public void testReadRgb16() {
    testRead(TestSupport.absFile("testsuite1/basn2c16.png"), true);
    testRead(TestSupport.absFile("testsuite1/basn2c16.png"), false);
    testRead(TestSupport.absFile("testsuite1/basi2c16.png"), false);
  }

  @Test
  public void testReadRgbA16() {
    testRead(TestSupport.absFile("testsuite1/basn6a16.png"), true);
    testRead(TestSupport.absFile("testsuite1/basn6a16.png"), false);
    testRead(TestSupport.absFile("testsuite1/basi6a16.png"), true);
  }

  @Test
  public void testReadG8() {
    testRead(TestSupport.absFile("testsuite1/basn0g08.png"), true);
    testRead(TestSupport.absFile("testsuite1/basn0g08.png"), false);
    testRead(TestSupport.absFile("testsuite1/basi0g08.png"), false);
  }

  @Test
  public void testReadGA8() {
    testRead(TestSupport.absFile("testsuite1/basn4a08.png"), true);
    testRead(TestSupport.absFile("testsuite1/basn4a08.png"), false);
    testRead(TestSupport.absFile("testsuite1/basi4a08.png"), false);
  }

  @Test
  public void testReadG16() {
    testRead(TestSupport.absFile("testsuite1/basn0g16.png"), true);
    testRead(TestSupport.absFile("testsuite1/basn0g16.png"), false);
    testRead(TestSupport.absFile("testsuite1/basi0g16.png"), true);
  }

  @Test
  public void testPalette8() {
    testRead(TestSupport.absFile("testsuite1/basn3p08.png"), true);
    testRead(TestSupport.absFile("testsuite1/basn3p08.png"), false);
    testRead(TestSupport.absFile("testsuite1/basi3p08.png"), false);
  }


  @Test
  public void testReadG124() {
    testRead2(TestSupport.absFile("testsuite1/basn0g01.png"), true);
    testRead2(TestSupport.absFile("testsuite1/basi0g01.png"), false);
    testRead2(TestSupport.absFile("testsuite1/basn0g02.png"), false);
    testRead2(TestSupport.absFile("testsuite1/basi0g01.png"), true);
    testRead2(TestSupport.absFile("testsuite1/basi0g04.png"), true);

  }

  @Test
  public void testReadPalette124() {
    testRead2(TestSupport.absFile("testsuite1/basn3p01.png"), false);
    testRead2(TestSupport.absFile("testsuite1/basi3p02.png"), true);
    testRead2(TestSupport.absFile("testsuite1/basn3p04.png"), false);
  }

  @Test
  public void testReadPaletteWithTrns() {
    testRead2(TestSupport.absFile("testsuite1/tp1n3p08.png"), false); // first palette val is transparent
  }

  @Test
  public void testReadPartialRgb8() {
    File f = TestSupport.absFile("testsuite2/basn2c08.png");
    File dest = TestSupport.absFile("test/__test.tmp.png");
    File dest2 = TestSupport.absFile("test/__test2.tmp.png");
    int nlines = 10, offset = 1, step = 2;
    {
      PngReaderBI png = new PngReaderBI(f);
      // dest.deleteOnExit();
      BufferedImage img = png.readAll(nlines, offset, step); // 10 lines, starting for 1, skipping 1
      System.err.println(ImageIoUtils.imageTypeName(img.getType()));
      ImageIoUtils.writePng(dest, img);
    }
    {
      PngReaderByte png2 = new PngReaderByte(f);
      PngWriter pngw = new PngWriter(dest2, png2.imgInfo.withSize(-1, 10));
      pngw.setFilterType(FilterType.FILTER_AVERAGE);
      pngw.writeRows(png2.readRows(nlines, offset, step));
      png2.end();
      pngw.end();
    }
    TestSupport.testSameCrc(dest, dest2);
  }

  public void testWrite1(File f, int convToType, boolean forceRgb) {
    try {
    File dest = TestSupport.absFile("test/__test.tmp.png");
    BufferedImage bi1 = ImageIoUtils.readPng(f);
    System.err.println(f+ " type=" +ImageIoUtils.imageTypeName(bi1.getType())  +" conv to " + (convToType>-1? ImageIoUtils.imageTypeName(convToType) :"-") + " force RGB="+forceRgb);
    BufferedImage bi2 = null;
    if (convToType > 0) {
      bi2 = new BufferedImage(bi1.getWidth(), bi1.getHeight(), convToType);
      bi2.getGraphics().drawImage(bi1, 0, 0, null);
    }
    BufferedImage2PngAdapter adap = new BufferedImage2PngAdapter(bi2 != null ? bi2 : bi1);
    adap.forceresortToGetRGB = forceRgb;
    PngWriterBI pngw = PngWriterBI.createInstance(adap, dest);
    pngw.writeAll();
    TestSupport.testSameValues(f, dest);
    } catch(AssertionFailedError e) {
      System.err.println("Error with " +f+ " typeconv=" + ImageIoUtils.imageTypeNameShort(convToType) + " cordeRgb="+forceRgb);
      throw e;
    }
  }

  @Test
  public void testxWriteRgb8() {
    testWrite1(TestSupport.absFile("testsuite2/basn2c08.png"),BufferedImage.TYPE_3BYTE_BGR,false);
    testWrite1(TestSupport.absFile("testsuite2/basn2c08.png"),BufferedImage.TYPE_3BYTE_BGR,true);
    testWrite1(TestSupport.absFile("testsuite2/basn2c08.png"),BufferedImage.TYPE_4BYTE_ABGR,false);
    testWrite1(TestSupport.absFile("testsuite2/basn2c08.png"),BufferedImage.TYPE_4BYTE_ABGR_PRE,false); // this uses custom
    testWrite1(TestSupport.absFile("testsuite2/basn2c08.png"),BufferedImage.TYPE_INT_ARGB,true);
    testWrite1(TestSupport.absFile("testsuite2/basn2c08.png"),BufferedImage.TYPE_INT_RGB,false);
    testWrite1(TestSupport.absFile("testsuite2/basn2c08.png"),BufferedImage.TYPE_INT_BGR,false);
    testWrite1(TestSupport.absFile("testsuite2/basn2c08.png"),BufferedImage.TYPE_INT_ARGB,false);
    testWrite1(TestSupport.absFile("testsuite2/basn2c08.png"),BufferedImage.TYPE_CUSTOM,false);
    //testWrite1(TestSupport.absFile("testsuite2/basn2c08.png"),BufferedImage.TYPE_BYTE_BINARY,false);
  }


}
