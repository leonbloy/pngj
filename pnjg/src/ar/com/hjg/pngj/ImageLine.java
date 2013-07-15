package ar.com.hjg.pngj;

/**
 * Lightweight wrapper for an image scanline, used for read and write.
 * <p>
 * This object can be (usually it is) reused while iterating over the image
 * lines.
 * <p>
 * See <code>scanline</code> field, to understand the format.
 * 
 * Format: int (one integer by sample),
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

	protected FilterType filterType =FilterType.FILTER_UNKNOWN; // informational ; only filled by the reader. not significant for interlaced

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
		filterType = FilterType.FILTER_UNKNOWN;
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


	public FilterType getFilterType() {
		return filterType;
	}

	public void setFilterType(FilterType ft) {
		filterType = ft;
	}

	/**
	 * Basic info
	 */
	public String toString() {
		return " cols=" + imgInfo.cols + " bpc=" + imgInfo.bitDepth + " size=" + scanline.length;
	}

	public void readFromPngRaw(byte[] raw, final int len, final int offset, final int step) {
		setFilterType(FilterType.getByVal(raw[0]));
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
			mask0 = ImageLineHelper.getMaskForPackedFormats(bd);
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

	public void writeToPngRaw(byte[] raw) {
		raw[0] = (byte) filterType.val;
		if (imgInfo.bitDepth == 8) {
			for (int i = 0; i < size; i++) {
				raw[i + 1] = (byte) scanline[i];
			}
		} else if (imgInfo.bitDepth == 16) {
			for (int i = 0, s = 1; i < size; i++) {
				raw[s++] = (byte)(scanline[i]>>8);
				raw[s++] = (byte)(scanline[i]&0xff);
			}
		} else { // packed formats
			int shi, bd, v;
			bd = imgInfo.bitDepth;
			shi = 8 - bd;
			v = 0;
			for (int i = 0, r = 1; i < size; i++) {
				v |= (scanline[i] << shi);
				shi -= bd;
				if (shi < 0 || i == size - 1) {
					raw[r++] = (byte) v;
					shi = 8 - bd;
					v=0;
				}
			}
		}
	}

	public void endReadFromPngRaw() {
	
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

	public ImageInfo getImageInfo() {
		return imgInfo;
	}


}
