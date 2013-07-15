package ar.com.hjg.pngj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.PngChunk;

public class TestSupport {

	// WARNING: showXXXX methods are also for machine consumption

	public static Random rand = new Random();

	public static final String PNG_TEST_STRIPES = "resources/test/stripes.png";
	public static final String PNG_TEST_TESTG2 = "resources/test/testg2.png";
	public static final String PNG_TEST_TESTG2I= "resources/test/testg2i.png";

	public static final String PNG_TEST_BAD_MISSINGIDAT ="resources/test/bad_missingidat.png";
	
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

	/**
	 * First byte is the filter type, nbytes is the valid content (including
	 * filter byte) This shows at most 9 bytes
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

	public static InputStream istream(String f) {
		return istream(new File(f));
	}

	public static OutputStream ostream(String f) {
		return ostream(new File(f));
	}

	public static File getPngTestSuiteDir() {
		return new File("resources/testsuite1");
	}

	public static InputStream istream(File f) {
		try {
			return new FileInputStream(f);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static OutputStream ostream(File f) {
		try {
			return new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static File getTempDir() {
		File t = new File("resources/test/temp");
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
		long crc1 = PngHelperInternal.getCrctestVal(png1);
		long crc2 = PngHelperInternal.getCrctestVal(png2);
		TestCase.assertEquals("different crcs " + image1 + "=" + crc1 + " " + image2 + "=" + crc2, crc1, crc2);
	}

	
	
	public static void testCrcEquals(File image1, long crc) {
		PngReader png1 = new PngReader(image1);
		PngHelperInternal.initCrcForTests(png1);
		png1.readRow(png1.imgInfo.rows - 1);
		png1.end();
		long crc1 = PngHelperInternal.getCrctestVal(png1);
		TestCase.assertEquals(crc1, crc);
	}
	
	public static File addSuffixToName(File orig, String suffix) {
		String x = orig.getPath();
		x = x.replaceAll("\\.png$", "");
		return new File(x + suffix + ".png");
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

	public static List<PngChunk> getChunkById(String id,Collection<PngChunk> chunks) {
		ArrayList<PngChunk> list = new ArrayList<PngChunk>();
		for(PngChunk c:chunks) if(c.id.equals(id)) list.add(c);
		return list;
	}
	
	/** does not include IDAT */
	public static ChunksList readAllChunks(File file,boolean includeIdat) {
		PngReader pngr = new PngReader(file);
		pngr.setChunksToSkip();
		pngr.getChunkseq().setIncludeNonBufferedChunks(includeIdat);
		pngr.readSkippingAllRows();
		pngr.end();
		return pngr.getChunksList();
	}

	
}
