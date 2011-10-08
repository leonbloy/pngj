package ar.com.hjg.pngj;

import ar.com.hjg.pngj.chunks.PngChunkPLTE;

/**
 * Bunch of utility static methods to process/analyze an image line. Not
 * essential at all.
 */
public class ImageLineHelper {
	private final static double BIG_VALUE = Double.MAX_VALUE * 0.5;
	private final static double BIG_VALUE_NEG = Double.MAX_VALUE * (-0.5);

	/**
	 * Given an indexed line with a palette, unpacks as a RGB array
	 * 
	 * @param line ImageLine as returned from PngReader
	 * @param pal  Palette chunk
	 * @param buf Preallocated array, optional
	 * @return R G B (one byte per sample)
	 */
	public int[] tf_palIdx2RGB(ImageLine line, PngChunkPLTE pal, int[] buf) {
		// TODO: test! Add alpha palette info?
		int nbytes = line.imgInfo.cols * 3;
		if(buf == null || buf.length < nbytes) buf = new int[nbytes];
		int[] src; // from where to read the indexes as bytes
		if(line.imgInfo.packed) { // requires unpacking
			line.tf_unpack(buf, false); // use buf temporarily (have space)
			src= buf;
		} else {
			src = line.scanline;
		}
		for(int c=line.imgInfo.cols-1;c>=0;c--) { 
			// scan from right to left to not overwrite myself  
			pal.getEntryRgb(src[c],buf,c*3);
		}
		return buf;
	}

	/** what follows is pretty uninteresting/untested/obsolete, subject to change */
	/**
	 * Just for basic info or debugging. Shows values for first and last pixel.
	 * Does not include alpha
	 */
	public static String infoFirstLastPixels(ImageLine line) {
		return line.imgInfo.channels == 1 ? String.format("first=(%d) last=(%d)",
				line.scanline[0], line.scanline[line.scanline.length - 1]) : String.format(
				"first=(%d %d %d) last=(%d %d %d)", line.scanline[0], line.scanline[1],
				line.scanline[2], line.scanline[line.scanline.length - line.imgInfo.channels],
				line.scanline[line.scanline.length - line.imgInfo.channels + 1],
				line.scanline[line.scanline.length - line.imgInfo.channels + 2]);
	}

	public static String infoFull(ImageLine line) {
		ImageLineStats stats = new ImageLineStats(line);
		return "row=" + line.getRown() + " " + stats.toString() + "\n  "
				+ infoFirstLastPixels(line);
	}

	/**
	 * Computes some statistics for the line. Not very efficient or elegant,
	 * mainly for tests. Only for RGB/RGBA Outputs values as doubles (0.0 - 1.0)
	 */
	static class ImageLineStats {
		public double[] prom = { 0.0, 0.0, 0.0, 0.0 }; // channel averages
		public double[] maxv = { BIG_VALUE_NEG, BIG_VALUE_NEG, BIG_VALUE_NEG, BIG_VALUE_NEG }; // maximo
		public double[] minv = { BIG_VALUE, BIG_VALUE, BIG_VALUE, BIG_VALUE };
		public double promlum = 0.0; // maximum global (luminance)
		public double maxlum = BIG_VALUE_NEG; // max luminance
		public double minlum = BIG_VALUE;
		public double[] maxdif = { BIG_VALUE_NEG, BIG_VALUE_NEG, BIG_VALUE_NEG, BIG_VALUE }; // maxima
		public final int channels; // diferencia

		public String toString() {
			return channels == 3 ? String
					.format(
							"prom=%.1f (%.1f %.1f %.1f) max=%.1f (%.1f %.1f %.1f) min=%.1f (%.1f %.1f %.1f)",
							promlum, prom[0], prom[1], prom[2], maxlum, maxv[0], maxv[1], maxv[2],
							minlum, minv[0], minv[1], minv[2])
					+ String.format(" maxdif=(%.1f %.1f %.1f)", maxdif[0], maxdif[1], maxdif[2])
					: String
							.format(
									"prom=%.1f (%.1f %.1f %.1f %.1f) max=%.1f (%.1f %.1f %.1f %.1f) min=%.1f (%.1f %.1f %.1f %.1f)",
									promlum, prom[0], prom[1], prom[2], prom[3], maxlum, maxv[0], maxv[1],
									maxv[2], maxv[3], minlum, minv[0], minv[1], minv[2], minv[3])
							+ String.format(" maxdif=(%.1f %.1f %.1f %.1f)", maxdif[0], maxdif[1],
									maxdif[2], maxdif[3]);
		}

		public ImageLineStats(ImageLine line) {
			this.channels = line.channels;
			if (line.channels < 3)
				throw new PngjException("ImageLineStats only works for RGB - RGBA");
			int ch = 0;
			double lum, x, d;
			for (int i = 0; i < line.imgInfo.cols; i++) {
				lum = 0;
				for (ch = channels - 1; ch >= 0; ch--) {
					x = int2double(line, line.scanline[i * channels]);
					if (ch < 3)
						lum += x;
					prom[ch] += x;
					if (x > maxv[ch])
						maxv[ch] = x;
					if (x < minv[ch])
						minv[ch] = x;
					if (i >= channels) {
						d = Math.abs(x - int2double(line, line.scanline[i - channels]));
						if (d > maxdif[ch])
							maxdif[ch] = d;
					}
				}
				promlum += lum;
				if (lum > maxlum)
					maxlum = lum;
				if (lum < minlum)
					minlum = lum;
			}
			for (ch = 0; ch < channels; ch++) {
				prom[ch] /= line.imgInfo.cols;
			}
			promlum /= (line.imgInfo.cols * 3.0);
			maxlum /= 3.0;
			minlum /= 3.0;
		}
	}

	/**
	 * integer packed R G B only for bitdepth=8! (does not check!)
	 * 
	 **/
	public static int getPixelRGB8(ImageLine line, int column) {
		int offset = column * line.channels;
		return (line.scanline[offset] << 16) + (line.scanline[offset + 1] << 8)
				+ (line.scanline[offset + 2]);
	}

	public static int getPixelARGB8(ImageLine line, int column) {
		int offset = column * line.channels;
		return (line.scanline[offset + 3] << 24) + (line.scanline[offset] << 16)
				+ (line.scanline[offset + 1] << 8) + (line.scanline[offset + 2]);
	}

	public static void setPixelsRGB8(ImageLine line, int[] rgb) {
		for (int i = 0; i < line.imgInfo.cols; i++) {
			line.scanline[i * line.channels] = ((rgb[i] & 0xFF0000) >> 16);
			line.scanline[i * line.channels + 1] = ((rgb[i] & 0xFF00) >> 8);
			line.scanline[i * line.channels + 2] = ((rgb[i] & 0xFF));
		}
	}

	public static void setPixelRGB8(ImageLine line, int col, int r,int g,int b) {
		line.scanline[col * line.channels] = r;
		line.scanline[col * line.channels + 1] = g;
		line.scanline[col * line.channels + 2] = b;
		
	}
	
	public static void setPixelRGB8(ImageLine line, int col, int rgb) {
		setPixelRGB8(line,col,((rgb & 0xFF0000) >> 16),((rgb & 0xFF00) >> 8),((rgb & 0xFF)));
	}

	public static void setValD(ImageLine line, int i, double d) {
		line.scanline[i] = double2int(line, d);
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
}
