package ar.com.hjg.pngj.test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import ar.com.hjg.pngj.pixels.CompressorStreamDeflater;
import ar.com.hjg.pngj.pixels.CompressorStreamLz4;
import ar.com.hjg.pngj.pixels.DeflaterEstimatorLz4;

public class CompressEstimTest {

  private static final int DEFLATE = 1;
  private static final int LZ4 = 2;

  static byte[] buf0 = new byte[512]; // 512
  static byte[] buf1 = new byte[4096]; // 4K
  static byte[] buf2 = new byte[1024 * 70]; // 70Kb
  static byte[] buf3 = new byte[1024 * 512]; // 512Kb

  static byte[][] buffers = new byte[][] {buf0, buf1, buf2, buf3};
  double[] cbufd = new double[4];
  double[] cbuflz4 = new double[4];

  private byte[] buftmp = new byte[8192];

  Deflater def = new Deflater();
  DeflaterEstimatorLz4 lz4 = new DeflaterEstimatorLz4();

  static {
    staticInit();
  }

  private static void staticInit() {
    initbuf();
  }

  private static void initbuf() {
    for (int bi = 0; bi < buffers.length; bi++) {
      byte[] b = buffers[bi];
      for (int i = 1; i < b.length; i++) {
        int x = (i % 33333);
        int l = 7 + (int) ((9 * Math.pow(i, 0.7)) / b.length);
        int j = (x * l) / 3;
        b[i] = (byte) (i < 1000 ? j : b[i - 400 + b[i - 1]]);
      }
      // System.out.print(b.length + ":" + Arrays.toString(Arrays.copyOfRange(b, 0, 13)));
      // System.out.println(" ..." + Arrays.toString(Arrays.copyOfRange(b, b.length - 13,
      // b.length)));
    }
  }

  @Before
  public void computecompressions() { // same as testOpaque1 with buffering
    for (int i = 0; i < buffers.length; i++) {
      cbufd[i] = compressEstim(buffers[i], DEFLATE);
      cbuflz4[i] = compressEstim(buffers[i], LZ4);
      // System.out.printf("len=%6d def=%.3f lz4=%.3f\n", buffers[i].length, cbufd[i], cbuflz4[i]);
    }
    /*
     * len= 512 def=0.850 lz4=0.840 len= 4096 def=0.880 lz4=0.948 len= 71680 def=0.153 lz4=0.216 len=524288 def=0.026
     * lz4=0.037
     */
  }

  private double compressEstim(byte[] f, int method) {
    return compressEstim(f, 0, f.length, method);
  }

  private double compressEstim(byte[] f, int off, int flen, int method) {
    if (flen == 0)
      return 1.0;
    if (method == LZ4) {
      int count = lz4.compressEstim(f, off, flen);
      return count / (double) flen;
    } else if (method == DEFLATE) {
      def.reset();
      def.setInput(f, off, flen);
      def.finish();
      int count = 0;
      int d;
      while ((d = def.deflate(buftmp)) > 0) {
        count += d;
      }
      return count / (double) flen;
    } else
      throw new RuntimeException("?");
  }

  @Test
  public void testCompPartitionDef() {
    byte[] b = buffers[3];
    double ctarget = cbufd[3];
    int method = DEFLATE;
    int segments = 32;
    int stride = b.length / segments;
    double ratio = 0;
    for (int s = 0, o = 0; s < segments; s++, o += stride) {
      ratio += compressEstim(b, o, stride, method);
    }
    ratio /= segments;
    // System.err.printf("ctarget=%.4f ratio=%.4f \n",ctarget,ratio);
    TestCase.assertEquals(ctarget, ratio, 0.02);
  }

  @Test
  public void testCompPartitionLz4() {
    byte[] b = buffers[3];
    double ctarget = cbuflz4[3];
    int method = LZ4;
    int segments = 32;
    int stride = b.length / segments;
    double ratio = 0;
    for (int s = 0, o = 0; s < segments; s++, o += stride) {
      ratio += compressEstim(b, o, stride, method);
    }
    ratio /= segments;
    // System.err.printf("ctarget=%.4f ratio=%.4f \n",ctarget,ratio);
    TestCase.assertEquals(ctarget, ratio, 0.02);
  }

  @Test
  public void testDeflater1() { // this is a little slow
    for (int i = 0; i < buffers.length; i++) {
      byte[] b = buffers[i];
      double ctarget = cbufd[i];
      CompressorStreamDeflater ds = new CompressorStreamDeflater(null, b.length, b.length);
      ds.write(b);
      double cr1 = ds.getCompressionRatio();
      ds.done(); // should not change
      double cr1b = ds.getCompressionRatio();
      TestCase.assertEquals(cr1, cr1b);
      TestCase.assertEquals(ctarget, cr1, 0.00001);
      ds.reset();
      for (int stride = 1; stride < b.length - 10; stride += 1 + b.length / 20) {
        writeWithStride(stride, ds, b);
        cr1 = ds.getCompressionRatio();
        ds.done(); // should not change
        cr1b = ds.getCompressionRatio();
        TestCase.assertEquals(cr1, cr1b);
        TestCase.assertEquals(ctarget, cr1, 0.00001);
        ds.reset();
      }
    }
  }

  @Test
  public void testLz4SmallFixedLength() {
    for (int i = 0; i < 2; i++) {
      byte[] b = buffers[i];
      double ctarget = cbuflz4[i];
      double cr1, cr1b;
      {
        CompressorStreamLz4 ds = new CompressorStreamLz4(null, b.length, b.length);
        ds.write(b);
        cr1 = ds.getCompressionRatio();
        ds.done(); // should not change
        cr1b = ds.getCompressionRatio();
        TestCase.assertEquals(cr1, cr1b);
        TestCase.assertEquals(ctarget, cr1, 0.00001);
        ds.close();
      }
      for (int stride = b.length - 30; stride > 0; stride -= (1 + b.length / 200)) {
        CompressorStreamLz4 ds2 = new CompressorStreamLz4(null, stride, b.length);
        writeWithStride(stride, ds2, b);
        cr1 = ds2.getCompressionRatio();
        TestCase.assertTrue(
            "ctarget=" + ctarget + " cr1=" + cr1 + " stride=" + stride + " buf" + i, ds2.isDone());
        ds2.done(); // should not change
        cr1b = ds2.getCompressionRatio();
        TestCase.assertEquals(cr1, cr1b);
        TestCase.assertEquals(ctarget, cr1, 0.00001);
        ds2.reset();
      }
    }
  }

  @Test
  public void testLz4LargeFixedLength() {
    for (int i = 2; i < buffers.length; i++) {
      byte[] b = buffers[i];
      double ctarget = cbuflz4[i];
      double cr1, cr1b;
      {
        CompressorStreamLz4 ds = new CompressorStreamLz4(null, b.length, b.length);
        ds.write(b);
        cr1 = ds.getCompressionRatio();
        ds.done(); // should not change
        cr1b = ds.getCompressionRatio();
        TestCase.assertEquals(cr1, cr1b);
        TestCase.assertEquals(ctarget, cr1, 0.00001);
        ds.close();
      }
      for (int stride = b.length - 30; stride > 0; stride -= (1 + b.length / 200)) {
        CompressorStreamLz4 ds2 = new CompressorStreamLz4(null, stride, b.length);
        writeWithStride(stride, ds2, b);
        cr1 = ds2.getCompressionRatio();
        TestCase.assertTrue(
            "ctarget=" + ctarget + " cr1=" + cr1 + " stride=" + stride + " buf" + i, ds2.isDone());
        ds2.done(); // should not change
        cr1b = ds2.getCompressionRatio();
        TestCase.assertEquals(cr1, cr1b);
        double err = 1 - ctarget / cr1;
        // System.err.printf("ctarget=%.4f cr1=%.4f stride=%d/%d err=%.4f\n",ctarget,cr1,stride,b.length,err);
        TestCase.assertTrue(
            "ctarget=" + ctarget + " cr1=" + cr1 + " stride=" + stride + " buf" + i,
            Math.abs(err) < 0.5); // we have some tolerance here
        ds2.close();
      }
    }
  }


  @Test
  public void testLz4LargeFixedLength2() {
    byte[] b = buffers[2];
    double ctarget = cbuflz4[2];
    double cr1, cr1b;
    {
      // PngHelperInternal.debug("===no stride ====================" );
      CompressorStreamLz4 ds = new CompressorStreamLz4(null, b.length, b.length);
      ds.write(b);
      cr1 = ds.getCompressionRatio();
      ds.done(); // should not change
      cr1b = ds.getCompressionRatio();
      TestCase.assertEquals(cr1, cr1b);
      TestCase.assertEquals(ctarget, cr1, 0.00001);
      ds.close();
    }
    {
      for (int stride = 44000; stride > 0; stride -= 43000) {
        // PngHelperInternal.debug("===stride " + stride + "========================");
        CompressorStreamLz4 ds2 = new CompressorStreamLz4(null, stride, b.length);
        writeWithStride(stride, ds2, b);
        cr1 = ds2.getCompressionRatio();
        TestCase.assertTrue("ctarget=" + ctarget + " cr1=" + cr1 + " stride=" + stride,
            ds2.isDone());
        ds2.done(); // should not change
        cr1b = ds2.getCompressionRatio();
        TestCase.assertEquals(cr1, cr1b);
        double err = 1 - ctarget / cr1;
        // System.err.printf("ctarget=%.4f cr1=%.4f stride=%d/%d err=%.4f\n",ctarget,cr1,stride,b.length,err);
        TestCase.assertTrue("ctarget=" + ctarget + " cr1=" + cr1 + " stride=" + stride,
            Math.abs(err) < 0.5); // we have some tolerance here
        ds2.close();
      }
    }
  }


  private void writeWithStride(int stride, OutputStream os, byte[] b) {
    writeWithStride(stride, os, b, 0, b.length);
  }

  private void writeWithStride(int stride, OutputStream os, byte[] b, int off, int len) {
    int len2;
    while (len > 0) {
      len2 = len > stride ? stride : len;
      try {
        os.write(b, off, len2);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      len -= len2;
      off += len2;
    }
  }
}
