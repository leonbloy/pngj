package ar.com.hjg.pngj;


/**
 * Lightweight wrapper for an image scanline, used for read and write.
 * <p>
 * This object can be (usually it is) reused while iterating over the image
 * lines.
 * <p>
 * See <code>scanline</code> field, to understand the format.
 * 
 * FOrmat: byte (one bytes per sample),
 */
public class ImageLineByte implements IImageLine, IImageLineArray {
	public final ImageInfo imgInfo;

	final byte[] scanline;

	protected FilterType filterUsed; // informational ; only filled by the reader. not significant for interlaced
	final int channels; // copied from imgInfo, more handy
	final int bitDepth; // copied from imgInfo, more handy
	final int elementsPerRow; // = imgInfo.samplePerRowPacked, if packed:imgInfo.samplePerRow elswhere

	public ImageLineByte(ImageInfo imgInfo) {
		this(imgInfo, null);
	}

	public ImageLineByte(ImageInfo imgInfo, byte[] sci) {
		this.imgInfo = imgInfo;
		channels = imgInfo.channels;
		bitDepth = imgInfo.bitDepth;
		filterUsed = FilterType.FILTER_UNKNOWN;
		elementsPerRow = imgInfo.samplesPerRow ;
		scanline = sci != null && sci.length >= elementsPerRow ? sci : new byte[elementsPerRow];
	}

	public static IImageLineFactory<ImageLineByte> getFactory(ImageInfo iminfo) {
		return new IImageLineFactory<ImageLineByte>() {
			public ImageLineByte createImageLine(ImageInfo iminfo) {
				return new ImageLineByte(iminfo);
			}
		};
	}

	public FilterType getFilterUsed() {
		return filterUsed;
	}

	public byte[] getScanlineByte() {
		return scanline;
	}

	/**
	 * Basic info
	 */
	public String toString() {
		return " cols=" + imgInfo.cols + " bpc=" + imgInfo.bitDepth + " size=" + scanline.length;
	}


	public void fromPngRaw(byte[] raw, final int len, final int offset, final int step) {
		filterUsed=FilterType.getByVal(raw[0]);
		int len1 = len - 1;
		int step1 = (step - 1) * imgInfo.channels;
		if (imgInfo.bitDepth == 8) {
			if (step == 1) {// 8bispp non-interlaced: most important case, should be optimized
				System.arraycopy(raw, 1, scanline, 0, len1);
			} else {// 8bispp interlaced
				for (int s = 1, c = 0, i = offset * imgInfo.channels; s <= len1; s++, i++) {
					scanline[i] = raw[s];
					c++;
					if (c == imgInfo.channels) {
						c = 0;
						i += step1;
					}
				}
			}
		} else if (imgInfo.bitDepth == 16) {
			if (step == 1) {// 16bispp non-interlaced
				for (int i = 0, s = 1; i < imgInfo.samplesPerRow; i++,s+=2) {
					scanline[i] = raw[s]; //get the first byte
				}
			} else {
				for (int s = 1, c = 0, i = offset != 0 ? offset * imgInfo.channels : 0; s <= len1; s+=2, i++) {
					scanline[i] = raw[s] ;
					c++;
					if (c == imgInfo.channels) {
						c = 0;
						i += step1;
					}
				}
			}
		} else { // packed formats
			int mask0, mask, shi;
			mask0 = ImageLineHelperNg.getMaskForPackedFormats(bitDepth);
			for (int i = offset * imgInfo.channels, r = 1, c = 0; r < len; r++) {
				mask = mask0;
				shi = 8 - bitDepth;
				do {
					scanline[i] = (byte)((raw[r] & mask) >> shi);
					mask >>= bitDepth;
					shi -= bitDepth;
					i++;
					c++;
					if (c == imgInfo.channels) {
						c = 0;
						i += step1;
					}
				} while (mask != 0 && i < elementsPerRow);
			}
		}
	}

	public void end() { // nothing to do here
	}

	public int getSize() {
		return elementsPerRow;
	}

	public int getElem(int i) {
		return scanline[i];
	}

	public void toPngRaw(byte[] raw) {
		throw new RuntimeException("not implemented"); //TODO
	}

	public byte[] getScanline() {
		return scanline;
	}

}
