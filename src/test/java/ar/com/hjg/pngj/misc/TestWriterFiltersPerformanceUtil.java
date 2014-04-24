package ar.com.hjg.pngj.misc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Random;
import java.util.zip.Deflater;

import javax.imageio.ImageIO;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineArray;
import ar.com.hjg.pngj.IPngWriterFactory;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.NullOs;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngReaderByte;
import ar.com.hjg.pngj.PngReaderDumb;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjInputException;
import ar.com.hjg.pngj.pixels.FiltersPerformance;
import ar.com.hjg.pngj.pixels.PixelsWriter;
import ar.com.hjg.pngj.pixels.PixelsWriterDefault;
import ar.com.hjg.pngj.pixels.PixelsWriterMultiple;
import ar.com.hjg.pngj.test.TestSupport;

public class TestWriterFiltersPerformanceUtil {
	private Collection<File> orig;
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

	public TestWriterFiltersPerformanceUtil(Collection<File> orig, IPngWriterFactory writerFactory) {
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
			TestWriterFiltersPerformance p = new TestWriterFiltersPerformance(f, writerFactory);
			p.setRepeat(nrepeat);
			p.condOnlyRGB8 = this.condOnlyRGB8;
			p.condOnlyAlpha = this.condOnlyAlpha;
			p.condOnly16bits = this.condOnly16bits;
			p.condOnlylessthan8bppOrPalette = this.condOnlylessthan8bppOrPalette;
			p.condOnlyGray = this.condOnlyGray;
			boolean done = p.doit();
			if (!done)
				continue;
			nFiles++;
			sb.append(p.getSumm(SHOW_FILENAME_FORCE != null ? SHOW_FILENAME_FORCE.booleanValue() : showFileName));
			double msecs = p.getMsecs();
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

	public String getSummary() {
		return desc
				+ String.format("\t%.4f\t%.4f\t%.4f\t%d\t%.1f\t%s", 0.0, performanceAv, performanceWAv, (int) timeAv,
						timeWeighted, extrainfo);
	}

	public void setDesc(String string) {
		desc = string;

	}

	public static TestWriterFiltersPerformanceUtil createFromDir(File dir, IPngWriterFactory writerFactory) {
		return new TestWriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir), writerFactory);

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
			inf.append(String.format("%s %d ", file.getName(), pngr.imgInfo.rows * pngr.imgInfo.bytesPerRow));
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
			for (int f : filters) {
				inf.append(String.format("%d,", f * 100 / pngr.imgInfo.rows));
			}
			inf.setLength(inf.length() - 1);
			System.out.println(inf);
			file.delete();
			filenew.renameTo(file);
		} catch (PngjInputException e) {
			// System.err.println("error with " + file +" " + e);
			file.renameTo(new File(file.getAbsolutePath() + ".bad"));
		}
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
			while (def.deflate(buf) > 0)
				;
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
			//writer.getPixelsWriter().setDeflaterStrategy(Deflater.DEFAULT_STRATEGY);
			writer.getPixelsWriter().setDeflaterCompLevel(clevel);
			writer.setIdatMaxSize(1000 * 1000);
			return writer;
		}
	}

	static String DIR_IMAGESET = "d:\\hjg\\wokspace\\Varios\\resources\\pngimages";

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
			TestWriterFiltersPerformanceUtil test = new TestWriterFiltersPerformanceUtil(
					TestSupport.getPngsFromDir(dir), new PngWriterGralFactory(FilterType.FILTER_ADAPTIVE_FULL));
			test.condOnlyRGB8 = true;
			test.doit(1);
			int files = test.nFiles;
			double goal = test.performanceWAv;
			double ganancia = lastgoal == Double.MAX_VALUE ? 1.0 : lastgoal - goal;
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
			System.out.printf("t=%d/%d goal=%.6f gan=%.6f i=%d %s arr=%s files=%d\n", tries, ntries, lastgoal,
					ganancia, which, (accepted ? "+" : "-"),
					Arrays.toString(FiltersPerformance.FILTER_WEIGHTS_DEFAULT), files);
		}
		for (int j = 0; j < 5; j++) {
			mean[j] /= ntries;
		}

		System.out.printf("last: %s\n", Arrays.toString(FiltersPerformance.FILTER_WEIGHTS_DEFAULT));
		System.out.printf("mean: %s acceptRate=%.2f\n", Arrays.toString(mean), accepts / (double) ntries);

	}

	public static void searchForBestPrefNone(File dir) {
		double bestgoal = Double.MAX_VALUE;
		double bestp = 0;
		for (double pnone = 1.3; pnone > 0.1; pnone -= .05) {
			FiltersPerformance.FILTER_WEIGHTS_DEFAULT[0] = pnone;
			TestWriterFiltersPerformanceUtil test = new TestWriterFiltersPerformanceUtil(
					TestSupport.getPngsFromDir(dir), new PngWriterGralFactory(FilterType.FILTER_ADAPTIVE_FULL));
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

	public static void showCompressionWithJava(File f, boolean showFilename) {
		if (f.isDirectory()) {
			for (File ff : TestSupport.getPngsFromDir(f))
				showCompressionWithJava(ff, showFilename);
			return;
		}
		try {
			BufferedImage img = ImageIO.read(f);
			NullOs nos = new NullOs();
			long t0 = System.currentTimeMillis();
			ImageIO.write(img, "PNG", nos);
			long t1 = System.currentTimeMillis();
			long size = nos.getBytes();
			if (showFilename)
				System.out.printf("%s\t%d\t%d\n", f.getName(), size, t1 - t0);
			else
				System.out.printf("%d\t%d\n", size, t1 - t0);
		} catch (IOException e) {
			System.out.printf("%s %d %d %s\n", f.getName(), 0, 0, e.getMessage());
		}
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
			long rawPixelsSize = (png.getImageInfo().bytesPerRow + 1) * png.getImageInfo().rows;
			System.out.printf("%s\t%s\t%d\t%d\t%d\n", f.getName(), iminfo.toStringBrief(), png.getChunkseq()
					.getBytesCount(), rawPixelsSize, png.getChunkseq().getIdatBytes());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void computeSpeedWithPngWriterPreserving(File f, int clevel) {
		TestWriterFiltersPerformanceUtil test = new TestWriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(f),
				new PngWriterPreserveFactory(clevel));
		System.out.println(test.doit(1));
		System.out.println(test.getSummary());
	}

	public static void computeSpeedWithPngWriterDefault(File dir, int clevel) {
		TestWriterFiltersPerformanceUtil test = new TestWriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir),
				new PngWriterGralFactory(FilterType.FILTER_DEFAULT, clevel));
		System.out.println(test.doit(1));
		System.out.println(test.getSummary());
	}

	public static void computeSpeedWithPngWriterNone(File dir, int clevel) {
		TestWriterFiltersPerformanceUtil test = new TestWriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir),
				new PngWriterGralFactory(FilterType.FILTER_NONE, clevel));
		System.out.println(test.doit(1));
		System.out.println(test.getSummary());
	}

	public void setShowFileName(boolean showFileName) {
		this.showFileName = showFileName;
	}

	public static void computeSpeedWithPngWriterAdaptative(File dir, FilterType f, int clevel) {
		TestWriterFiltersPerformanceUtil test = new TestWriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir),
				new PngWriterGralFactory(f, clevel));
		System.out.println(test.doit(1));
		System.out.println(test.getSummary());
	}

	public static void computeSpeedWithPngWriterSuperAdaptative(File dir, int clevel, long memory, boolean useLz4) {
		TestWriterFiltersPerformanceUtil test = new TestWriterFiltersPerformanceUtil(TestSupport.getPngsFromDir(dir),
				new PngWriterSuperAdaptiveFactory(useLz4, memory, clevel));
		System.out.println(test.doit(1));
		System.out.println(test.getSummary());
	}

	public static class PngWriterSuperAdaptiveFactory implements IPngWriterFactory {
		private int clevel;
		private boolean lz4;
		private long memTarget;

		public PngWriterSuperAdaptiveFactory(boolean lz4, long memoryTarget, int clevel) {
			this.lz4 = lz4;
			this.memTarget = memoryTarget;
			this.clevel = clevel;
		}

		public PngWriterSuperAdaptiveFactory() {
			this(true, -1, 6);
		}

		public PngWriter createPngWriter(OutputStream outputStream, ImageInfo imgInfo) {
			PngWriter pngw = new PngWriter(outputStream, imgInfo) {
				@Override
				protected PixelsWriter createPixelsWriter(ImageInfo imginfo) {
					PixelsWriterMultiple pw = new PixelsWriterMultiple(imgInfo);
					pw.setUseLz4(lz4);
					return pw;
				}

			};
			pngw.getPixelsWriter().setDeflaterCompLevel(clevel);
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
			return pngw;
		}
	}

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);
		long t0 = System.currentTimeMillis();
		int clevel = 6;
		//File files = TestSupport.absFile(new File("..\\..\\priv\\imgsets\\1"));
		File files = TestSupport.absFile(new File("..\\..\\priv\\imgsets\\1"));
		SHOW_FILENAME_FORCE = !files.isDirectory();
		//computeSizeOriginal(files); // 1
		// computeSpeedWithPngWriterPreserving(files,clevel); //2	
		// showCompressionWithJava(files, false); // 3
		//computeSpeedWithPngWriterDefault(files,clevel); //4
		//computeSpeedWithPngWriterDefault(files,clevel); //4
		computeSpeedWithPngWriterAdaptative(files, FilterType.FILTER_ADAPTIVE_FULL, clevel);
		//computeSpeedWithPngWriterSuperAdaptative(files, clevel, -1, false);
		// computeSpeedWithPngWriterNone(files,clevel); //2
		// computeSpeedWithPngWriterWithAdaptative(files,clevel); //2
		// computeSpeedWithX(files,PngWriterBands.class); //2
		System.out.println(System.currentTimeMillis() - t0);
	}

}
