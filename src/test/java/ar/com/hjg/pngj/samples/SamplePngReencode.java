package ar.com.hjg.pngj.samples;

import java.io.File;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.pixels.PixelsWriter;
import ar.com.hjg.pngj.pixels.PixelsWriterDefault;

/**
 * reencodes a png image with a given filter and compression level
 */
public class SamplePngReencode {
  public static void reencode2(String orig, String dest, int cLevel) {
    PngReader pngr = new PngReader(new File(orig));
    PngWriter pngw = new PngWriter(new File(dest), pngr.imgInfo, true) {
      @Override
      protected PixelsWriter createPixelsWriter(ImageInfo imginfo) {
        PixelsWriterDefault pw = new PixelsWriterDefault(imgInfo);
        pw.setFilterType(FilterType.FILTER_PRESERVE);
        return pw;
      }
    };
    pngw.setIdatMaxSize(10000000);
    System.out.println(pngr.toString());
    System.out.printf("Creating Image %s  superadaptive compLevel=%d \n", dest, cLevel);
    pngw.setCompLevel(cLevel);
    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
    for (int row = 0; row < pngr.imgInfo.rows; row++) {
      IImageLine l1 = pngr.readRow();
      pngw.writeRow(l1);
    }
    pngr.end();
    pngw.end();
    System.out.printf("Done. Compression: %.3f \n", pngw.computeCompressionRatio());
  }

  public static void reencode(String orig, String dest, FilterType filterType, int cLevel) {
    PngReader pngr = new PngReader(new File(orig));
    PngWriter pngw = new PngWriter(new File(dest), pngr.imgInfo, true);
    System.out.println(pngr.toString());
    System.out.printf("Creating Image %s  filter=%s compLevel=%d \n", dest, filterType.toString(),
        cLevel);
    pngw.setFilterType(filterType);
    pngw.setCompLevel(cLevel);
    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
    for (int row = 0; row < pngr.imgInfo.rows; row++) {
      IImageLine l1 = pngr.readRow();
      pngw.writeRow(l1);
    }
    pngr.end();
    pngw.end();
    System.out.printf("Done. Compression: %.3f \n", pngw.computeCompressionRatio());
  }

  public static void fromCmdLineArgs(String[] args) {
    if (args.length != 4 || args[0].equals(args[1])) {
      System.err.println("Arguments: [pngsrc] [pngdest] [filter] [compressionlevel]");
      System.err.println(" Where filter = 0..4  , compressionLevel = 0 .. 9");
      System.exit(1);
    }
    long t0 = System.currentTimeMillis();
    reencode(args[0], args[1], FilterType.getByVal(Integer.parseInt(args[2])),
        Integer.parseInt(args[3]));
    // reencode2(args[0], args[1], Integer.parseInt(args[3]));
    long t1 = System.currentTimeMillis();
    System.out.println("Listo: " + (t1 - t0) + " msecs");
  }

  public static void main(String[] args) throws Exception {
    fromCmdLineArgs(args);
  }
}
