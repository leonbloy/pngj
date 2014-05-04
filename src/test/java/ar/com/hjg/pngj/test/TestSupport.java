package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import junit.framework.TestCase;
import ar.com.hjg.pngj.BufferedStreamFeeder;
import ar.com.hjg.pngj.ChunkSeqReader;
import ar.com.hjg.pngj.ChunkSeqReaderSimple;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineArray;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngReaderByte;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.PngjInputException;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.PngChunk;

/**
 * Methods of this class are designed for debug and testing PNGJ library, they are not optimized
 */
public class TestSupport {

	// WARNING: showXXXX methods are also for machine consumption

	public static Random rand = new Random();

	static {
		Locale.setDefault(Locale.US);
	}

	private static File resourcesDir = null;

	// this files should be accessible from the classpath 
	public static final String PNG_TEST_STRIPES = "test/stripes.png";
	public static final String PNG_TEST_STRIPES2 = "test/stripes2.png"; 
	public static final String PNG_TEST_BADCRC = "test/testg1badcrc.png"; 
	public static final String PNG_TEST_BADTRUNCATED = "test/bad_truncated.png"; 
	public static final String PNG_TEST_TESTG2 = "test/testg2.png";
	public static final String PNG_TEST_TESTG2I = "test/testg2i.png";//interlaced
	public static final String PNG_TEST_IDATTRICKY = "test/stripes0idat.png"; // Correct buttricky: several 0 length idats

	public static final String PNG_TEST_BAD_MISSINGIDAT = "test/bad_missingidat.png";

	public static String showChunks(List<PngChunk> chunks) {
		StringBuilder sb = new StringBuilder();
		for (PngChunk chunk : chunks) {
			sb.append(showChunk(chunk)).append(" ");
		}
		return sb.toString();
	}

	public static void feedFromStreamTest(ChunkSeqReader as, String fs) {
		feedFromStreamTest(as, fs, -1);
	}

	public static int randBufSize() {
		return rand.nextBoolean() ? rand.nextInt(8) + 1 : rand.nextInt(30) + 80000; // very small or very big
	}

	public static void feedFromStreamTest(ChunkSeqReader as, String fs, int bufferSize) {
		if (bufferSize < 1)
			bufferSize = randBufSize();
		BufferedStreamFeeder bf = new BufferedStreamFeeder(TestSupport.istream(fs), bufferSize);
		while (!as.isDone()) {
			if (!bf.hasMoreToFeed())
				throw new PngjInputException("premature ending");
			if (bf.feed(as) == 0)
				break;
		}
		bf.close();
	}

	public static String showChunk(PngChunk chunk) {
		return chunk == null ? "null" : chunk.id + "[" + chunk.getLen() + "]";
	}

	public static String showChunksRaw(List<ChunkRaw> chunks) {
		StringBuilder sb = new StringBuilder();
		for (ChunkRaw chunk : chunks) {
			sb.append(showChunkRaw(chunk)).append(" ");
		}
		return sb.toString();
	}

	public static String showChunkRaw(ChunkRaw chunk) {
		return chunk == null ? "null" : chunk.id + "[" + chunk.len + "]";
	}

	public static List<ChunkRaw> chunkListToChunksRaw(List<PngChunk> chunks) {
		List<ChunkRaw> cr=new ArrayList<ChunkRaw>();
		for(PngChunk c:chunks) cr.add(c.getRaw());
		return cr;
	}
	
	public static String showFilters(File pngr, int maxgroups, boolean usenewlines) {
		return showFilters(pngr, maxgroups, usenewlines,false,false);
	}
	
	/** show detailed filter information (grouped by consecutive rows) eg: PAETH:337(103) means 103 rows of type PAETH starting from row 337 */
	public static String showFilters(File pngr, int maxgroups, boolean usenewlines, boolean showSumm,
			boolean showSummPercent) {
		if (maxgroups == 0)
			usenewlines = false;
		if (maxgroups < 0)
			maxgroups = Integer.MAX_VALUE;
		int[] types = new int[5];
		PngReaderByte png = new PngReaderByte(pngr);
		StringBuilder sb = new StringBuilder();
		int[] ft = new int[png.imgInfo.rows + 1];
		for (int r = 0; r < png.imgInfo.rows; r++) {
			ft[r] = ((IImageLineArray) png.readRow()).getFilterType().val;
			types[ft[r]]++;
		}
		png.end();
		ft[png.imgInfo.rows] = -1; // dummy end value to ease computation
		if (showSummPercent) {
			for (int i = 0; i <= 4; i++) {
				types[i] = (int) ((types[i] * 100 + png.imgInfo.rows / 2) / png.imgInfo.rows);
			}
		}
		if (showSumm || showSummPercent)
			sb.append(Arrays.toString(types) + (usenewlines ? "\n" : "\t"));
		// groups
		if (maxgroups != 0) {
			int contgroups = 0;
			int r0 = 0;
			for (int r = 1; r < ft.length; r++) {
				if (ft[r] != ft[r - 1]) { // found group
					sb.append(String.format("%s:%d(%d)", FilterType.getByVal(ft[r - 1]),r0, r - r0)).append(
							usenewlines ? "\n" : " ");
					contgroups++;
					r0 = r;
					if (contgroups >= maxgroups && r < ft.length - 1) {
						sb.append("...").append(usenewlines ? "\n" : " ");
						break;
					}
				}
			}
		}
		return sb.toString().trim().replaceAll("FILTER_", "");
	}

	/**
	 * First byte is the filter type, nbytes is the valid content (including filter byte) This shows at most 9 bytes
	 */
	public static String showRow(byte[] row, int nbytes, int rown, int dx, int ox) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("r=%d", rown));
		if (dx != 1 || ox != 0)
			sb.append(String.format("(dx:%d,ox:%d)", dx, ox));
		int n = nbytes - 1;
		if (n > 9)
			n = 9;
		sb.append(n == nbytes - 1 ? String.format("[") : String.format(" b(%d/%d)=[", n, nbytes - 1));
		for (int i = 0; i <= n; i++) {
			sb.append(String.format("%3d", row[i] & 0xff));
			sb.append(i == 0 ? "|" : (i < n ? " " : ""));
		}
		return sb.append("]").toString();
	}

	public static String showLine(IImageLineArray line) {
		StringBuilder sb = new StringBuilder();

		int n = line.getSize();
		if (n > 9)
			n = 9;
		sb.append(n == line.getSize() ? String.format("[") : String.format(" b(%d/%d)=[", n, line.getSize()));
		for (int i = 0; i < n; i++) {
			sb.append(String.format("%3d", line.getElem(i)));
			sb.append(i < n - 1 ? " " : "");
		}
		return sb.append("]").toString();
	}

	public static String showRow(byte[] row, int nbytes, int rown) {
		return showRow(row, nbytes, rown, 1, 0);
	}

	/**
	 * the location (if relative) is realtive to the resources dir
	 * 
	 * @param f
	 * @return
	 */
	public static InputStream istream(String f) {
		return istream(new File(f));
	}

	/**
	 * the location (if relative) is realtive to the resources dir
	 * 
	 * @param f
	 * @return
	 */
	public static OutputStream ostream(String f) {
		return ostream(new File(f));
	}

	/**
	 * the location (if relative) is realtive to the resources dir
	 * 
	 * @param f
	 * @return
	 */
	public static InputStream istream(File f) {
		try {
			if (!f.isAbsolute())
				f = new File(getResourcesDir(), f.getPath());
			return new FileInputStream(f);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static File absFile(File f) {
		if (!f.isAbsolute())
			f = new File(getResourcesDir(), f.getPath());
		return f;
	}

	public static File absFile(String x) {
		File f = new File(x);
		if (!f.isAbsolute())
			f = new File(getResourcesDir(), f.getPath());
		return f;
	}

	/**
	 * the location (if relative) is realtive to the resources dir
	 * 
	 * @param f
	 * @return
	 */
	public static OutputStream ostream(File f) {
		try {
			if (!f.isAbsolute())
				f = new File(getResourcesDir(), f.getPath());
			return new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The resources dir for the tests should include PNG_TEST_STRIPES (and testsuite1) Typically target/tests-classes
	 * */
	public static File getResourcesDir() {
		if (resourcesDir == null) {
			String tokenfile = PNG_TEST_STRIPES;
			URL u = TestSupport.class.getClassLoader().getResource(tokenfile);
			if (u == null)
				throw new PngjException(
						PNG_TEST_STRIPES
								+ " not found in classpath, this is required in order to locate the resources dir for the PNGJ tests to run");
			File f;
			try {
				f = (new File(u.toURI())).getParentFile().getParentFile();
			} catch (URISyntaxException e) {
				throw new PngjException(e);
			}
			resourcesDir = f;
		}
		return resourcesDir;
	}

	public static List<File> listPngFromDir(File dir) {
		dir = absFile(dir);
		ArrayList<File> pngs = new ArrayList<File>();
		for (String f : dir.list()) {
			if (f.toLowerCase().endsWith(".png"))
				pngs.add(new File(dir, f));
		}
		return pngs;
	}

	public static File getPngTestSuiteDir() {
		return new File(getResourcesDir(), "testsuite1");
	}

	public static File getTempDir() {
		File t = new File(getResourcesDir(), "test/temp");
		if (!t.isDirectory())
			throw new RuntimeException("missing test resource dir: " + t);
		return t;
	}

	public static void cleanAll() {
		File t = getTempDir();
		for (File x : t.listFiles()) {
			if (x.getName().endsWith("png")) {
				boolean ok = x.delete();
				if (!ok)
					System.err.println("could not remove " + x);
			}
		}
	}

	public static void testSameCrc(File image1, File image2) {
		PngReader png1 = new PngReader(image1);
		PngHelperInternal.initCrcForTests(png1);
		PngReader png2 = new PngReader(image2);
		PngHelperInternal.initCrcForTests(png2);
		TestCase.assertEquals("Cannot compare, one is interlaced, the other not:", png1.isInterlaced(),
				png2.isInterlaced());
		TestCase.assertEquals("Image are of different type", png1.imgInfo, png2.imgInfo);
		png1.readRow(png1.imgInfo.rows - 1);
		png2.readRow(png2.imgInfo.rows - 1);
		png1.end();
		png2.end();
		long crc1 = PngHelperInternal.getDigest(png1);
		long crc2 = PngHelperInternal.getDigest(png2);
		TestCase.assertEquals("different crcs " + image1 + "=" + crc1 + " " + image2 + "=" + crc2, crc1, crc2);
	}

	public static void testCrcEquals(File image1, long crc) {
		PngReader png1 = new PngReader(image1);
		PngHelperInternal.initCrcForTests(png1);
		png1.readRow(png1.imgInfo.rows - 1);
		png1.end();
		long crc1 = PngHelperInternal.getDigest(png1);
		TestCase.assertEquals(crc1, crc);
	}

	public static File addSuffixToName(File orig, String suffix) {
		String x = orig.getPath();
		x = x.replaceAll("\\.png$", "");
		return new File(x + suffix + ".png");
	}

	public static String addSuffixToName(String orig, String suffix) {
		return orig.replaceAll("\\.png$", "") + ".png";
	}

	public static class NullOutputStream extends OutputStream {
		private int cont = 0;

		@Override
		public void write(int arg0) throws IOException {
			// nothing!
			cont++;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			cont += len;
		}

		@Override
		public void write(byte[] b) throws IOException {
			cont += b.length;
		}

		public int getCont() {
			return cont;
		}

	}

	public static NullOutputStream createNullOutputStream() {
		return new NullOutputStream();
	}

	public static List<PngChunk> getChunkById(String id, Collection<PngChunk> chunks) {
		ArrayList<PngChunk> list = new ArrayList<PngChunk>();
		for (PngChunk c : chunks)
			if (c.id.equals(id))
				list.add(c);
		return list;
	}

	/** does not include IDAT */
	public static ChunksList readAllChunks(File file, boolean includeIdat) {
		PngReader pngr = new PngReader(file);
		pngr.setChunksToSkip();
		pngr.getChunkseq().setIncludeNonBufferedChunks(includeIdat);
		pngr.readSkippingAllRows();
		pngr.end();
		return pngr.getChunksList();
	}

	public static File getTmpFile(String suffix) {
		return new File(getTempDir(), "temp" + suffix + ".png");
	}

	public static PngWriter prepareFileTmp(File f, ImageInfo imi) {
		PngWriter png = new PngWriter(f, imi, true);
		return png;
	}

	/** creates a simple PNG image 32x32 RGB8 for test */
	public static PngWriter prepareFileTmp(File f, boolean palette) {
		return prepareFileTmp(f, new ImageInfo(32, 32, 8, false, false, palette));
	}

	public static IImageLine generateNoiseLine(ImageInfo imi) { // byte format!
		ImageLineByte line = new ImageLineByte(imi);
		Random r = new Random();
		byte[] scanline = line.getScanline();
		r.nextBytes(scanline);
		return line;
	}

	public static PngWriter prepareFileTmp(File f) {
		return prepareFileTmp(f, false);
	}

	public static void endFileTmp(PngWriter png) { // writes dummy data
		ImageLineInt imline = new ImageLineInt(png.imgInfo);
		for (int i = 0; i < png.imgInfo.rows; i++)
			png.writeRow(imline);
		png.end();
	}

	public static List<File> getPngsFromDir(File dirOrFile, boolean recurse) {
		List<File> li = new ArrayList<File>();
		if (dirOrFile.isDirectory()) {
			File[] files = dirOrFile.listFiles();
			Arrays.sort(files); // to guarantee predecible order
			for (File f : files) {
				if (recurse && f.isDirectory())
					li.addAll(getPngsFromDir(f, true));
				if (f.getName().toLowerCase().endsWith(".png"))
					li.add(f);
			}
		} else
			li.add(dirOrFile);// not a dir, but a file
		return li;
	}

	public static List<File> getPngsFromDir(File dir) {
		return getPngsFromDir(dir, true);
	}

	public static String getChunksSummary(String f) {
		return getChunksSummary(f,true);
	}
	
	// if fast=true does not check CRCs
	public static String getChunksSummary(String f,boolean fast) {
		ChunkSeqReaderSimple c = new ChunkSeqReaderSimple(fast);
		c.feedFromInputStream(TestSupport.istream(f));
		return TestSupport.showChunksRaw(c.getChunks());
	}

}
