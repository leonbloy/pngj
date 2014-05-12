package ar.com.hjg.pngj.awt;

import java.awt.image.BufferedImage;
import java.io.File;

import org.junit.Test;

import ar.com.hjg.pngj.test.TestSupport;


public class ImageLineBITest {
  
  
  private void testWrite(File ori,boolean preferCustom) {
    System.err.printf("====testing %s cust=%s==\n",ori.getName(),preferCustom);
    PngReaderBI png=new PngReaderBI(ori);
    png.setPreferCustomInsteadOfBGR(preferCustom);
    File dest = TestSupport.absFile("test/__test.tmp.png");
    //dest.deleteOnExit();
    BufferedImage img = png.readAll();
    System.err.println(ImageIoUtils.imageTypeName(img.getType()));
    ImageIoUtils.writePng(dest, img);
    TestSupport.testSameCrc(ori, dest);
  }

  private void testWrite2(File ori,boolean preferCustom) {
    System.err.printf("====testing with values %s cust=%s==\n",ori.getName(),preferCustom);
    PngReaderBI png=new PngReaderBI(ori);
    png.setPreferCustomInsteadOfBGR(preferCustom);
    File dest = TestSupport.absFile("test/__test.tmp.png");
    //dest.deleteOnExit();
    BufferedImage img = png.readAll();
    System.err.println(ImageIoUtils.imageTypeName(img.getType()));
    ImageIoUtils.writePng(dest, img);
    TestSupport.testSameValues(ori, dest);
  }

  
  @Test
  public void testWriteRgb8() {
    testWrite(TestSupport.absFile("testsuite1/basn2c08.png"),true);
    testWrite(TestSupport.absFile("testsuite1/basn2c08.png"),false);
    testWrite(TestSupport.absFile("testsuite1/basi2c08.png"),true);
  }
  
  @Test
  public void testWriteRgba8() {
    testWrite(TestSupport.absFile("testsuite1/basn6a08.png"),true);
    testWrite(TestSupport.absFile("testsuite1/basn6a08.png"),false);
    testWrite(TestSupport.absFile("testsuite1/basi6a08.png"),false);
  }
  
  @Test
  public void testWriteRgb16() {
    testWrite(TestSupport.absFile("testsuite1/basn2c16.png"),true);
    testWrite(TestSupport.absFile("testsuite1/basn2c16.png"),false);
    testWrite(TestSupport.absFile("testsuite1/basi2c16.png"),false);
  }
  
  @Test
  public void testWriteRgbA16() {
    testWrite(TestSupport.absFile("testsuite1/basn6a16.png"),true);
    testWrite(TestSupport.absFile("testsuite1/basn6a16.png"),false);
    testWrite(TestSupport.absFile("testsuite1/basi6a16.png"),true);
  }
  
  @Test
  public void testWriteG8() {
    testWrite(TestSupport.absFile("testsuite1/basn0g08.png"),true);
    testWrite(TestSupport.absFile("testsuite1/basn0g08.png"),false);
    testWrite(TestSupport.absFile("testsuite1/basi0g08.png"),false);
  }
  
  @Test
  public void testWriteGA8() {
    testWrite(TestSupport.absFile("testsuite1/basn4a08.png"),true);
    testWrite(TestSupport.absFile("testsuite1/basn4a08.png"),false);
    testWrite(TestSupport.absFile("testsuite1/basi4a08.png"),false);
  }

  @Test
  public void testWriteG16() {
    testWrite(TestSupport.absFile("testsuite1/basn0g16.png"),true);
    testWrite(TestSupport.absFile("testsuite1/basn0g16.png"),false);
    testWrite(TestSupport.absFile("testsuite1/basi0g16.png"),true);
  }

  @Test
  public void testPalette8() {
    testWrite(TestSupport.absFile("testsuite1/basn3p08.png"),true);
    testWrite(TestSupport.absFile("testsuite1/basn3p08.png"),false);
    testWrite(TestSupport.absFile("testsuite1/basi3p08.png"),false);
  }
  

  @Test
  public void testWriteG124() {
    testWrite2(TestSupport.absFile("testsuite1/basn0g01.png"),true);
    testWrite2(TestSupport.absFile("testsuite1/basi0g01.png"),false);
    testWrite2(TestSupport.absFile("testsuite1/basn0g02.png"),false);
    testWrite2(TestSupport.absFile("testsuite1/basi0g01.png"),true);
    testWrite2(TestSupport.absFile("testsuite1/basi0g04.png"),true);
   
  }

  @Test
  public void testPalette124() {
    testWrite2(TestSupport.absFile("testsuite1/basn3p01.png"),false);
    testWrite2(TestSupport.absFile("testsuite1/basi3p02.png"),true);
    testWrite2(TestSupport.absFile("testsuite1/basn3p04.png"),false);
  }
  // to do: TRNS
}
