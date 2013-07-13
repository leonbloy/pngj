package ar.com.hjg.pngj;


/**
 * Bunch of utility static methods to process/analyze an image line at the pixel
 * level.
 * <p>
 * Not essential at all, some methods are probably to be removed if future
 * releases.
 * <p>
 * WARNING: most methods for getting/setting values work currently only for
 * integer base imageLines
 */
public class ImageLineHelperNg {

	private final static double BIG_VALUE = Double.MAX_VALUE * 0.5;

	private final static double BIG_VALUE_NEG = Double.MAX_VALUE * (-0.5);

	static int[] DEPTH_UNPACK_1 = new int[2];
	static int[] DEPTH_UNPACK_2 = new int[4];
	static int[] DEPTH_UNPACK_4 = new int[16];
	static int[][] DEPTH_UNPACK = new int[5][];

	static {
		for (int i = 0; i < 2; i++)
			DEPTH_UNPACK_1[i] = i * 255;
		for (int i = 0; i < 4; i++)
			DEPTH_UNPACK_2[i] = (i * 255) / 3;
		for (int i = 0; i < 16; i++)
			DEPTH_UNPACK_4[i] = (i * 255) / 15;
		DEPTH_UNPACK[1] = DEPTH_UNPACK_1;
		DEPTH_UNPACK[2] = DEPTH_UNPACK_2;
		DEPTH_UNPACK[4] = DEPTH_UNPACK_2;
	}


	
	/**
	 * what follows is pretty uninteresting/untested/obsolete, subject to change
	 */
	/**
	 * Just for basic info or debugging. Shows values for first and last pixel.
	 * Does not include alpha
	 */
	public static String infoFirstLastPixels(ImageLine line) {
		return line.imgInfo.channels == 1 ? String.format("first=(%d) last=(%d)", line.scanline[0],
				line.scanline[line.scanline.length - 1]) : String.format("first=(%d %d %d) last=(%d %d %d)",
				line.scanline[0], line.scanline[1], line.scanline[2], line.scanline[line.scanline.length
						- line.imgInfo.channels], line.scanline[line.scanline.length - line.imgInfo.channels + 1],
				line.scanline[line.scanline.length - line.imgInfo.channels + 2]);
	}

	
	/**
	 * integer packed R G B only for bitdepth=8! (does not check!)
	 * 
	 **/
	public static int getPixelRGB8(ImageLine line, int column) {
		int offset = column * line.channels;
		return (line.scanline[offset] << 16) + (line.scanline[offset + 1] << 8) + (line.scanline[offset + 2]);
	}

	public static int getPixelARGB8(ImageLine line, int column) {
		int offset = column * line.channels;
		return (line.scanline[offset + 3] << 24) + (line.scanline[offset] << 16) + (line.scanline[offset + 1] << 8)
				+ (line.scanline[offset + 2]);
	}

	public static void setPixelsRGB8(ImageLine line, int[] rgb) {
		for (int i = 0, j = 0; i < line.imgInfo.cols; i++) {
			line.scanline[j++] = ((rgb[i] >> 16) & 0xFF);
			line.scanline[j++] = ((rgb[i] >> 8) & 0xFF);
			line.scanline[j++] = ((rgb[i] & 0xFF));
		}
	}

	public static void setPixelRGB8(ImageLine line, int col, int r, int g, int b) {
		col *= line.channels;
		line.scanline[col++] = r;
		line.scanline[col++] = g;
		line.scanline[col] = b;
	}

	public static void setPixelRGB8(ImageLine line, int col, int rgb) {
		setPixelRGB8(line, col, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
	}

	public static void setPixelsRGBA8(ImageLine line, int[] rgb) {
		for (int i = 0, j = 0; i < line.imgInfo.cols; i++) {
			line.scanline[j++] = ((rgb[i] >> 16) & 0xFF);
			line.scanline[j++] = ((rgb[i] >> 8) & 0xFF);
			line.scanline[j++] = ((rgb[i] & 0xFF));
			line.scanline[j++] = ((rgb[i] >> 24) & 0xFF);
		}
	}

	public static void setPixelRGBA8(ImageLine line, int col, int r, int g, int b, int a) {
		col *= line.channels;
		line.scanline[col++] = r;
		line.scanline[col++] = g;
		line.scanline[col++] = b;
		line.scanline[col] = a;
	}

	public static void setPixelRGBA8(ImageLine line, int col, int rgb) {
		setPixelRGBA8(line, col, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (rgb >> 24) & 0xFF);
	}

	public static void setValD(ImageLine line, int i, double d) {
		line.scanline[i] = double2int(line, d);
	}

	public static int interpol(int a, int b, int c, int d, double dx, double dy) {
		// a b -> x (0-1)
		// c d
		//
		double e = a * (1.0 - dx) + b * dx;
		double f = c * (1.0 - dx) + d * dx;
		return (int) (e * (1 - dy) + f * dy + 0.5);
	}

	public static double int2double(ImageLine line, int p) {
		return line.bitDepth == 16 ? p / 65535.0 : p / 255.0;
		// TODO: replace my multiplication? check for other bitdepths
	}

	public static double int2doubleClamped(ImageLine line, int p) {
		// TODO: replace my multiplication?
		double d = line.bitDepth == 16 ? p / 65535.0 : p / 255.0;
		return d <= 0.0 ? 0 : (d >= 1.0 ? 1.0 : d);
	}

	public static int double2int(ImageLine line, double d) {
		d = d <= 0.0 ? 0 : (d >= 1.0 ? 1.0 : d);
		return line.bitDepth == 16 ? (int) (d * 65535.0 + 0.5) : (int) (d * 255.0 + 0.5); //
	}

	public static int double2intClamped(ImageLine line, double d) {
		d = d <= 0.0 ? 0 : (d >= 1.0 ? 1.0 : d);
		return line.bitDepth == 16 ? (int) (d * 65535.0 + 0.5) : (int) (d * 255.0 + 0.5); //
	}

	public static int clampTo_0_255(int i) {
		return i > 255 ? 255 : (i < 0 ? 0 : i);
	}

	public static int clampTo_0_65535(int i) {
		return i > 65535 ? 65535 : (i < 0 ? 0 : i);
	}

	public static int clampTo_128_127(int x) {
		return x > 127 ? 127 : (x < -128 ? -128 : x);
	}

	/**
	 * Unpacks scanline (for bitdepth 1-2-4) into a array <code>int[]</code>
	 * <p>
	 * You can (OPTIONALLY) pass an preallocated array, that will be filled and
	 * returned. If null, it will be allocated
	 * <p>
	 * If
	 * <code>scale==true<code>, it scales the value (just a bit shift) towards 0-255.
	 * <p>
	 * You probably should use {@link ImageLine#unpackToNewImageLine()}
	 * 
	 */
	public static int[] unpack(ImageInfo imgInfo, int[] src, int[] dst, boolean scale) {
		int len1 = imgInfo.samplesPerRow;
		int len0 = imgInfo.samplesPerRowPacked;
		if (dst == null || dst.length < len1)
			dst = new int[len1];
		if (imgInfo.packed)
			ImageLine.unpackInplaceInt(imgInfo, src, dst, scale);
		else
			System.arraycopy(src, 0, dst, 0, len0);
		return dst;
	}

	public static byte[] unpack(ImageInfo imgInfo, byte[] src, byte[] dst, boolean scale) {
		int len1 = imgInfo.samplesPerRow;
		int len0 = imgInfo.samplesPerRowPacked;
		if (dst == null || dst.length < len1)
			dst = new byte[len1];
		if (imgInfo.packed)
			ImageLine.unpackInplaceByte(imgInfo, src, dst, scale);
		else
			System.arraycopy(src, 0, dst, 0, len0);
		return dst;
	}

	/**
	 * Packs scanline (for bitdepth 1-2-4) from array into the scanline
	 * <p>
	 * If <code>scale==true<code>, it scales the value (just a bit shift).
	 * 
	 * You probably should use {@link ImageLine#packToNewImageLine()}
	 */
	public static int[] pack(ImageInfo imgInfo, int[] src, int[] dst, boolean scale) {
		int len0 = imgInfo.samplesPerRowPacked;
		if (dst == null || dst.length < len0)
			dst = new int[len0];
		if (imgInfo.packed)
			ImageLine.packInplaceInt(imgInfo, src, dst, scale);
		else
			System.arraycopy(src, 0, dst, 0, len0);
		return dst;
	}

	public static byte[] pack(ImageInfo imgInfo, byte[] src, byte[] dst, boolean scale) {
		int len0 = imgInfo.samplesPerRowPacked;
		if (dst == null || dst.length < len0)
			dst = new byte[len0];
		if (imgInfo.packed)
			ImageLine.packInplaceByte(imgInfo, src, dst, scale);
		else
			System.arraycopy(src, 0, dst, 0, len0);
		return dst;
	}

	static int getMaskForPackedFormats(int bitDepth) { // Utility function for pack/unpack
		if (bitDepth == 4)
			return 0xf0;
		else if (bitDepth == 2)
			return 0xc0;
		else
			return 0x80; // bitDepth == 1
	}

	static int getMaskForPackedFormatsLs(int bitDepth) { // Utility function for pack/unpack
		if (bitDepth == 4)
			return 0x0f;
		else if (bitDepth == 2)
			return 0x03;
		else
			return 0x01; // bitDepth == 1
	}

}
