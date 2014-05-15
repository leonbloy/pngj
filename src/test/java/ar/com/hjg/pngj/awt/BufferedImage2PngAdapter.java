package ar.com.hjg.pngj.awt;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

public class BufferedImage2PngAdapter {

  public final BufferedImage image;
  public boolean forceresortToGetRGB = false;

  // WARNING: these defaults are assumed in computeParams
  int datasize = 1; //1:byte, 2short, 4 int
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
    ImageInfo iminfo =
        nlines == imi0.rows ? imi0 : new ImageInfo(imi0.cols, nlines, imi0.bitDepth, imi0.alpha,
            imi0.greyscale, imi0.indexed);
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
          // see if it's not one of our own nice formats.. All this is quite related to Png2BufferedImageAdapter.createBufferedImageCustom()
          if (!(colormodel instanceof ComponentColorModel))
            break;// no luck
          if (colormodel.getColorSpace() == Png2BufferedImageAdapter.createColorSpace(true)) {
            gray = true;
          } else {
            if (colormodel.getColorSpace() != Png2BufferedImageAdapter.createColorSpace(false))
              break;// unrecognized colorspace, no luck
          }
          int[] nbits = ((ComponentColorModel) colormodel).getComponentSize();
          datasize = nbits[0]/8;
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
            datasize = sizes[0]/8;
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
          } else if (sampleModel instanceof SinglePixelPackedSampleModel) { // we accept this only for one channel (gray 8 or 16 bits)
            int[] sizes = ((SinglePixelPackedSampleModel) sampleModel).getSampleSize();
            if (sizes.length != 1)
              break;
            datasize = sizes[0]/8;
            if (sizes[0] <8)
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
    int bitdepth = datasize ==1 || packedInInt ? 8 : 16;
    imi0 = new ImageInfo(cols, rows, bitdepth, hasalpha & !withpalette, gray, withpalette);
    computePlteAndTrns(imi0);
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
