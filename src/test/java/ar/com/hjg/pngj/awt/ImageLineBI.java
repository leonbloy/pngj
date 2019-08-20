package ar.com.hjg.pngj.awt;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

public class ImageLineBI implements IImageLine {

	private final ImageLineSetBI imageLineSetBI;
	private final ImageInfo imgInfo;
	private final Png2BufferedImageAdapter adapter2bi;
	private final BufferedImage2PngAdapter adapter2png;
	private int rowNumber = -1; // This is the number in the BI, not necessarily in the pNG

	public final int datalen; // length in samples of the row
	private final BufferedImage bi;

	public ImageLineBI(ImageLineSetBI imageLineBiSet, int rowInBI) {
		this.imageLineSetBI = imageLineBiSet;
		bi = imageLineBiSet.image;
		this.imgInfo = imageLineBiSet.iminfo;
		adapter2bi = imageLineBiSet.adapter2bi;
		adapter2png = imageLineBiSet.adapter2png;
		this.rowNumber = rowInBI;
		datalen = imgInfo.cols * imgInfo.channels;
	}

	public static class Png2BufferedImageAdapter {

		private ImageInfo iminfo;
		private PngChunkPLTE pal;
		private PngChunkTRNS trns;
		boolean usetrnsForPallete = true;
		boolean usetrnsForNonPalette = false;
		boolean preferCustom = true; // more quick but perhaps less compatible?
		// computedParams
		private int datatype;
		private boolean withPalette;
		private boolean transparency;
		private boolean packedBytes; // sample model= MultiPixelPacked or PixelInterleaved
		private int components;
		private int biType;
		private boolean bgrOrder;

		public Png2BufferedImageAdapter(ImageInfo iminfo, PngChunkPLTE pal, PngChunkTRNS trns) {
			this.iminfo = iminfo;
			this.pal = pal;
			this.trns = trns;
			if (pal == null && iminfo.indexed)
				throw new RuntimeException("missing palette");
		}

		// first strategy:
		// buffer datatype = TYPE_BYTE, except for 16 bits USHORT
		// samplemodel= PixelInterleaved ALWAYS
		// We do basically the same as Java.awt, except that packed formats are
		// treated as 8-bits
		// TODO
		protected void computeParams() {
			packedBytes = false; // we NEVER pack bits
			transparency = iminfo.alpha || (trns != null && iminfo.indexed && usetrnsForPallete)
					|| (trns != null && (!iminfo.indexed) && usetrnsForNonPalette);
			withPalette = iminfo.indexed || (iminfo.greyscale && iminfo.bitDepth < 8);
			datatype = iminfo.bitDepth == 16 ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;
			if (iminfo.bitDepth == 16) {
				biType = (iminfo.greyscale && !iminfo.alpha) ? BufferedImage.TYPE_USHORT_GRAY
						: BufferedImage.TYPE_CUSTOM;
			} else if (iminfo.bitDepth == 8) {
				if (iminfo.channels == 3)
					biType = BufferedImage.TYPE_3BYTE_BGR;
				else if (iminfo.channels == 4)
					biType = BufferedImage.TYPE_4BYTE_ABGR;
				else if (iminfo.greyscale)
					biType = iminfo.alpha ? BufferedImage.TYPE_CUSTOM : BufferedImage.TYPE_BYTE_GRAY;
				else
					biType = BufferedImage.TYPE_BYTE_INDEXED;
			} else
				biType = BufferedImage.TYPE_BYTE_INDEXED;
			components = withPalette ? (transparency ? 4 : 3) : iminfo.channels;
		}

		protected IndexColorModel buildLut() {

			IndexColorModel cm;
			if (iminfo.greyscale) {
				int len = 1 << iminfo.bitDepth;
				byte[] r = new byte[len];
				for (int i = 0; i < len; i++)
					r[i] = (byte) (len == 256 ? i : ((i * 255) / (len - 1)));
				cm = new IndexColorModel(8, len, r, r, r);
			} else {
				int len = pal.getNentries();
				int lent = trns != null ? trns.getPalletteAlpha().length : 0;
				boolean alpha = lent > 0;
				byte[] r = new byte[len];
				byte[] g = new byte[len];
				byte[] b = new byte[len];
				byte[] a = alpha ? new byte[len] : null;
				int rgb[] = new int[3];
				for (int i = 0; i < len; i++) {
					pal.getEntryRgb(i, rgb);
					r[i] = (byte) rgb[0];
					g[i] = (byte) rgb[1];
					b[i] = (byte) rgb[2];
					if (alpha)
						a[i] = (byte) (i < lent ? trns.getPalletteAlpha()[i] : 255);
				}
				cm = alpha ? new IndexColorModel(8, len, r, g, b, a) : new IndexColorModel(8, len, r, g, b);
			}
			return cm;
		}

		public BufferedImage createBufferedImage() {
			return createBufferedImage(iminfo.cols, iminfo.rows);
		}

		public BufferedImage createBufferedImage(int cols, int rows) {
			computeParams();
			BufferedImage bi = null;
			if (withPalette) {
				bi = createBufferedImageWithPalette(cols, rows);
			} else if (preferCustom || biType == BufferedImage.TYPE_CUSTOM) {
				boolean usebgr = false; // test:
				bi = createBufferedImageCustom(cols, rows, usebgr);
				bgrOrder = usebgr && iminfo.channels > 2;
			} else if (biType == BufferedImage.TYPE_4BYTE_ABGR || biType == BufferedImage.TYPE_3BYTE_BGR
					|| biType == BufferedImage.TYPE_BYTE_GRAY || biType == BufferedImage.TYPE_USHORT_GRAY) {
				// nice types
				bi = new BufferedImage(cols, rows, biType);
				bgrOrder = iminfo.channels > 2;
			}
			// final checks
			if (bi == null)
				throw new PngjException("Unknown type");
			return bi;
		}

		private BufferedImage createBufferedImageCustom(int cols, int rows, boolean bgr) {
			// used for GA8 , GA16 RGB16 RGBA16 (perhaps more) (but no for palette or G124)
			boolean useByte = iminfo.bitDepth < 16;
			boolean hasAlpha = iminfo.alpha;
			ColorSpace colorspace = createColorSpace(iminfo.greyscale);
			int[] nBits = new int[iminfo.channels];
			Arrays.fill(nBits, useByte ? 8 : 16);
			ComponentColorModel colorModel = new ComponentColorModel(colorspace, nBits, hasAlpha, false,
					hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE,
					useByte ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT);
			WritableRaster raster = null;
			int[] bOffs = new int[iminfo.channels];
			for (int i = 0; i < bOffs.length; i++) {
				bOffs[i] = bgr ? bOffs.length - i - 1 : i; // RGB or BGR?
			}
			if (iminfo.channels == 1)
				raster = colorModel.createCompatibleWritableRaster(cols, rows);
			else
				raster = Raster.createInterleavedRaster(useByte ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT, cols,
						rows, cols * iminfo.channels, iminfo.channels, bOffs, null);
			BufferedImage bi = new BufferedImage(colorModel, raster, false, null);
			return bi;

		}

		private BufferedImage createBufferedImageWithPalette(int cols, int rows) {
			IndexColorModel cm = buildLut();
			return new BufferedImage(cols, rows, biType, cm);
		}

		public ImageInfo getIminfo() {
			return iminfo;
		}

		public int getDatatype() {
			return datatype;
		}

		public boolean isWithPalette() {
			return withPalette;
		}

		public boolean isTransparency() {
			return transparency;
		}

		public boolean isPackedBytes() {
			return packedBytes;
		}

		public int getComponents() {
			return components;
		}

		public int getBiType() {
			return biType;
		}

		public void setPreferCustom(boolean preferCustom) {
			this.preferCustom = preferCustom;
		}

		public boolean isBgrOrder() {
			return bgrOrder;
		}

	}

	public static class BufferedImage2PngAdapter {

		public final BufferedImage image;
		public boolean forceresortToGetRGB = false;

		// WARNING: these defaults are assumed in computeParams
		int datasize = 1; // 1:byte, 2short, 4 int
		int channels = 3; // including alpha; for palette this is 1 (even with palette with alpha)
		boolean withpalette = false;
		boolean hasalpha = false; // if withpalette=true and hasalpha this means that we need to add a TRNS chunk
		boolean gray = false;
		boolean resortToGetRGB = false; // if this is true, much of the above is not used
		boolean reverseOrder = false; // BGR instead of RGB, etc
		boolean packedInInt = false; // only for packedInInt

		private ColorModel colormodel;

		private ImageInfo imi0;

		private PngChunkTRNS trnsChunk;

		private PngChunkPLTE plteChunk;

		public BufferedImage2PngAdapter(BufferedImage bi) {
			this.image = bi;
		}

		public ImageInfo createImgInfo(int nlines, int offset, int step) {
			computeParams();
			ImageInfo iminfo = nlines == imi0.rows ? imi0
					: new ImageInfo(imi0.cols, nlines, imi0.bitDepth, imi0.alpha, imi0.greyscale, imi0.indexed);
			return iminfo;
		}

		private void computeParams() {
			final int type = image.getType();
			colormodel = image.getColorModel();
			// Tentative, all will probably be changed later
			hasalpha = colormodel.getTransparency() != Transparency.OPAQUE;
			channels = colormodel.getComponentSize().length;
			datasize = 1; // tentative!
			if (!forceresortToGetRGB) {
				switch (type) {
				case BufferedImage.TYPE_3BYTE_BGR:
					reverseOrder = true;
					break;
				case BufferedImage.TYPE_4BYTE_ABGR:
					reverseOrder = true;
					hasalpha = true;
					channels = 4;
					break;
				case BufferedImage.TYPE_BYTE_GRAY:
					channels = 1;
					gray = true; // is this secure? see doc about s_GRAY and alpha
					break;
				case BufferedImage.TYPE_USHORT_GRAY:
					channels = 1;
					gray = true; // is this secure? see doc about s_GRAY and alpha
					datasize = 2;
					break;
				case BufferedImage.TYPE_BYTE_INDEXED:
					channels = 1;
					withpalette = true;
					hasalpha = colormodel.getTransparency() != Transparency.OPAQUE;
					break;
				case BufferedImage.TYPE_INT_ARGB:
					channels = 4;
					hasalpha = true;
					datasize = 4;
					packedInInt = true;
					reverseOrder = false; // however, this has special meaning in this case (RGB, but alpha comes first)
					break;
				case BufferedImage.TYPE_INT_BGR:
				case BufferedImage.TYPE_INT_RGB:
					channels = 3;
					hasalpha = false;
					datasize = 4;
					packedInInt = true;
					reverseOrder = type == BufferedImage.TYPE_INT_BGR;
					break;
				case BufferedImage.TYPE_CUSTOM:
					resortToGetRGB = true; // tentatively, but...
					// see if it's not one of our own nice formats.. All this is quite related to
					// Png2BufferedImageAdapter.createBufferedImageCustom()
					if (!(colormodel instanceof ComponentColorModel))
						break;// no luck
					if (colormodel.getColorSpace() == createColorSpace(true)) {
						gray = true;
					} else {
						if (colormodel.getColorSpace() != createColorSpace(false))
							break;// unrecognized colorspace, no luck
					}
					int[] nbits = ((ComponentColorModel) colormodel).getComponentSize();
					datasize = nbits[0] / 8;
					if (nbits[0] < 8)
						break;
					WritableRaster raster = image.getRaster();
					DataBuffer databuf = raster.getDataBuffer();
					SampleModel sampleModel = raster.getSampleModel();
					if (sampleModel instanceof PixelInterleavedSampleModel) { // for more-than-one channel
						int[] sizes = sampleModel.getSampleSize();
						if (sizes.length > 4 || sizes.length < 2)
							break;
						channels = sizes.length;
						// hasalpha = channels!=3; // we must check this later
						datasize = sizes[0] / 8;
						if (nbits[0] < 8)
							break;
						if (!allEqual(sizes))
							break;// all sizes must be equal
						// all right, let's check the order. we accept either GA RGB RGBA or the reverse
						int[] boff = ((PixelInterleavedSampleModel) sampleModel).getBandOffsets();
						int direction = increasing(boff);
						if (direction == 1)
							reverseOrder = false;
						else if (direction == -1)
							reverseOrder = true;
						else
							break;
					} else if (sampleModel instanceof SinglePixelPackedSampleModel) { // we accept this only for one
						// channel
						// (gray 8 or 16 bits)
						int[] sizes = ((SinglePixelPackedSampleModel) sampleModel).getSampleSize();
						if (sizes.length != 1)
							break;
						datasize = sizes[0] / 8;
						if (sizes[0] < 8)
							break;
						channels = 1;
						gray = true;
					}
					if (databuf.getNumBanks() != 1)
						break;
					// ok!
					resortToGetRGB = false;
					break;
				default: // bad luck
					resortToGetRGB = true;
					break;
				}
			} else
				resortToGetRGB = true;
			int rows = image.getHeight();
			int cols = image.getWidth();
			int bitdepth = datasize == 1 || packedInInt ? 8 : 16;
			imi0 = new ImageInfo(cols, rows, bitdepth, hasalpha & !withpalette, gray, withpalette);
			computePlteAndTrns(imi0);
		}

		private void computePlteAndTrns(ImageInfo imi) {
			if (!(image.getColorModel() instanceof IndexColorModel))
				return;
			IndexColorModel icm = (IndexColorModel) image.getColorModel();
			int[] bitsPentry = icm.getComponentSize();
			if (bitsPentry.length == 3 && hasalpha)
				throw new PngjException("bad lut 1");
			if (bitsPentry.length == 4 && !hasalpha)
				throw new PngjException("bad lut 2");
			int len = icm.getMapSize();
			plteChunk = new PngChunkPLTE(imi);
			plteChunk.setNentries(len);
			if (hasalpha) {
				trnsChunk = new PngChunkTRNS(imi);
				trnsChunk.setNentriesPalAlpha(len);
			}
			for (int i = 0; i < len; i++) {
				int r = icm.getRed(i); // TODO: see if the sRGB scaling does not break something
				int g = icm.getGreen(i);
				int b = icm.getBlue(i);
				int a = hasalpha ? icm.getAlpha(i) : 255;
				plteChunk.setEntry(i, r, g, b);
				if (hasalpha)
					trnsChunk.setEntryPalAlpha(i, a);
			}
		}

		public PngChunkTRNS getTrnsChunk() {
			return trnsChunk;
		}

		public PngChunkPLTE getPlteChunk() {
			return plteChunk;
		}

	}

	public int getRowNumber() {
		return rowNumber;
	}

	// we copy as RGB and RGBA, we fix that if needed in endReadFromPngRaw()
	public void readFromPngRaw(byte[] raw, final int len, final int offset, final int step) {
		final WritableRaster raster = bi.getRaster();
		final byte[] datab = adapter2bi.getDatatype() == DataBuffer.TYPE_BYTE
				? ((DataBufferByte) raster.getDataBuffer()).getData()
				: null;
		final short[] datas = adapter2bi.getDatatype() == DataBuffer.TYPE_USHORT
				? ((DataBufferUShort) raster.getDataBuffer()).getData()
				: null;
		final int samples = ((len - 1) * 8) / imgInfo.bitDepth;
		final int pixels = samples / imgInfo.channels;
		final int step1 = (step - 1) * imgInfo.channels;
		int irp, idp; // irp=index in raw, idp:index in data, advances by pixel bordes; idp in data in
		// samples, moves up and
		// down
		irp = 1;
		idp = imgInfo.channels * (imgInfo.cols * rowNumber + offset);
		int idpmax = idp + datalen; // notincluded
		if (imgInfo.bitDepth == 8) {
			if (step == 1) { // most usual
				System.arraycopy(raw, 1, datab, idp, samples);
			} else { // this should only happen for interlaced
				for (int p = 0; p < pixels; p++) {
					for (int c = 0; c < imgInfo.channels; c++)
						datab[idp++] = raw[irp++];
					idp += step1;
				}
			}
		} else if (imgInfo.bitDepth == 16) {
			if (step == 1) {
				for (int s = 0; s < samples; s++) {
					datas[idp++] = (short) (((raw[irp++] & 0xFF) << 8) | (raw[irp++] & 0xFF));
				}
			} else { // 16bpp interlaced
				for (int p = 0; p < pixels; p++) {
					for (int c = 0; c < imgInfo.channels; c++) {
						datas[idp++] = (short) (((raw[irp++] & 0xFF) << 8) | (raw[irp++] & 0xFF));
					}
					idp += step1;
				}
			}
		} else { // packed formats
			int mask0, mask, shi, bd;
			bd = imgInfo.bitDepth;
			mask0 = ImageLineHelper.getMaskForPackedFormats(bd);
			for (int c = 0; irp < len; irp++) {
				mask = mask0;
				shi = 8 - bd;
				do {
					datab[idp++] = (byte) ((raw[irp] & mask) >> shi);
					mask >>= bd;
					shi -= bd;
					c++;
					if (c == imgInfo.channels) {
						c = 0;
						idp += step1;
					}
				} while (mask != 0 && idp < idpmax);
			}
		}
	}

	public void endReadFromPngRaw() { // fixes order if necessary
		final int offsetd = imgInfo.channels * (imgInfo.cols * rowNumber);
		final int offsetdm = offsetd + datalen;
		if (imgInfo.channels > 2 && adapter2bi.isBgrOrder()) {
			// if (rowNumber == 0) System.err.println("fixing order");

			if (adapter2bi.getDatatype() == DataBuffer.TYPE_BYTE) {
				byte b;
				final byte[] datab = ((DataBufferByte) imageLineSetBI.image.getRaster().getDataBuffer()).getData();
				if (imgInfo.channels == 3) {
					for (int i = offsetd, i2 = i + 2; i < offsetdm; i += 3, i2 += 3) {
						b = datab[i];
						datab[i] = datab[i2];
						datab[i2] = b;
					}
				} else {
					for (int i = offsetd, i2 = i + 3; i < offsetdm; i += 4, i2 += 4) {
						b = datab[i];
						datab[i] = datab[i2];
						datab[i2] = b;
						b = datab[i + 1];
						datab[i + 1] = datab[i2 - 1];
						datab[i2 - 1] = b;
					}
				}
			} else {
				final short[] datas = ((DataBufferUShort) imageLineSetBI.image.getRaster().getDataBuffer()).getData();
				short s;
				if (imgInfo.channels == 3) {
					for (int i = offsetd, i2 = i + 2; i < offsetdm; i += 3, i2 += 3) {
						s = datas[i];
						datas[i] = datas[i2];
						datas[i2] = s;
					}
				} else {
					for (int i = offsetd, i2 = i + 3; i < offsetdm; i += 4, i2 += 4) {
						s = datas[i];
						datas[i] = datas[i2];
						datas[i2] = s;
						s = datas[i + 1];
						datas[i + 1] = datas[i2 - 1];
						datas[i2 - 1] = s;
					}
				}
			}
		}
	}

	public void writeToPngRaw(byte[] raw) {
		final WritableRaster raster = imageLineSetBI.image.getRaster();
		final int samples = datalen;
		final int pixels = samples / imgInfo.channels;
		if (adapter2png.resortToGetRGB) { // we dont' read the buffers at low level
			if (imgInfo.indexed)
				throw new PngjException("cant resort to RGB with palette");
			for (int c = 0, ir = 1; c < pixels; c++) {
				int color = bi.getRGB(c, rowNumber);
				raw[ir++] = (byte) ((color >> 16) & 0xff); // r (o gray)
				if (imgInfo.bitDepth == 16)
					ir++;
				if (imgInfo.channels > 1) {
					raw[ir++] = (byte) ((color >> (imgInfo.channels == 2 ? 24 : 8)) & 0xff); // g o a
					if (imgInfo.bitDepth == 16)
						ir++;
					if (imgInfo.channels > 2) {
						raw[ir++] = (byte) ((color) & 0xff); // g
						if (imgInfo.bitDepth == 16)
							ir++;
						if (imgInfo.channels == 4) {
							raw[ir++] = (byte) ((color >> 24) & 0xff); // a
							if (imgInfo.bitDepth == 16)
								ir++;
						}
					}
				}
			}
		} else { // we read databuffers at low level
			if (adapter2png.packedInInt) { // ufff TYPE_INT_ARGB TYPE_INT_BGR TYPE_INT_RGB
				DataBufferInt buf = (DataBufferInt) raster.getDataBuffer();
				final int[] datai = buf.getData();
				byte a = 0, r, g, b;
				int v;
				for (int ir = 1, id = rowNumber * imgInfo.cols, p = 0; p < imgInfo.cols; p++, id++) {
					v = datai[id];
					if (adapter2png.channels == 4)
						a = (byte) ((v >> 24) & 0xff);
					r = (byte) ((v >> 16) & 0xff);
					g = (byte) ((v >> 8) & 0xff);
					b = (byte) ((v) & 0xff);
					raw[ir++] = adapter2png.reverseOrder ? b : r;
					raw[ir++] = g;
					raw[ir++] = adapter2png.reverseOrder ? r : b;
					if (adapter2png.channels == 4)
						raw[ir++] = a;
				}
			} else if (adapter2png.datasize == 1) {
				final byte[] datab = ((DataBufferByte) raster.getDataBuffer()).getData();
				int id = rowNumber * imgInfo.samplesPerRow;
				int ir = 1;
				byte b;
				if (imgInfo.channels == 1)
					System.arraycopy(datab, id, raw, ir, imgInfo.samplesPerRow); // sweet!
				else {
					for (int p = 0; p < imgInfo.cols; p++) { // firt pass: assume no revers
						raw[ir++] = datab[id++];
						raw[ir++] = datab[id++];
						if (imgInfo.channels >= 3) {
							raw[ir++] = datab[id++];
							if (imgInfo.channels >= 4) {
								raw[ir++] = datab[id++];
							}
						}
					}
					if (adapter2png.reverseOrder) {
						ir = 1;
						int ir2 = ir + imgInfo.channels - 1;
						for (int p = 0; p < imgInfo.cols; p++, ir += imgInfo.channels, ir2 += imgInfo.channels) {
							b = raw[ir];
							raw[ir] = raw[ir2];
							raw[ir2] = b;
							if (imgInfo.channels == 4) {
								b = raw[ir + 1];
								raw[ir + 1] = raw[ir2 - 1];
								raw[ir2 - 1] = b;
							}
						}
					}
				}
			} else if (adapter2png.datasize == 2) { // 16 bits?
				final short[] datas = ((DataBufferUShort) raster.getDataBuffer()).getData();
				int id = rowNumber * imgInfo.samplesPerRow;
				int ir = 1;
				byte b;
				for (int p = 0; p < imgInfo.cols; p++) { // firt pass: assume no revers
					raw[ir++] = (byte) ((datas[id] >> 8) & 0xff);
					raw[ir++] = (byte) ((datas[id++]) & 0xff);
					if (imgInfo.channels >= 2) {
						raw[ir++] = (byte) ((datas[id] >> 8) & 0xff);
						raw[ir++] = (byte) ((datas[id++]) & 0xff);
						if (imgInfo.channels >= 3) {
							raw[ir++] = (byte) ((datas[id] >> 8) & 0xff);
							raw[ir++] = (byte) ((datas[id++]) & 0xff);
							if (imgInfo.channels >= 4) {
								raw[ir++] = (byte) ((datas[id] >> 8) & 0xff);
								raw[ir++] = (byte) ((datas[id++]) & 0xff);
							}
						}
					}
				}
				if (adapter2png.reverseOrder && imgInfo.channels > 1) {
					ir = 1;
					int ir2 = ir + (imgInfo.channels - 1) * 2;
					int stepIr = (imgInfo.channels == 2 ? 2 : 4);
					int stepIr2 = (imgInfo.channels == 4 ? 9 : stepIr + 1); // very dark magic
					for (int p = 0; p < imgInfo.cols; p++, ir += stepIr, ir2 += stepIr2) {
						b = raw[ir];
						raw[ir++] = raw[ir2];
						raw[ir2++] = b;
						b = raw[ir];
						raw[ir++] = raw[ir2];
						raw[ir2] = b;
						if (imgInfo.channels == 4) {
							ir2 -= 3;
							b = raw[ir];
							raw[ir++] = raw[ir2];
							raw[ir2++] = b;
							b = raw[ir];
							raw[ir++] = raw[ir2];
							raw[ir2] = b;
						}
					}
				}
			} else
				throw new PngjException("unexpected case " + adapter2png);
		}

	}

	static ColorSpace createColorSpace(boolean isgray) { // is this right?
		return isgray ? ColorSpace.getInstance(ColorSpace.CS_GRAY) : ColorSpace.getInstance(ColorSpace.CS_sRGB); // For
		// GA
		// we
		// should
		// use
		// other?
	}

	static boolean allEqual(int[] s) {
		for (int i = 1; i < s.length; i++)
			if (s[i] != s[i - 1])
				return false;
		return true;
	}

	// returns 1 if (0 1 2 . .) -1 if ( . .. 2 1 0), 0 elsewhere
	static int increasing(int[] s) {
		if (s[0] == 0) {
			for (int i = 1; i < s.length; i++)
				if (s[i] != i)
					return 0;
			return 1;
		}
		for (int i = 0, j = s.length - 1; i < s.length; i++, j--)
			if (s[j] != i)
				return 0;
		return -1;
	}

}
