package ar.com.hjg.pngj.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;

public class WriterFiltersPerformance {
	public final File orig;
	private int times = 1;
	private long msecs;
	private double compression; // only pixels,

	private boolean writeToFile = false;

	private String extrainfo;
	ImageInfo imgInfo;
	private long idatbytesOri;

	public boolean condOnlyRGB8 = false;
	public boolean condOnlyAlpha = false;
	public boolean condOnly16bits = false;
	public boolean condOnlylessthan8bppOrPalette = false;
	public boolean condOnlyGray = false;
	private IPngWriterFactory writerFactory;

	public final static File tempFile = new File("C:/tmp/res.png");

	static {
		Locale.setDefault(Locale.US);
	}

	public WriterFiltersPerformance(File orig, IPngWriterFactory writerFactory) {
		this.orig = orig;
		this.writerFactory = writerFactory;
		init();
	}

	private void init() {
		if (!orig.canRead())
			throw new RuntimeException("Can't read " + orig);

	}

	/**
	 * return -1 if refused to run, elsewhere the msecs spend (only) on writing
	 * pixels
	 */
	private long reencode() {
		try {
			PngReader reader = new PngReader(orig);
			if (!shouldRun(reader.imgInfo)) {
				reader.close();
				return -1;
			}
			imgInfo = reader.imgInfo;

			OutputStream os = writeToFile ? new FileOutputStream(tempFile) : new NullOs();
			PngWriter writer = writerFactory.createPngWriter(os, reader.imgInfo);
			writer.copyChunksFrom(reader.getChunksList());
			long t0 = System.currentTimeMillis();
			for (int i = 0; i < reader.imgInfo.rows; i++) {
				writer.writeRow(reader.readRow());
			}
			long t1 = System.currentTimeMillis();
			reader.end();
			writer.end();
			os.close();
			idatbytesOri = reader.getChunkseq().getIdatBytes();
			extrainfo = writer.getDebuginfo() + " " + writer.getPixelsWriter().getFiltersUsed();
			compression = writer.getPixelsWriter().getCompression();
			if (writeToFile)
				PngHelperInternal.debug("Result in " + tempFile);
			return t1 - t0;
		} catch (Exception e) {
			throw new RuntimeException("error with " + orig, e);
		}

	}

	/*
	 * private boolean condOnlyRGB8=false; private boolean condOnlyAlpha=false;
	 * private boolean condOnly16bits=false; private boolean
	 * condOnlylessthan8bppOrPalette=false; private boolean condOnlyGray=false;
	 */
	private boolean shouldRun(ImageInfo imgInfo2) {
		if (condOnlyRGB8)
			return imgInfo2.bitDepth == 8 && imgInfo2.channels == 3;
		if (condOnlyAlpha)
			return imgInfo2.alpha;
		if (condOnly16bits)
			return imgInfo2.bitDepth == 16;
		if (condOnlyGray)
			return imgInfo2.greyscale;
		if (condOnlylessthan8bppOrPalette)
			return imgInfo2.indexed || imgInfo2.bitDepth < 8;
		return true;
	}

	/** returns false if it didint run */
	public boolean doit() {
		msecs = 0;
		for (int i = 0; i < times; i++) {
			long m = reencode();
			if (m < 0)
				return false;
			msecs += m;
		}
		if (times > 1)
			msecs /= (times);
		return true;
	}

	public void setWriteToFile(boolean writeToFile) {
		this.writeToFile = writeToFile;
	}

	public long getMsecs() {
		return msecs;
	}

	public void setRepeat(int repeat) {
		this.times = repeat;
	}

	public String getSumm(boolean withFilename) { // filename, compression, extra info
		// extrainfo =
		// String.format("(%d\t%d\t%d)\t",orig.length(),imgInfo.rows*(imgInfo.bytesPerRow+1),idatbytesOri);
		String r = withFilename ? orig.getName() + "\t" : "";
		return r + String.format("%.3f\t%s\n", compression, extrainfo);
	}

	public static String processDir(File dir, IPngWriterFactory writerFactory, int repeat, boolean showFileName) {
		StringBuilder sb = new StringBuilder();
		for (File f : dir.listFiles()) {
			if (!f.getName().endsWith("png"))
				continue;
			String res = processFile(f, writerFactory, repeat, showFileName);
			sb.append(res);
		}
		return sb.toString();
	}

	public static String processFile(File f, IPngWriterFactory writerFactory, int repeat, boolean showFileName) {
		WriterFiltersPerformance tp = new WriterFiltersPerformance(f, writerFactory);
		tp.setRepeat(repeat);
		tp.doit();
		return tp.getSumm(showFileName);
	}

	public String getExtraInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public double getCompression() {
		return compression;
	}

}
