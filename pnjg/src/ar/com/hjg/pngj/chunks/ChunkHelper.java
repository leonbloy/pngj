package ar.com.hjg.pngj.chunks;

// see http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
// http://www.w3.org/TR/PNG/#5Chunk-naming-conventions
// http://www.w3.org/TR/PNG/#table53
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import ar.com.hjg.pngj.PngHelper;
import ar.com.hjg.pngj.PngjException;

public class ChunkHelper {
	public static final String IHDR = "IHDR";
	public static final String PLTE = "PLTE";
	public static final String IDAT = "IDAT";
	public static final String IEND = "IEND";
	public static final String cHRM = "cHRM";// No Before PLTE and IDAT
	public static final String gAMA = "gAMA";// No Before PLTE and IDAT
	public static final String iCCP = "iCCP";// No Before PLTE and IDAT
	public static final String sBIT = "sBIT";// No Before PLTE and IDAT
	public static final String sRGB = "sRGB";// No Before PLTE and IDAT
	public static final String bKGD = "bKGD";// No After PLTE; before IDAT
	public static final String hIST = "hIST";// No After PLTE; before IDAT
	public static final String tRNS = "tRNS";// No After PLTE; before IDAT
	public static final String pHYs = "pHYs";// No Before IDAT
	public static final String sPLT = "sPLT";// Yes Before IDAT
	public static final String tIME = "tIME";// No None
	public static final String iTXt = "iTXt";// Yes None
	public static final String tEXt = "tEXt";// Yes None
	public static final String zTXt = "zTXt";// Yes None
	public static final byte[] b_IHDR = toBytes(IHDR);
	public static final byte[] b_PLTE = toBytes(PLTE);
	public static final byte[] b_IDAT = toBytes(IDAT);
	public static final byte[] b_IEND = toBytes(IEND);
	public static final byte[] b_cHRM = toBytes(cHRM);
	public static final byte[] b_gAMA = toBytes(gAMA);
	public static final byte[] b_iCCP = toBytes(iCCP);
	public static final byte[] b_sBIT = toBytes(sBIT);
	public static final byte[] b_sRGB = toBytes(sRGB);
	public static final byte[] b_bKGD = toBytes(bKGD);
	public static final byte[] b_hIST = toBytes(hIST);
	public static final byte[] b_tRNS = toBytes(tRNS);
	public static final byte[] b_pHYs = toBytes(pHYs);
	public static final byte[] b_sPLT = toBytes(sPLT);
	public static final byte[] b_tIME = toBytes(tIME);
	public static final byte[] b_iTXt = toBytes(iTXt);
	public static final byte[] b_tEXt = toBytes(tEXt);
	public static final byte[] b_zTXt = toBytes(zTXt);
	public static Set<String> KNOWN_CHUNKS_CRITICAL = PngHelper.asSet(IHDR, PLTE, IDAT, IEND);
	// ancillary known chunks, before PLTE and IDAT
	public static Set<String> KNOWN_CHUNKS_BEFORE_PLTE = PngHelper.asSet(cHRM, gAMA, iCCP, sBIT,
			sRGB);
	// ancillary known chunks, after PLTE , before IDAT
	public static Set<String> KNOWN_CHUNKS_AFTER_PLTE = PngHelper.asSet(bKGD, hIST, tRNS);
	// ancillary known chunks, before IDAT (before or after PLTE)
	public static Set<String> KNOWN_CHUNKS_BEFORE_IDAT = PngHelper.asSet(pHYs, sPLT);
	// ancillary known chunks, before or after IDAT
	public static Set<String> KNOWN_CHUNKS_ANYWHERE = PngHelper.asSet(tIME, iTXt, tEXt, zTXt);
	public static Set<String> KNOWN_CHUNKS_BEFORE_IDAT_ALL = PngHelper.unionSets(KNOWN_CHUNKS_BEFORE_PLTE,
			KNOWN_CHUNKS_AFTER_PLTE, KNOWN_CHUNKS_BEFORE_IDAT);
	public static Set<String> KNOWN_CHUNKS_ANCILLARY_ALL = PngHelper.unionSets(KNOWN_CHUNKS_BEFORE_IDAT_ALL,
			KNOWN_CHUNKS_ANYWHERE);

	public static boolean isKnown(String id) {
		return KNOWN_CHUNKS_CRITICAL.contains(id) || KNOWN_CHUNKS_ANCILLARY_ALL.contains(id);
	}

	public static byte[] toBytes(String x) {
		return x.getBytes(PngHelper.charsetLatin1);
	}

	public static String toString(byte[] x) {
		return new String(x, PngHelper.charsetLatin1);
	}

	public static boolean isCritical(String id) { // critical chunk ?
		// first letter is uppercase
		return (Character.isUpperCase(id.charAt(0)));
	}

	public static boolean isPublic(String id) { // public chunk?
		// second letter is uppercase
		return (Character.isUpperCase(id.charAt(1)));
	}

	public static boolean isSafeToCopy(String id) { // safe to copy?
		// fourth letter is lower case
		return (!Character.isUpperCase(id.charAt(3)));
	}

	public static boolean beforeIDAT(String id) { // only for ancillary
		if (KNOWN_CHUNKS_BEFORE_IDAT_ALL.contains(id))
			return true;
		return false;
	}

	public static boolean beforePLTE(String id) { // only for ancillary
		if (KNOWN_CHUNKS_BEFORE_PLTE.contains(id))
			return true;
		return false;
	}

	public static boolean admitsMultiple(String id) { // only for ancillary
		if (id.equals(sPLT) || id.equals(iTXt) || id.equals(tEXt) || id.equals(zTXt))
			return true;
		else
			return false;
	}

	public static int posNullByte(byte[] b) {
		for (int i = 0; i < b.length; i++)
			if (b[i] == 0)
				return i;
		return -1;
	}

	public static boolean shouldLoad(String id, ChunkLoadBehaviour behav) {
		if (isCritical(id))
			return true;
		boolean kwown = isKnown(id);
		switch (behav) {
		case LOAD_CHUNK_ALWAYS:
			return true;
		case LOAD_CHUNK_IF_SAFE:
			return kwown || isSafeToCopy(id);
		case LOAD_CHUNK_KNOWN:
			return kwown;
		case LOAD_CHUNK_NEVER:
			return false;
		}
		return false; // should not reach here
	}

	public final static byte[] compressBytes(byte[] ori, boolean compress) {
		return compressBytes(ori, 0, ori.length, compress);
	}

	public static byte[] compressBytes(byte[] ori, int offset, int len, boolean compress) {
		try {
			ByteArrayInputStream inb = new ByteArrayInputStream(ori, offset, len);
			InputStream in = compress ? inb : new InflaterInputStream(inb);
			ByteArrayOutputStream outb = new ByteArrayOutputStream();
			OutputStream out = compress ? new DeflaterOutputStream(outb) : outb;
			shovelInToOut(in, out);
			in.close();
			out.close();
			return outb.toByteArray();
		} catch (Exception e) {
			throw new PngjException(e);
		}
	}

	/**
	 * Shovels all data from an input stream to an output stream.
	 */
	private static void shovelInToOut(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int len;
		while ((len = in.read(buffer)) > 0) {
			out.write(buffer, 0, len);
		}
	}
}
