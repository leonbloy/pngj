package ar.com.hjg.pngj.misc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.zip.Deflater;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineArray;
import ar.com.hjg.pngj.IPngWriterFactory;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngReaderByte;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngWriterHc;
import ar.com.hjg.pngj.PngjInputException;
import ar.com.hjg.pngj.awt.ImageIoUtils;
import ar.com.hjg.pngj.pixels.DeflaterEstimatorLz4;
import ar.com.hjg.pngj.pixels.FiltersPerformance;
import ar.com.hjg.pngj.pixels.PixelsWriter;
import ar.com.hjg.pngj.pixels.PixelsWriterDefault;
import ar.com.hjg.pngj.samples.PngReaderDumb;
import ar.com.hjg.pngj.test.TestSupport;

public class WriterFiltersPerformanceUtil {
  private Collection<File> orig;
  private long[] timeBySize = new long[3];
  private double timeAv = 0;
  private double timeWeighted = 0;
  private double performanceAv = 0;
  // private double badness = 0; // only takes into account the "bad" results
  private double performanceWAv = 0;
  private int nFiles;
  private long bytesOrigRaw;
  private String desc = "";
  String extrainfo = "";
  public boolean condOnlyRGB8 = false;
  public boolean condOnlyAlpha = false;
  public boolean condOnly16bits = false;
  public boolean condOnlylessthan8bppOrPalette = false;
  public boolean condOnlyGray = false;

  private static Boolean SHOW_FILENAME_FORCE = null; // if this is != null,
  // overrides

  boolean showFileName = false;
  private IPngWriterFactory writerFactory;

  public WriterFiltersPerformanceUtil(Collection<File> orig, IPngWriterFactory writerFactory) {
    this.orig = orig;
    this.writerFactory = writerFactory;
  }

  public String doit(int nrepeat) {
    StringBuilder sb = new StringBuilder();
    if (!desc.isEmpty())
      sb.append(desc + "\n");
    if (nFiles != 0)
      throw new RuntimeException("?");
    for (File f : orig) {
      // System.out.println(f+ "memory " +
      // Runtime.getRuntime().freeMemory());
      WriterFiltersPerformance p = new WriterFiltersPerformance(f, writerFactory);
      p.setRepeat(nrepeat);
      p.setWriteToFile(orig.size() == 1); // if only one file, we write it
      p.condOnlyRGB8 = this.condOnlyRGB8;
      p.condOnlyAlpha = this.condOnlyAlpha;
      p.condOnly16bits = this.condOnly16bits;
      p.condOnlylessthan8bppOrPalette = this.condOnlylessthan8bppOrPalette;
      p.condOnlyGray = this.condOnlyGray;
      boolean done = p.doit();
      if (!done)
        continue;
      nFiles++;
      sb.append(p.getSumm(SHOW_FILENAME_FORCE != null ? SHOW_FILENAME_FORCE.booleanValue()
          : showFileName));
      double msecs = p.getMsecs();
      timeBySize[getSizeType(f)] += msecs;
      bytesOrigRaw += p.imgInfo.getTotalRawBytes();
      timeAv += msecs;
      timeWeighted += msecs * 1000.0 / p.imgInfo.getTotalRawBytes();
      performanceAv += p.getCompression();
      performanceWAv += p.imgInfo.getTotalRawBytes() * p.getCompression();
      double bad = p.getCompression() > 1 ? p.getCompression() - 1 : 0;
      // badness += Math.pow(bad, 3.0);
    }
    timeAv /= nFiles;
    timeWeighted /= nFiles;
    performanceAv /= nFiles;
    performanceWAv /= bytesOrigRaw;
    // badness = Math.pow(badness / nFiles, 1.0 / 3.0);
    return sb.toString();
  }

  static int getSizeType(File f) {
    int sizetype = 0;
    switch (f.getParentFile().getName().charAt(0)) {
      case 'l':
        sizetype = 0;
        break;
      case 'm':
        sizetype = 1;
        break;
      case 's':
        sizetype = 2;
        break;
      default:
        throw new RuntimeException("bad name, not starting with s l m" + f.getName());
    }
    return sizetype;
  }

  public String getSummary() {
    return String.format("\n%d\n%d\n%d\n%.4f\t%.4f\t%d\t%.1f\t%s ", timeBySize[0], timeBySize[1],
        timeBySize[2], performanceAv, performanceWAv, (int) timeAv, timeWeighted, extrainfo) + desc;
  }

  public void setDesc(String string) {
    desc = string;

  }

  public static WriterFiltersPerformanceUtil createFromDir(File dir, IPngWriterFactory writerFactory) {
    return new WriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir), writerFactory);

  }

  public static void reencodePreservingFilters(File file, int complevel) {
    try {
      PngReader pngr = new PngReader(file);
      File filenew = new File(file.getAbsolutePath() + ".new.png");
      PngWriter pngw = new PngWriter(filenew, pngr.imgInfo);
      pngw.setIdatMaxSize(1000 * 1000);
      pngw.setFilterPreserve(true);
      pngw.setCompLevel(complevel);
      // filename bytesraw(idat) bytescompressed
      StringBuilder inf = new StringBuilder();
      inf.append(String.format("%s %d ", file.getName(), pngr.imgInfo.rows
          * pngr.imgInfo.bytesPerRow));
      int[] filters = new int[5];
      pngw.copyChunksFrom(pngr.getChunksList());
      for (int i = 0; i < pngr.imgInfo.rows; i++) {
        IImageLine line = pngr.readRow(i);
        pngw.writeRow(line);
        filters[((ImageLineInt) line).getFilterType().val]++;
      }
      pngr.end();
      pngw.end();
      inf.append(String.format("%d ", filenew.length()));
      inf.append(formatFilterUse(filters, pngr.imgInfo.rows, true));
      System.out.println(inf);
      file.delete();
      filenew.renameTo(file);
    } catch (PngjInputException e) {
      // System.err.println("error with " + file +" " + e);
      file.renameTo(new File(file.getAbsolutePath() + ".bad"));
    }
  }

  private static String formatFilterUse(int[] filters, int rows, boolean usePercent) {
    int[] types = Arrays.copyOf(filters, 5);
    if (usePercent)
      for (int i = 0; i <= 4; i++)
        types[i] = (int) ((types[i] * 100 + rows / 2) / rows);
    String s = Arrays.toString(types);
    return s.substring(1, s.length() - 1).replace(" ", "");
  }

  /* none */
  public static class PngWriterNone extends PngWriter {
    public PngWriterNone(OutputStream outputStream, ImageInfo imgInfo) {
      super(outputStream, imgInfo);
      setCompLevel(9);
      setFilterType(FilterType.FILTER_NONE);
    }
  }

  public static class PngWriterBands extends PngWriter {

    int nrowsband = 8;

    public PngWriterBands(OutputStream os, ImageInfo imgInfo) {
      super(os, imgInfo);
    }

    @Override
    public PixelsWriter createPixelsWriter(ImageInfo imginfo) {
      return new PixelsWriterDefault(imginfo) {
        @Override
        protected void decideCurFilterType() {
          int r = currentRow / nrowsband;
          curfilterType = (r % 2) == 0 ? FilterType.FILTER_NONE : FilterType.FILTER_PAETH;
        }

      };
    }
  }

  public static class DeflaterDummy {
    Deflater def;
    private byte[] buf;
    private int[] histog = new int[256];
    private long histogsum;
    private boolean withHistog = true;

    public DeflaterDummy() {
      def = new Deflater();
      def.setLevel(9);
      buf = new byte[1024];
    }

    public void write(byte[] b, int o, int len) {
      def.setInput(b, o, len);
      if (withHistog) {
        histogsum += len;
        for (int i = o; i < len + o; i++)
          histog[b[i] & 0xff]++;
      }
      while (!def.needsInput() && !def.finished())
        def.deflate(buf);
    }

    public void end() {
      def.finish();
      while (def.deflate(buf) > 0);
    }

    public void reset() {
      def.reset();
      histogsum = 0;
      Arrays.fill(histog, 0);
    }

    /* should be called after end(), before reset */
    public double getRatio() {
      return def.getBytesWritten() / (double) def.getBytesRead();
    }

    public double getEntropy() {
      if (!withHistog)
        return 0;
      double h = 0, x, f;
      f = 1.0 / histogsum;
      for (int xi : histog) {
        if (xi == 0)
          continue;
        x = xi * f;
        h += Math.log(x) * x;
      }
      return (h / (-Math.log(2.0))) / 8.0;
    }
  }

  public static class PngWriterPreserveFactory implements IPngWriterFactory {
    private int clevel;

    public PngWriterPreserveFactory(int clevel) {
      this.clevel = clevel;
    }

    public PngWriter createPngWriter(OutputStream outputStream, ImageInfo imgInfo) {
      PngWriter writer = new PngWriter(outputStream, imgInfo);
      writer.setFilterPreserve(true);
      // writer.getPixelsWriter().setDeflaterStrategy(Deflater.DEFAULT_STRATEGY);
      writer.getPixelsWriter().setDeflaterCompLevel(clevel);
      writer.setIdatMaxSize(1000 * 1000);
      return writer;
    }
  }

  static String DIR_IMAGESET = "d:\\hjg\\wokspace\\Varios\\resources\\pngimages";
  private static ImageWriter imwriter;

  public static void searchForBestPref(File dir) {
    Random r = new Random();
    double lastgoal = Double.MAX_VALUE;
    double[] bak = new double[5];
    double[] mean = new double[5];
    boolean accepted = true;
    int ntries = 50;
    Arrays.fill(mean, 0.0);
    int accepts = 0;
    for (int tries = 0; tries < ntries; tries++) {
      System.arraycopy(FiltersPerformance.FILTER_WEIGHTS_DEFAULT, 0, bak, 0, 5);
      int which = r.nextInt(5);
      which = (tries + 3) % 5;
      double dx = tries > 0 ? (r.nextBoolean() ? 1.033 : 1 / 1.033) : 1.0;
      double dxx = Math.pow(1 / dx, 0.25);
      for (int j = 0; j < 5; j++)
        FiltersPerformance.FILTER_WEIGHTS_DEFAULT[j] *= which == j ? dx : dxx;
      WriterFiltersPerformanceUtil test =
          new WriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir),
              new PngWriterGralFactory(FilterType.FILTER_ADAPTIVE_FULL));
      test.condOnlyRGB8 = true;
      test.doit(1);
      int files = test.nFiles;
      double goal = test.performanceWAv;
      double ganancia = lastgoal >= Double.MAX_VALUE / 2 ? 1.0 : lastgoal - goal;
      accepted = true;
      if (ganancia < 0) {
        if (r.nextDouble() > Math.exp(ganancia / 0.00001))
          accepted = false; // Montecarlo - temperature show be set so that there is ~50% acceptance
      }
      if (accepted) {
        accepts++;
        lastgoal = goal;
      } else { // goal > lastgoal
        System.arraycopy(bak, 0, FiltersPerformance.FILTER_WEIGHTS_DEFAULT, 0, 5);
      }
      for (int j = 0; j < 5; j++) {
        mean[j] += FiltersPerformance.FILTER_WEIGHTS_DEFAULT[j];
      }
      System.out.printf("t=%d/%d goal=%.6f gan=%.6f i=%d %s arr=%s files=%d\n", tries, ntries,
          lastgoal, ganancia, which, (accepted ? "+" : "-"),
          Arrays.toString(FiltersPerformance.FILTER_WEIGHTS_DEFAULT), files);
    }
    for (int j = 0; j < 5; j++) {
      mean[j] /= ntries;
    }

    System.out.printf("last: %s\n", Arrays.toString(FiltersPerformance.FILTER_WEIGHTS_DEFAULT));
    System.out.printf("mean: %s acceptRate=%.2f\n", Arrays.toString(mean), accepts
        / (double) ntries);

  }

  public static void searchForBestPrefNone(File dir) {
    double bestgoal = Double.MAX_VALUE;
    double bestp = 0;
    for (double pnone = 1.3; pnone > 0.1; pnone -= .05) {
      FiltersPerformance.FILTER_WEIGHTS_DEFAULT[0] = pnone;
      WriterFiltersPerformanceUtil test =
          new WriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir),
              new PngWriterGralFactory(FilterType.FILTER_ADAPTIVE_FULL));
      test.condOnly16bits = true;
      test.doit(1);
      int files = test.nFiles;
      double goal = test.performanceWAv;
      if (goal < bestgoal) {
        bestgoal = goal;
        bestp = pnone;
      }
      System.out.printf("pnone=%.6f goal=%.6f files=%d\n", pnone, goal, files);
    }

    System.out.printf("best pnone: %.6f p= %s \n", bestp,
        Arrays.toString(FiltersPerformance.FILTER_WEIGHTS_DEFAULT));

  }

  public static void showFilterNoneFromDir(File dir) {
    for (File f : TestSupport.getPngsFromDir(dir)) {
      PngReaderByte png = new PngReaderByte(f);
      int fnone = 0;
      for (int r = 0; r < png.imgInfo.rows; r++) {
        FilterType ft1 = ((IImageLineArray) png.readRow()).getFilterType();
        if (ft1.equals(FilterType.FILTER_NONE))
          fnone++;
      }
      png.end();
      System.out.printf("%s\t%.2f\n", f.getName(), fnone / (double) png.imgInfo.rows);
    }
  }

  public static void showCompressionWith2Filters(File f, FilterType ft1, FilterType ft2) {
    int clevel = 6;
    WriterFiltersPerformance test1 =
        new WriterFiltersPerformance(f, new PngWriterGralFactory(ft1, clevel));
    WriterFiltersPerformance test2 =
        new WriterFiltersPerformance(f, new PngWriterGralFactory(ft2, clevel));
    WriterFiltersPerformance test12 =
        new WriterFiltersPerformance(f, new PngWriterAlternateFactory(ft1, ft2, clevel));
    test1.doit();
    test2.doit();
    test12.doit();
    System.out.printf("%s\t%.3f\t%.3f\t%.3f\n", f, test1.getCompression(), test2.getCompression(),
        test12.getCompression());
  }

  public static void showCompressionWithJava(File file, boolean showFilename, boolean preferBetter) {
    List<File> list;
    if (file.isDirectory()) {
      list = TestSupport.getPngsFromDir(file);
    } else {
      list = new ArrayList<File>();
      list.add(file);
    }
    showCompressionWithJava(list, showFilename, preferBetter);
  }

  // WARNING: the preferBetter is used/resolved statically once!
  public static void showCompressionWithJava(List<File> files, boolean showFilename,
      boolean preferBetter) {
    int times[] = new int[3];
    ImageWriter iw = ImageIoUtils.getJavaImageWriter(preferBetter);
    StringBuilder msg = new StringBuilder();
    for (File f : files) {
      long size = f.length();
      File dest = null;
      try {

        // File dest = f.getName().equals("m3250.png") ? new File("C:\\temp\\m3250.png") : null;
        dest = File.createTempFile(f.getName(), "");
        BufferedImage img = ImageIO.read(f);
        OutputStream nos = dest != null ? new FileOutputStream(dest) : new NullOs();
        ImageOutputStream ios = ImageIO.createImageOutputStream(nos);
        long t0 = System.currentTimeMillis();
        iw.setOutput(ios);
        iw.write(img);
        long t1 = System.currentTimeMillis();
        ios.close();
        nos.close();
        times[getSizeType(f)] += (t1 - t0);
        dest.deleteOnExit();
        nos.close();
        size = dest != null ? dest.length() : ((NullOs) nos).getBytes();
      } catch (Exception e) {
        msg.append("Problems with file :" + f + " :" + e.getMessage() + "\n");
      }
      if (showFilename)
        System.out.printf("%s\t", f.getName());
      printCompressionForFile(dest, true);
      System.out.println("");
    }
    System.out.printf("\n%d\n%d\n%d\n\n", times[0], times[1], times[2]);
    System.out.println("iw:" + iw + " " + msg.toString());
  }


  public static void printCompressionForFile(File file, boolean includeFilterstats) {
    if (file == null)
      return;
    PngReaderByte png = new PngReaderByte(file);
    while (png.hasMoreRows())
      png.readRow();
    png.end();
    int[] filters = png.getChunkseq().getIdatSet().getFilterUseStat();
    long rawPixSize = png.imgInfo.getTotalRawBytes();
    long idatBytes = png.getChunkseq().getIdatBytes();
    System.out.printf("%.3f\t", idatBytes / (double) rawPixSize);
    if (includeFilterstats)
      System.out.printf("%s\t", formatFilterUse(filters, png.imgInfo.rows, true));
  }


  public static void computeSizeOriginal(File f) {
    if (f.isDirectory()) {
      for (File ff : TestSupport.getPngsFromDir(f))
        computeSizeOriginal(ff);
      return;
    }
    try {
      PngReaderDumb png = new PngReaderDumb(f);
      png.readAll();
      ImageInfo iminfo = png.getImageInfo();
      // name, total size, raw pizels size, idat size
      long rawPixelsSize = png.getImageInfo().getTotalRawBytes();
      System.out.printf("%s\t%s\t%d\t%d\t%d\n", f.getName(), png.toStringCompact(), png
          .getChunkseq().getBytesCount(), rawPixelsSize, png.getChunkseq().getIdatBytes());

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static void computeSpeedWithPngWriterPreserving(File f, int clevel) {
    WriterFiltersPerformanceUtil test =
        new WriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(f),
            new PngWriterPreserveFactory(clevel));
    System.out.print(test.doit(1));
    System.out.println(test.getSummary());
  }

  public static void computeSpeedWithPngWriterDefault(File dir, int clevel) {
    WriterFiltersPerformanceUtil test =
        new WriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir), new PngWriterGralFactory(
            FilterType.FILTER_DEFAULT, clevel));
    System.out.print(test.doit(1));
    System.out.println(test.getSummary());
  }

  public static void computeSpeedWithPngWriterNone(File dir, int clevel) {
    WriterFiltersPerformanceUtil test =
        new WriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir), new PngWriterGralFactory(
            FilterType.FILTER_NONE, clevel));
    System.out.print(test.doit(1));
    System.out.println(test.getSummary());
  }

  public void setShowFileName(boolean showFileName) {
    this.showFileName = showFileName;
  }

  public static void computeSpeedWithPngWriterAdaptative(File dir, FilterType f, int clevel) {
    WriterFiltersPerformanceUtil test =
        new WriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir), new PngWriterGralFactory(
            f, clevel));
    System.out.print(test.doit(1));
    System.out.println(test.getSummary());
  }

  public static void computeSpeedWithPngWriterSuperAdaptative(File dir, int clevel, int memoryKb,
      boolean useLz4) {
    WriterFiltersPerformanceUtil test =
        new WriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir),
            new PngWriterSuperAdaptiveFactory(useLz4, memoryKb, clevel));
    System.out.print(test.doit(1));
    System.out.println(test.getSummary());
  }


  private static void computeSpeedWithPngWriterDeflatePerLine(File dir, int clevel, boolean lz4) {
    WriterFiltersPerformanceUtil test =
        new WriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir),
            new PngWriterDeflatePerLine(clevel, lz4));
    System.out.print(test.doit(1));
    System.out.println(test.getSummary());
  }

  public static class PngWriterSuperAdaptiveFactory implements IPngWriterFactory {
    private int clevel;
    private boolean lz4;
    private int memoryKb;

    public PngWriterSuperAdaptiveFactory(boolean lz4, int memoryKb, int clevel) {
      this.lz4 = lz4;
      this.memoryKb = memoryKb;
      this.clevel = clevel;
    }

    public PngWriterSuperAdaptiveFactory() {
      this(true, -1, 6);
    }

    public PngWriter createPngWriter(OutputStream outputStream, ImageInfo imgInfo) {
      PngWriterHc pngw = new PngWriterHc(outputStream, imgInfo);
      pngw.getPixelsWriter().setDeflaterCompLevel(clevel);
      pngw.getPixelWriterMultiple().setUseLz4(lz4);
      pngw.getPixelWriterMultiple().setHintMemoryKb(memoryKb);
      // pngw.getPixelsWriter().setDeflaterStrategy(Deflater.DEFAULT_STRATEGY);
      return pngw;
    }
  }

  public static class PngWriterGralFactory implements IPngWriterFactory {
    private int clevel;
    private FilterType ftype;

    public PngWriterGralFactory() {
      this(FilterType.FILTER_DEFAULT);
    }

    public PngWriterGralFactory(FilterType f) {
      this(f, 6);
    }

    public PngWriterGralFactory(FilterType f, int clevel) {
      this.clevel = clevel;
      this.ftype = f;
    }

    public PngWriter createPngWriter(OutputStream outputStream, ImageInfo imgInfo) {
      PngWriter pngw = new PngWriter(outputStream, imgInfo);
      pngw.setFilterType(ftype);
      pngw.getPixelsWriter().setDeflaterCompLevel(clevel);
      // ((PixelsWriterDefault)pngw.getPixelsWriter()).tuneMemory(2.0);
      return pngw;
    }
  }

  // only for test, not very efficient
  public static class PngWriterDeflatePerLine implements IPngWriterFactory {
    private int clevel;
    private boolean lz4;

    public PngWriterDeflatePerLine(int clevel, boolean uselz4) {
      this.clevel = clevel;
      this.lz4 = uselz4;
    }

    public PngWriter createPngWriter(OutputStream outputStream, ImageInfo imgInfo) {
      PngWriter pngw = new PngWriter(outputStream, imgInfo) {
        @Override
        protected PixelsWriter createPixelsWriter(ImageInfo imginfo) {
          return new PixelsWriterDeflatePerLine(imgInfo, lz4, clevel);
        }
      };
      pngw.setCompLevel(clevel);
      return pngw;
    }
  }

  public static class PixelsWriterDeflatePerLine extends PixelsWriterDefault {
    Deflater def;
    private boolean lz4;
    byte[] bufdec = new byte[1000];
    DeflaterEstimatorLz4 lz4estim;

    public PixelsWriterDeflatePerLine(ImageInfo imgInfo, boolean lz4, int clevel) {
      super(imgInfo);
      this.lz4 = lz4;
      if (lz4)
        lz4estim = new DeflaterEstimatorLz4();
      else
        def = new Deflater(clevel);
    }

    @Override
    protected void decideCurFilterType() {
      double betterPerf = Double.MAX_VALUE;
      FilterType betterfilter = null;
      for (FilterType ft : FilterType.getAllStandard()) {
        byte[] f = filterRowWithFilterType(ft, rowb, rowbprev, rowbfilter);
        double c = compress(f, 0, buflen);
        // if(currentRow<4)
        // PngHelperInternal.debug(String.format("row=%d ft=%s c=%.3f",currentRow,ft.toString(),c));
        if (c < betterPerf) {
          betterfilter = ft;
          betterPerf = c;
        }
      }
      if (currentRow > 90 && currentRow < 100)
        PngHelperInternal.debug(String.format("row=%d bestft=%s ", currentRow, betterfilter));
      curfilterType = betterfilter;
    }

    private double compress(byte[] f, int off, int flen) {
      if (flen == 0)
        return 1.0;
      if (lz4) {
        int count = lz4estim.compressEstim(f, off, flen);
        return count / (double) flen;
      } else {
        def.reset();
        def.setInput(f, off, flen);
        def.finish();
        int count = 0;
        int d;
        while ((d = def.deflate(bufdec)) > 0) {
          count += d;
        }
        return count / (double) flen;
      }
    }

    @Override
    public void close() {
      if (def != null)
        def.end();
      super.close();
    }

  }

  // alternates two filtes
  public static class PngWriterAlternateFactory implements IPngWriterFactory {
    final int clevel;
    final FilterType ftype1;
    final FilterType ftype2;

    public PngWriterAlternateFactory(FilterType f1, FilterType f2, int clevel) {
      this.clevel = clevel;
      this.ftype1 = f1;
      this.ftype2 = f2;
    }

    public PngWriter createPngWriter(OutputStream outputStream, ImageInfo imgInfo) {
      PngWriter pngw = new PngWriter(outputStream, imgInfo) {
        @Override
        protected PixelsWriter createPixelsWriter(ImageInfo imginfo) {
          return new PixelsWriterDefault(imgInfo) {
            @Override
            protected void decideCurFilterType() {
              if ((currentRow % 2) == 0)
                curfilterType = ftype1;
              else
                curfilterType = ftype2;
            }
          };
        }
      };
      pngw.getPixelsWriter().setDeflaterCompLevel(clevel);
      return pngw;
    }
  }

 

  public static void main(String[] args) {
    Locale.setDefault(Locale.US);
    long t0 = System.currentTimeMillis();
    int clevel = 6;
    boolean useBetterJavaEncoder = false;
    // PngHelperInternal.setDebug(true);
    // File files = TestSupport.absFile(new File("..\\..\\priv\\imgsets\\2"));
    // File files = TestSupport.absFile(new File("..\\..\\priv\\imgsets\\2"));
    // File files = TestSupport.absFile(new File("..\\..\\priv\\imgsets\\1\\l\\l0090.png"));
    // File files = TestSupport.absFile(new File("..\\..\\priv\\imgsets\\1\\l\\l0130.png"));
    // File files = TestSupport.absFile(new File("..\\..\\priv\\imgsets\\1\\m\\m2230.png"));
    File files = TestSupport.absFile(new File("..\\..\\priv\\imgsets\\2\\l\\l1575.png"));
    SHOW_FILENAME_FORCE = !files.isDirectory();
    // computeSizeOriginal(files); // 1
    // computeSpeedWithPngWriterPreserving(files,clevel); //2
    // showCompressionWithJava(files, false,useBetterJavaEncoder); // 3
    // computeSpeedWithPngWriterDefault(files,clevel); //4
    // computeSpeedWithPngWriterNone(files, clevel);
    // computeSpeedWithPngWriterAdaptative(files, FilterType.FILTER_ADAPTIVE_FAST, clevel);
    computeSpeedWithPngWriterSuperAdaptative(files, clevel, 100, true);
    // computeSpeedWithPngWriterDeflatePerLine(files, clevel, false);
    // showCompressionWith2Filters(files, FilterType.FILTER_SUB, FilterType.FILTER_AVERAGE);
    // computeSpeedWithPngWriterNone(files,clevel); //2
    // computeSpeedWithPngWriterWithAdaptative(files,clevel); //2
    // computeSpeedWithX(files,PngWriterBands.class); //2
    System.out.println(System.currentTimeMillis() - t0);
  }



}
