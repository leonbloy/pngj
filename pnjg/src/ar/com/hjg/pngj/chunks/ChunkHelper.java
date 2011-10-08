package ar.com.hjg.pngj.chunks;

// see http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
// http://www.w3.org/TR/PNG/#5Chunk-naming-conventions
// http://www.w3.org/TR/PNG/#table53
import java.util.Set;

import ar.com.hjg.pngj.PngHelper;

public class ChunkHelper {
	public static final String IHDR_TEXT = "IHDR";
	public static final String PLTE_TEXT = "PLTE";
	public static final String IDAT_TEXT = "IDAT";
	public static final String IEND_TEXT = "IEND";
	public static final String cHRM_TEXT = "cHRM";// No Before PLTE and IDAT
	public static final String gAMA_TEXT = "gAMA";// No Before PLTE and IDAT
	public static final String iCCP_TEXT = "iCCP";// No Before PLTE and IDAT
	public static final String sBIT_TEXT = "sBIT";// No Before PLTE and IDAT
	public static final String sRGB_TEXT = "sRGB";// No Before PLTE and IDAT
	public static final String bKGD_TEXT = "bKGD";// No After PLTE; before IDAT
	public static final String hIST_TEXT = "hIST";// No After PLTE; before IDAT
	public static final String tRNS_TEXT = "tRNS";// No After PLTE; before IDAT
	public static final String pHYs_TEXT = "pHYs";// No Before IDAT
	public static final String sPLT_TEXT = "sPLT";// Yes Before IDAT
	public static final String tIME_TEXT = "tIME";// No None
	public static final String iTXt_TEXT = "iTXt";// Yes None
	public static final String tEXt_TEXT = "tEXt";// Yes None
	public static final String zTXt_TEXT = "zTXt";// Yes None
	public static final byte[] IHDR = toBytes(IHDR_TEXT);
	public static final byte[] PLTE = toBytes(PLTE_TEXT);
	public static final byte[] IDAT = toBytes(IDAT_TEXT);
	public static final byte[] IEND = toBytes(IEND_TEXT);
	public static final byte[] cHRM = toBytes(cHRM_TEXT);
	public static final byte[] gAMA = toBytes(gAMA_TEXT);
	public static final byte[] iCCP = toBytes(iCCP_TEXT);
	public static final byte[] sBIT = toBytes(sBIT_TEXT);
	public static final byte[] sRGB = toBytes(sRGB_TEXT);
	public static final byte[] bKGD = toBytes(bKGD_TEXT);
	public static final byte[] hIST = toBytes(hIST_TEXT);
	public static final byte[] tRNS = toBytes(tRNS_TEXT);
	public static final byte[] pHYs = toBytes(pHYs_TEXT);
	public static final byte[] sPLT = toBytes(sPLT_TEXT);
	public static final byte[] tIME = toBytes(tIME_TEXT);
	public static final byte[] iTXt = toBytes(iTXt_TEXT);
	public static final byte[] tEXt = toBytes(tEXt_TEXT);
	public static final byte[] zTXt = toBytes(zTXt_TEXT);
	public static Set<String> KNOWN_CHUNKS_CRITICAL = PngHelper.asSet(IHDR_TEXT, PLTE_TEXT,
			IDAT_TEXT, IEND_TEXT);
	// ancillary known chunks, before PLTE and IDAT
	public static Set<String> KNOWN_CHUNKS_BEFORE_PLTE = PngHelper.asSet(cHRM_TEXT,
			gAMA_TEXT, iCCP_TEXT, sBIT_TEXT, sRGB_TEXT);
	// ancillary known chunks, after PLTE , before IDAT
	public static Set<String> KNOWN_CHUNKS_AFTER_PLTE = PngHelper.asSet(bKGD_TEXT,
			hIST_TEXT, tRNS_TEXT);
	// ancillary known chunks, before IDAT (before or after PLTE)
	public static Set<String> KNOWN_CHUNKS_BEFORE_IDAT = PngHelper.asSet(pHYs_TEXT,
			sPLT_TEXT);
	// ancillary known chunks, before or after IDAT
	public static Set<String> KNOWN_CHUNKS_ANYWHERE = PngHelper.asSet(tIME_TEXT, iTXt_TEXT,
			tEXt_TEXT, zTXt_TEXT);
	public static Set<String> KNOWN_CHUNKS_BEFORE_IDAT_ALL = PngHelper.unionSets(
			KNOWN_CHUNKS_BEFORE_PLTE, KNOWN_CHUNKS_AFTER_PLTE, KNOWN_CHUNKS_BEFORE_IDAT);
	public static Set<String> KNOWN_CHUNKS_ANCILLARY_ALL = PngHelper.unionSets(
			KNOWN_CHUNKS_BEFORE_IDAT_ALL, KNOWN_CHUNKS_ANYWHERE);

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
		if (id.equals(sPLT_TEXT) || id.equals(iTXt_TEXT) || id.equals(tEXt_TEXT)
				|| id.equals(zTXt_TEXT))
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
}
