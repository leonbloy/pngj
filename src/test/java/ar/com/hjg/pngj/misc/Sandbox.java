package ar.com.hjg.pngj.misc;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngReaderInt;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;

public class Sandbox {
  public static void reencodeWithJavaIo(File s, File d) throws IOException {
    BufferedImage img = ImageIO.read(s);
    ImageIO.write(img, "PNG", d);
    System.out.printf("%s -> %s \n", s.getName(), d.getName());
  }

  public static void convert(String origFilename, String destFilename) {
    // you can also use PngReader (esentially the same) or PngReaderByte
    PngReaderInt pngr = new PngReaderInt(new File(origFilename));
    System.out.println(pngr.toString());
    int channels = pngr.imgInfo.channels;
    if (channels < 3 || pngr.imgInfo.bitDepth != 8)
      throw new RuntimeException("For simplicity this supports only RGB8/RGBA8 images");
    // writer with same image properties as original
    PngWriter pngw = new PngWriter(new File(destFilename), pngr.imgInfo, true);
    // instruct the writer to grab all ancillary chunks from the original
    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL_SAFE);
    // add a textual chunk to writer
    pngw.getMetadata()
        .setText(PngChunkTextVar.KEY_Description, "Decreased red and increased green");
    // also: while(pngr.hasMoreRows())
    for (int row = 0; row < pngr.imgInfo.rows; row++) {
      ImageLineInt l1 = pngr.readRowInt(); // each element is a sample
      int[] scanline = l1.getScanline(); // to save typing
      for (int j = 0; j < pngr.imgInfo.cols; j++) {
        scanline[j * channels] /= 2;
        scanline[j * channels + 1] = ImageLineHelper.clampTo_0_255(scanline[j * channels + 1] + 20);
      }
      pngw.writeRow(l1);
    }
    pngr.end(); // it's recommended to end the reader first, in case there are trailing chunks to read
    pngw.end();
  }


  public static void reencodeWithFilter(File s, FilterType ftype) throws IOException {
    PngReader p = new PngReader(s);
    NullOs nos = new NullOs();
    PngWriter pw = new PngWriter(nos, p.imgInfo);
    pw.setFilterType(ftype);
    pw.setCompLevel(6);
    pw.writeRows(p.readRows());
    p.end();
    pw.end();
    System.out.println(s + " f=" + ftype + " comp:" + pw.getPixelsWriter().getCompression() + " "
        + pw.getPixelsWriter().getFiltersUsed());
  }

  public static void images() throws IOException {
    File png = new File("c:\\temp\\04ptx.png");
    BufferedImage bi1 = ImageIO.read(png);
    System.out.printf("%s is TYPE_BYTE_BINARY? %s (%d)\n", png,
        String.valueOf(BufferedImage.TYPE_BYTE_BINARY == bi1.getType()), bi1.getType());
    int p1i1 = bi1.getRGB(1, 1);
    int p2i1 = bi1.getRGB(2, 1);
    System.out.printf("im1: p1=%08x %s  p2=%08x %s\n", p1i1, formatARGB(p1i1), p2i1,
        formatARGB(p2i1));
    BufferedImage bi2 =
        new BufferedImage(bi1.getWidth(), bi1.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
    Graphics2D g = bi2.createGraphics();
    g.setComposite(AlphaComposite.Src);
    g.drawImage(bi1, 0, 0, null);
    int p1i2 = bi2.getRGB(1, 1);
    int p2i2 = bi2.getRGB(2, 1);
    System.out.printf("im2: p1=%08x %s  p2=%08x %s\n", p1i2, formatARGB(p1i2), p2i2,
        formatARGB(p2i2));
  }

  // http://stackoverflow.com/questions/23707736/strange-bufferedimage-behaviour-with-4bits-palette
  public static void images3() throws IOException {
    File png = new File("c:\\temp\\04ptxx.png");
    BufferedImage bi1 = ImageIO.read(png);
    System.out.printf("%s is TYPE_BYTE_BINARY? %s (%d)\n", png,
        String.valueOf(BufferedImage.TYPE_BYTE_BINARY == bi1.getType()), bi1.getType());
    int p1i1 = bi1.getRGB(0, 0);
    int p2i1 = bi1.getRGB(1, 0);
    System.out.printf("im1: p1=%08x %s  p2=%08x %s\n", p1i1, formatARGB(p1i1), p2i1,
        formatARGB(p2i1));
    {
      BufferedImage bi2 =
          new BufferedImage(bi1.getWidth(), bi1.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
      Graphics2D g2d = (Graphics2D) bi2.getGraphics();
      bi2.getGraphics().drawImage(bi1, 0, 0, null);
      int p1i2 = bi2.getRGB(0, 0);
      int p2i2 = bi2.getRGB(1, 0);
      System.out.printf("im2: p1=%08x %s  p2=%08x %s\n", p1i2, formatARGB(p1i2), p2i2,
          formatARGB(p2i2));
    }
    {
      BufferedImage bi3 =
          new BufferedImage(bi1.getWidth(), bi1.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);
      bi3.getGraphics().drawImage(bi1, 0, 0, null);
      int p1i3 = bi3.getRGB(0, 0);
      int p2i3 = bi3.getRGB(1, 0);
      System.out.printf("im3: p1=%08x %s  p2=%08x %s\n", p1i3, formatARGB(p1i3), p2i3,
          formatARGB(p2i3));
    }
  }



  public static void imagesas() throws IOException {
    File png =
        new File("d:\\devel\\repositories\\pngj\\src\\test\\resources\\colormodels\\08ax2.png");
    BufferedImage bi1 = ImageIO.read(png);
    int p1i1 = bi1.getRGB(0, 0);
    int p2i1 = bi1.getRGB(1, 0);
    System.out.printf("im1: p1=%08x %s  p2=%08x %s\n", p1i1, formatARGB(p1i1), p2i1,
        formatARGB(p2i1) + " TYPE_4BYTE_ABGR? " + (bi1.getType() == BufferedImage.TYPE_4BYTE_ABGR));
    BufferedImage bi2 =
        new BufferedImage(bi1.getWidth(), bi1.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
    ((Graphics2D) bi2.getGraphics()).setComposite(AlphaComposite.Src);
    bi2.getGraphics().drawImage(bi1, 0, 0, null);
    int p1i2 = bi2.getRGB(0, 0);
    int p2i2 = bi2.getRGB(1, 0);
    System.out.printf("im2: p1=%08x %s  p2=%08x %s\n", p1i2, formatARGB(p1i2), p2i2,
        formatARGB(p2i2));
  }

  public static boolean imagesTestBi11(int a, int r, int g, int b) throws IOException {
    BufferedImage bi1 = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
    int argb = ((a << 24) | (r << 16) | (g << 8) | b);
    bi1.setRGB(0, 0, argb);
    BufferedImage bi2 = new BufferedImage(bi1.getWidth(), bi1.getHeight(), bi1.getType());
    bi2.getGraphics().drawImage(bi1, 0, 0, null);
    int p1 = bi2.getRGB(0, 0);
    int p2 = bi1.getRGB(0, 0);
    System.out.printf("im1: %08x %s ", p2, formatARGB(p2));
    System.out.printf("im2: %08x %s %s\n", p1, formatARGB(p1), (p1 == p2 ? "" : "DIF"));
    return p1 == p2;
  }

  public static String formatARGB(int v) {
    return String.format("(%d,%d,%d,%d)", (v >> 24) & 0xFF, (v >> 16) & 0xFF, (v >> 8) & 0xFF,
        v & 0xFF);
  }

  public static String formatARGB2(int v) {
    int a = (v >> 24) & 0xFF;
    return String.format("(%d,%d,%d,%d) (%f,%f,%f)", (v >> 24) & 0xFF, (v >> 16) & 0xFF,
        (v >> 8) & 0xFF, v & 0xFF, a * ((v >> 16) & 0xFF) / 255.0, a * ((v >> 8) & 0xFF) / 255.0, a
            * (v & 0xFF) / 255.0);
  }



  public static int guessVal(int v) {
    int a = (v >> 24) & 0xFF;
    int r = (v >> 16) & 0xFF;
    int g = (v >> 8) & 0xFF;
    int b = (v) & 0xFF;
    if (a == 0)
      return 0;
    r = (int) Math.round((a * r) / 255.0);
    g = (int) Math.round(a * (g / 255.0));
    b = (int) Math.round(a * b / 255.0);
    r = (r * 255) / a;
    g = (g * 255) / a;
    b = (b * 255) / a;
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  public static void imagesTestBiIssue2(int a, int r, int g, int b) throws IOException {
    BufferedImage bi1 = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    int argb = (a << 24 | r << 16 | g << 8 | b);
    bi1.setRGB(0, 0, argb);
    int p1 = bi1.getRGB(0, 0);
    BufferedImage bi2 = new BufferedImage(bi1.getWidth(), bi1.getHeight(), bi1.getType());
    Graphics2D gr = bi2.createGraphics();
    gr.setComposite(AlphaComposite.Src);
    gr.drawImage(bi1, 0, 0, null);
    int p2 = bi2.getRGB(0, 0);
    int p3 = guessVal(argb);
    if (p2 != p1) {
      System.out.printf("im1: %08x %s ", p1, formatARGB2(p1));
      System.out.printf("im2: %08x (%08x) %s %s\n", p2, p3, formatARGB2(p2),
          (p1 == p3 ? "" : "DIF"));
      System.out.printf("(%08x) %s \n", p3, formatARGB2(p3));
    }
  }

  public static void imagesTestBiIssue3(int a, int r, int g, int b) throws IOException {
    BufferedImage bi1 = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    int argb = (a << 24 | r << 16 | g << 8 | b);
    bi1.setRGB(0, 0, argb);
    int p1 = bi1.getRGB(0, 0);
    BufferedImage bi2 = new BufferedImage(bi1.getWidth(), bi1.getHeight(), bi1.getType());
    ((Graphics2D) bi2.getGraphics()).setComposite(AlphaComposite.Src);
    int p2 = bi2.getRGB(0, 0);
    int rx = (p2 >> 16) & 0xff;
    int r3 = guessVal(a, r);
    if (rx != r3)
      System.out.printf("%d %d %d (%d)\n", a, r, rx, r3);
  }

  private static int guessVal(int a, int r) {
    int rr = (int) Math.round(r * a / 255.0);
    double x = Math.nextAfter(255.0, 1.0);
    int rr2 = (int) Math.round(rr * 255.0 / a);
    return rr2;
  }

  public static void main(String[] args) throws Exception {
    images();
  }
}
