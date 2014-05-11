package ar.com.hjg.pngj.misc;

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

  public static void main(String[] args) throws Exception {
    reencodeWithFilter(new File(
        "d:\\devel\\repositories\\pngj\\src\\test\\resources\\test\\bad_truncated.png"),
        FilterType.FILTER_NONE);
  }
}
