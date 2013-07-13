package ar.com.hjg.pngj;

/**
 * Lightweight wrapper for an image scanline, used for read and write.
 * <p>
 * This object can be (usually it is) reused while iterating over the image
 * lines.
 * <p>
 * See <code>scanline</code> field, to understand the format.
 * 
 * FOrmat: int (one integer by sample),
 */
public class ImageLine implements IImageLine, IImageLineArray {
	public final ImageInfo imgInfo;

	/**
	 * The 'scanline' is an array of integers, corresponds to an image line
	 * (row).
	 * <p>
	 * Except for 'packed' formats (gray/indexed with 1-2-4 bitdepth) each
	 * <code>int</code> is a "sample" (one for channel), (0-255 or 0-65535) in
	 * the corresponding PNG sequence: <code>R G B R G B...</code> or
	 * <code>R G B A R G B A...</tt> 
	 * or <code>g g g ...</code> or <code>i i i</code> (palette index)
	 * <p>
	 * For bitdepth=1/2/4 , and if samplesUnpacked=false, each value is a PACKED
	 * byte!
	 * <p>
	 * To convert a indexed line to RGB balues, see
	 * <code>ImageLineHelper.palIdx2RGB()</code> (you can't do the reverse)
	 */
	protected final int[] scanline;
	protected final int size;

	protected FilterType filterUsed; // informational ; only filled by the reader. not significant for interlaced

	/**
	 * 
	 * @param imgInfo
	 *            Inmutable ImageInfo, basic parameter of the image we are
	 *            reading or writing
	 * @param stype
	 *            INT or BYTE : this determines which scanline is the really
	 *            used one
	 * @param unpackedMode
	 *            If true, we use unpacked format, even for packed original
	 *            images
	 * 
	 */
	public ImageLine(ImageInfo imgInfo) {
		this(imgInfo, null);
	}

	public ImageLine(ImageInfo imgInfo, int[] sci) {
		this.imgInfo = imgInfo;
		filterUsed = FilterType.FILTER_UNKNOWN;
		size = imgInfo.samplesPerRow;
		scanline = sci != null && sci.length >= size ? sci : new int[size];
	}

	public static IImageLineFactory<ImageLine> getFactory(ImageInfo iminfo) {
		return new IImageLineFactory<ImageLine>() {
			public ImageLine createImageLine(ImageInfo iminfo) {
				return new ImageLine(iminfo);
			}
		};
	}

	/*
	 * Unpacks scanline (for bitdepth 1-2-4)
	 * 
	 * Arrays must be prealocated. src : samplesPerRowPacked dst : samplesPerRow
	 * 
	 * This usually works in place (with src==dst and length=samplesPerRow)!
	 * 
	 * If not, you should only call this only when necesary (bitdepth <8)
	 * 
	 * If <code>scale==true<code>, and if image is not indexed, it scales the value towards 0-255.
	 */
	static void unpackInplaceInt(final ImageInfo iminfo, final int[] src, final int[] dst, boolean scale) {
		final int bitDepth = iminfo.bitDepth;
		if (bitDepth >= 8)
			return; // nothing to do
		if (scale && iminfo.indexed)
			scale = false;
		final int mask0 = ImageLineHelper.getMaskForPackedFormatsLs(bitDepth);
		final int[] unpackArray = bitDepth == 1 ? ImageLineHelper.DEPTH_UNPACK_1
				: (bitDepth == 4 ? ImageLineHelper.DEPTH_UNPACK_4 : ImageLineHelper.DEPTH_UNPACK_2);
		final int offset0 = 8 * iminfo.samplesPerRowPacked - bitDepth * iminfo.samplesPerRow;
		int mask, offset, v;
		if (offset0 != 8) {
			mask = mask0 << offset0;
			offset = offset0; // how many bits to shift the mask to the right to recover mask0
		} else {
			mask = mask0;
			offset = 0;
		}
		for (int j = iminfo.samplesPerRow - 1, i = iminfo.samplesPerRowPacked - 1; j >= 0; j--) {
			v = (src[i] & mask) >> offset;
			if (scale)
				v = unpackArray[v];
			dst[j] = v;
			mask <<= bitDepth;
			offset += bitDepth;
			if (offset == 8) {
				mask = mask0;
				offset = 0;
				i--;
			}
		}
	}

	/*
	 * Unpacks scanline (for bitdepth 1-2-4)
	 * 
	 * Arrays must be prealocated. src : samplesPerRow dst : samplesPerRowPacked
	 * 
	 * This usually works in place (with src==dst and length=samplesPerRow)! If not, you should only call this only when
	 * necesary (bitdepth <8)
	 * 
	 * The trailing elements are trash
	 * 
	 * 
	 * If <code>scale==true<code>, it scales the value (just a bit shift) towards 0-255.
	 */
	static void packInplaceInt(final ImageInfo iminfo, final int[] src, final int[] dst, final boolean scaled) {
		final int bitDepth = iminfo.bitDepth;
		if (bitDepth >= 8)
			return; // nothing to do
		final int mask0 = ImageLineHelper.getMaskForPackedFormatsLs(bitDepth);
		final int scalefactor = 8 - bitDepth;
		final int offset0 = 8 - bitDepth;
		int v, v0;
		int offset = 8 - bitDepth;
		v0 = src[0]; // first value is special for in place
		dst[0] = 0;
		if (scaled)
			v0 >>= scalefactor;
		v0 = ((v0 & mask0) << offset);
		for (int i = 0, j = 0; j < iminfo.samplesPerRow; j++) {
			v = src[j];
			if (scaled)
				v >>= scalefactor;
			dst[i] |= ((v & mask0) << offset);
			offset -= bitDepth;
			if (offset < 0) {
				offset = offset0;
				i++;
				dst[i] = 0;
			}
		}
		dst[0] |= v0;
	}

	static void unpackInplaceByte(final ImageInfo iminfo, final byte[] src, final byte[] dst, final boolean scale) {
		final int bitDepth = iminfo.bitDepth;
		if (bitDepth >= 8)
			return; // nothing to do
		final int mask0 = ImageLineHelper.getMaskForPackedFormatsLs(bitDepth);
		final int[] unpackArray = ImageLineHelper.DEPTH_UNPACK[bitDepth];
		final int offset0 = 8 * iminfo.samplesPerRowPacked - bitDepth * iminfo.samplesPerRow;
		int mask, offset, v;
		if (offset0 != 8) {
			mask = mask0 << offset0;
			offset = offset0; // how many bits to shift the mask to the right to recover mask0
		} else {
			mask = mask0;
			offset = 0;
		}
		for (int j = iminfo.samplesPerRow - 1, i = iminfo.samplesPerRowPacked - 1; j >= 0; j--) {
			v = (src[i] & mask) >> offset;
			if (scale)
				v = unpackArray[v];
			dst[j] = (byte) v;
			mask <<= bitDepth;
			offset += bitDepth;
			if (offset == 8) {
				mask = mask0;
				offset = 0;
				i--;
			}
		}
	}

	/**
	 * size original: samplesPerRow sizeFinal: samplesPerRowPacked (trailing
	 * elements are trash!)
	 **/
	static void packInplaceByte(final ImageInfo iminfo, final byte[] src, final byte[] dst, final boolean scaled) {
		final int bitDepth = iminfo.bitDepth;
		if (bitDepth >= 8)
			return; // nothing to do
		final int mask0 = ImageLineHelper.getMaskForPackedFormatsLs(bitDepth);
		final int scalefactor = 8 - bitDepth;
		final int offset0 = 8 - bitDepth;
		int v, v0;
		int offset = 8 - bitDepth;
		v0 = src[0]; // first value is special
		dst[0] = 0;
		if (scaled)
			v0 >>= scalefactor;
		v0 = ((v0 & mask0) << offset);
		for (int i = 0, j = 0; j < iminfo.samplesPerRow; j++) {
			v = src[j];
			if (scaled)
				v >>= scalefactor;
			dst[i] |= ((v & mask0) << offset);
			offset -= bitDepth;
			if (offset < 0) {
				offset = offset0;
				i++;
				dst[i] = 0;
			}
		}
		dst[0] |= v0;
	}

	/**
	 * Creates a new ImageLine similar to this, but unpacked
	 * 
	 * The caller must be sure that the original was really packed
	 */
	public ImageLine unpackToNewImageLine() {
		ImageLine newline = new ImageLine(imgInfo);
		unpackInplaceInt(imgInfo, scanline, newline.scanline, false);
		return newline;
	}

	/**
	 * Creates a new ImageLine similar to this, but packed
	 * 
	 * The caller must be sure that the original was really unpacked
	 */
	public ImageLine packToNewImageLine() {
		ImageLine newline = new ImageLine(imgInfo);
		packInplaceInt(imgInfo, scanline, newline.scanline, false);
		return newline;
	}

	public FilterType getFilterUsed() {
		return filterUsed;
	}

	public void setFilterUsed(FilterType ft) {
		filterUsed = ft;
	}

	public int[] getScanlineInt() {
		return scanline;
	}

	/**
	 * Basic info
	 */
	public String toString() {
		return " cols=" + imgInfo.cols + " bpc=" + imgInfo.bitDepth + " size=" + scanline.length;
	}

	public void fromPngRaw(byte[] raw, final int len, final int offset, final int step) {
		setFilterUsed(FilterType.getByVal(raw[0]));
		int len1 = len - 1;
		int step1 = (step - 1) * imgInfo.channels;
		if (imgInfo.bitDepth == 8) {
			if (step == 1) {// 8bispp non-interlaced: most important case, should be optimized
				for (int i = 0; i < size; i++) {
					scanline[i] = (raw[i + 1] & 0xff);
				}
			} else {// 8bispp interlaced
				for (int s = 1, c = 0, i = offset * imgInfo.channels; s <= len1; s++, i++) {
					scanline[i] = (raw[s] & 0xff);
					c++;
					if (c == imgInfo.channels) {
						c = 0;
						i += step1;
					}
				}
			}
		} else if (imgInfo.bitDepth == 16) {
			if (step == 1) {// 16bispp non-interlaced
				for (int i = 0, s = 1; i < size; i++) {
					scanline[i] = ((raw[s++] & 0xFF) << 8) | (raw[s++] & 0xFF); // 16 bitspc
				}
			} else {
				for (int s = 1, c = 0, i = offset != 0 ? offset * imgInfo.channels : 0; s <= len1; s++, i++) {
					scanline[i] = ((raw[s++] & 0xFF) << 8) | (raw[s] & 0xFF); // 16 bitspc
					c++;
					if (c == imgInfo.channels) {
						c = 0;
						i += step1;
					}
				}
			}
		} else { // packed formats
			int mask0, mask, shi, bd;
			bd = imgInfo.bitDepth;
			mask0 = ImageLineHelperNg.getMaskForPackedFormats(bd);
			for (int i = offset * imgInfo.channels, r = 1, c = 0; r < len; r++) {
				mask = mask0;
				shi = 8 - bd;
				do {
					scanline[i] = (raw[r] & mask) >> shi;
					mask >>= bd;
					shi -= bd;
					i++;
					c++;
					if (c == imgInfo.channels) {
						c = 0;
						i += step1;
					}
				} while (mask != 0 && i < size);
			}
		}
	}

	public void toPngRaw(byte[] raw) {
		if (imgInfo.bitDepth == 8) {
			for (int i = 0; i < size; i++) {
				raw[i + 1] = (byte) scanline[i];
			}
		} else if (imgInfo.bitDepth == 16) {
			for (int i = 0, s = 1; i < imgInfo.samplesPerRow; i++) {
				scanline[i] = ((raw[s++] & 0xFF) << 8) | (raw[s++] & 0xFF);
			}
		} else { // packed formats
			int shi, bd, v;
			bd = imgInfo.bitDepth;
			shi = 8 - bd;
			v = 0;
			for (int i = 0, r = 1; i < size; i++) {
				v |= (scanline[i] << shi);
				shi += bd;
				if (shi >= 8 || i == size - 1) {
					raw[r++] = (byte) v;
					shi = 8 - bd;
				}
			}
		}
	}

	public void end() { // nothing to do here
	}

	public int getSize() {
		return size;
	}

	public int getElem(int i) {
		return scanline[i];
	}

	public int[] getScanline() {
		return scanline;
	}

}
