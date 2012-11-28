package ar.com.hjg.pngj;

import java.util.Arrays;

import ar.com.hjg.pngj.ImageLineHelper.ImageLineStats;

/**
 * Lightweight wrapper for an image scanline, used for read and write.
 * <p>
 * This object can be (usually it is) reused while iterating over the image lines.
 * <p>
 * See <code>scanline</code> field, to understand the format.
 */
public class ImageLine {
	public final ImageInfo imgInfo;

	/**
	 * tracks the current row number (from 0 to rows-1)
	 */
	private int rown = 0;

	/**
	 * The 'scanline' is an array of integers, corresponds to an image line (row).
	 * <p>
	 * Except for 'packed' formats (gray/indexed with 1-2-4 bitdepth) each <code>int</code> is a "sample" (one for
	 * channel), (0-255 or 0-65535) in the corresponding PNG sequence: <code>R G B R G B...</code> or
	 * <code>R G B A R G B A...</tt> 
	 * or <code>g g g ...</code> or <code>i i i</code> (palette index)
	 * <p>
	 * For bitdepth=1/2/4 , each value is a PACKED byte! To get an unpacked copy, see <code>tf_pack()</code> and its
	 * inverse <code>tf_unpack()</code>
	 * <p>
	 * To convert a indexed line to RGB balues, see <code>ImageLineHelper.palIdx2RGB()</code> (you can't do the reverse)
	 */
	public final int[] scanline;
	public final byte[] scanlineb;

	public enum SampleType {
		INT, SHORT, BYTE
	}

	public final SampleType sampleType;

	protected FilterType filterUsed; // informational ; only filled by the reader
	final int channels; // copied from imgInfo, more handy
	final int bitDepth; // copied from imgInfo, more handy

	public ImageLine(ImageInfo imgInfo) {
		this(imgInfo, SampleType.INT);
	}

	public ImageLine(ImageInfo imgInfo, SampleType stype) {
		this.imgInfo = imgInfo;
		channels = imgInfo.channels;
		bitDepth = imgInfo.bitDepth;
		filterUsed = FilterType.FILTER_UNKNOWN;
		this.sampleType = stype;
		if (stype == SampleType.INT) {
			scanline = new int[imgInfo.samplesPerRowP];
			scanlineb = null;
		} else if (stype == SampleType.BYTE) {
			scanlineb = new byte[imgInfo.samplesPerRowP];
			scanline = null;
		} else {
			throw new PngjException("not implemented");
		}

	}

	/** This row number inside the image (0 is top) */
	public int getRown() {
		return rown;
	}

	/** Increments row number */
	public void incRown() {
		this.rown++;
	}

	/** Sets row number (0 : Rows-1) */
	public void setRown(int n) {
		this.rown = n;
	}

	/**
	 * Unpacks scanline (for bitdepth 1-2-4) into a array <code>int[]</code>
	 * <p>
	 * You can (OPTIONALLY) pass an preallocated array, that will be filled and returned. If null, it will be allocated
	 * <p>
	 * If <code>scale==true<code>, it scales the value (just a bit shift) towards 0-255.
	 */
	public int[] unpack(int[] buf, boolean scale) {
		int len = imgInfo.samplesPerRow;
		if (buf == null || buf.length < len)
			buf = new int[len];
		if (bitDepth >= 8)
			System.arraycopy(scanline, 0, buf, 0, scanline.length);
		else {
			int mask, offset, v;
			int mask0 = getMaskForPackedFormats();
			int offset0 = 8 - bitDepth;
			mask = mask0;
			offset = offset0;
			for (int i = 0, j = 0; i < len; i++) {
				v = (scanline[j] & mask) >> offset;
				if (scale)
					v <<= offset0;
				buf[i] = v;
				mask = mask >> bitDepth;
				offset -= bitDepth;
				if (mask == 0) { // new byte in source
					mask = mask0;
					offset = offset0;
					j++;
				}
			}
		}
		return buf;
	}

	public byte[] unpack(byte[] buf, boolean scale) { // i know, i know, code duplication stinks
		int len = imgInfo.samplesPerRow;
		if (buf == null || buf.length < len)
			buf = new byte[len];
		if (bitDepth >= 8)
			System.arraycopy(scanlineb, 0, buf, 0, scanlineb.length);
		else {
			int mask, offset, v;
			int mask0 = getMaskForPackedFormats();
			int offset0 = 8 - bitDepth;
			mask = mask0;
			offset = offset0;
			for (int i = 0, j = 0; i < len; i++) {
				v = (scanlineb[j] & mask) >> offset;
				if (scale)
					v <<= offset0;
				buf[i] = (byte) (v&0xFF);
				mask = mask >> bitDepth;
				offset -= bitDepth;
				if (mask == 0) { // new byte in source
					mask = mask0;
					offset = offset0;
					j++;
				}
			}
		}
		return buf;
	}
	
	/**
	 * Packs scanline (for bitdepth 1-2-4) from array into the scanline
	 * <p>
	 * If <code>scale==true<code>, it scales the value (just a bit shift).
	 */
	public void pack(int[] buf, boolean scale) { // writes scanline
		int len = imgInfo.samplesPerRow;
		if (buf == null || buf.length < len)
			buf = new int[len];
		if (bitDepth >= 8)
			System.arraycopy(buf, 0, scanline, 0, scanline.length);
		else {
			int offset0 = 8 - bitDepth;
			int mask0 = getMaskForPackedFormats() >> offset0;
			int offset, v;
			offset = offset0;
			Arrays.fill(scanline, 0);
			for (int i = 0, j = 0; i < len; i++) {
				v = buf[i];
				if (scale)
					v >>= offset0;
				v = (v & mask0) << offset;
				scanline[j] |= v;
				offset -= bitDepth;
				if (offset < 0) { // new byte in scanline
					offset = offset0;
					j++;
				}
			}
		}
	}

	public void pack(byte[] buf, boolean scale) { // writes scanline
		int len = imgInfo.samplesPerRow;
		if (buf == null || buf.length < len)
			buf = new byte[len];
		if (bitDepth >= 8)
			System.arraycopy(buf, 0, scanlineb, 0, scanlineb.length);
		else {
			int offset0 = 8 - bitDepth;
			int mask0 = getMaskForPackedFormats() >> offset0;
			int offset, v;
			offset = offset0;
			Arrays.fill(scanlineb, (byte)0);
			for (int i = 0, j = 0; i < len; i++) {
				v = buf[i];
				if (scale)
					v >>= offset0;
				v = (v & mask0) << offset;
				scanlineb[j] |= v;
				offset -= bitDepth;
				if (offset < 0) { // new byte in scanline
					offset = offset0;
					j++;
				}
			}
		}
	}
	
	private int getMaskForPackedFormats() { // Utility function for pack/unpack
		if (bitDepth == 1)
			return 0x80;
		else if (bitDepth == 2)
			return 0xc0;
		else if (bitDepth == 4)
			return 0xf0;
		throw new RuntimeException("invalid bitDepth " + bitDepth);
	}

	public FilterType getFilterUsed() {
		return filterUsed;
	}

	public void setFilterUsed(FilterType ft) {
		filterUsed = ft;
	}

	/**
	 * Basic info
	 */
	public String toString() {
		return "row=" + rown + " cols=" + imgInfo.cols + " bpc=" + imgInfo.bitDepth + " size=" + scanline.length;
	}

	/**
	 * Prints some statistics - just for debugging
	 */
	public static void showLineInfo(ImageLine line) {
		System.out.println(line);
		ImageLineStats stats = new ImageLineHelper.ImageLineStats(line);
		System.out.println(stats);
		System.out.println(ImageLineHelper.infoFirstLastPixels(line));
	}

}
