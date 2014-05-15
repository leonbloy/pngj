package ar.com.hjg.pngj.awt;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

public class Png2BufferedImageAdapter {

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
    transparency =
        iminfo.alpha || (trns != null && iminfo.indexed && usetrnsForPallete)
            || (trns != null && (!iminfo.indexed) && usetrnsForNonPalette);
    withPalette = iminfo.indexed || (iminfo.greyscale && iminfo.bitDepth < 8);
    datatype = iminfo.bitDepth == 16 ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;
    if (iminfo.bitDepth == 16) {
      biType =
          (iminfo.greyscale && !iminfo.alpha) ? BufferedImage.TYPE_USHORT_GRAY
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
    ComponentColorModel colorModel =
        new ComponentColorModel(colorspace, nBits, hasAlpha, false,
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
      raster =
          Raster.createInterleavedRaster(useByte ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT,
              cols, rows, cols * iminfo.channels, iminfo.channels, bOffs, null);
    BufferedImage bi = new BufferedImage(colorModel, raster, false, null);
    return bi;

  }

  static ColorSpace createColorSpace(boolean isgray) { // is this right?
    return isgray ? ColorSpace.getInstance(ColorSpace.CS_GRAY) : ColorSpace
        .getInstance(ColorSpace.CS_sRGB); // For GA we should use other?
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
