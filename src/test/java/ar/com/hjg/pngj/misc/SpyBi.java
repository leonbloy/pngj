package ar.com.hjg.pngj.misc;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngReaderByte;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.awt.ImageIoUtils;
import ar.com.hjg.pngj.cli.CliArgs;

public class SpyBi {

  static StringBuilder help = new StringBuilder();


  public static void run(String[] args) {
    help.append("Given a list of png files (or dir), outputs a summarry of BufferedImage properties, \n");
    help.append("as loaded by Java ImageIO.read \n");
    CliArgs cli = CliArgs.buildFrom(args, help);
    cli.checkAtLeastNargs(1);
    SpyBi me = new SpyBi();
    me.listpng = cli.listPngsFromArgs();
    cli.checkNoMoreOpts();
    me.doit();
  }

  public static final String BAD_OR_NOT_PNG = "BAD_OR_NOT_PNG_FILE";

  private List<File> listpng;

  private StringBuilder err = new StringBuilder();


  private SpyBi() {}

  private void doit() {
    for (File f : listpng)
      doitForFile(f);
    System.err.println(err);
  }

  String sumPng(PngReader png) {
    ImageInfo im = png.imgInfo;
    String res = "";
    res +=
        String.valueOf(im.bitDepth) + (im.alpha ? "a" : "") + (im.indexed ? "p" : "")
            + (im.greyscale ? "g" : "");
    // res += png.interlaced ? "i" : "n"; interlaced info is irrelevant
    res += png.getMetadata().getTRNS() != null ? "t" : "";
    // res+= im.cols*im.rows <1024? "S" : "L"; irrelevant
    return res;
  }

  String sumBi(BufferedImage bi) {
    String type = ImageIoUtils.imageTypeNameShort(bi.getType());
    ColorModel colorModel = bi.getColorModel();
    String colorModelClass = colorModel.getClass().getSimpleName().replaceAll("ColorModel", "");
    boolean isAlphaPre = bi.isAlphaPremultiplied();
    int pixelSize = colorModel.getPixelSize();
    if (colorModel.isAlphaPremultiplied() != isAlphaPre)
      throw new RuntimeException("?");
    boolean hasAlpha = colorModel.hasAlpha();
    String componentsSize = Arrays.toString(colorModel.getComponentSize());
    ColorSpace colorSpace = colorModel.getColorSpace();
    boolean issRGB = colorSpace.isCS_sRGB();
    SampleModel sampleModel = bi.getSampleModel();
    if (sampleModel instanceof PixelInterleavedSampleModel
        && bi.getType() == BufferedImage.TYPE_CUSTOM) {
      PixelInterleavedSampleModel sm2 = (PixelInterleavedSampleModel) sampleModel;
    }
    String boff =
        sampleModel instanceof PixelInterleavedSampleModel ? Arrays
            .toString(((PixelInterleavedSampleModel) sampleModel).getBandOffsets()) : "NA";
    String sampleModelClass = sampleModel.getClass().getSimpleName().replaceAll("SampleModel", "");
    WritableRaster raster = bi.getRaster();
    DataBuffer buffer = raster.getDataBuffer();
    String databufferclas = buffer.getClass().getSimpleName().replaceAll("DataBuffer", "");
    int bufferDataType = buffer.getDataType();
    String res =
        String.format(type + ";cm=" + colorModelClass + ",ps=" + pixelSize + ",ha="
            + (hasAlpha ? "Y" : "N") + ",ap=" + (isAlphaPre ? "Y" : "N") + ",cs=" + componentsSize
            + ";srgb=" + issRGB + ";sm=" + sampleModelClass + ";db=" + databufferclas + ","
            + bufferDataType + ",bo=" + boff);
    return res;
  }

  protected void doitForFile(File f) {
    File temp = new File(f.getParent(), "_tmp.png");
    temp.deleteOnExit();
    try {
      PngReaderByte png1 = new PngReaderByte(f);
      png1.end();
      String png1info = sumPng(png1);

      BufferedImage im = ImageIoUtils.readPng(f, false);
      String biinfo = sumBi(im);
      ImageIoUtils.writePng(temp, im, false);
      PngReaderByte png2 = new PngReaderByte(temp);
      String png2info = sumPng(png2);
      png2.end();
      System.out.printf("%s\t%s\t%s\t%s\n", f.getName(), png1info, biinfo, png2info);
    } catch (PngjException e) {
      err.append(String.format("%s\t%s\t%s\n", f.getName(), "PNGJ error", e.getMessage()));
    } catch (Exception e) {
      err.append(String.format("%s\t%s\t%s\n", f.getName(), "ImageIO error", e.getMessage()));
    }

  }



  public static void main(String[] args) {
    run(args);
    /*
     * run(new String[]{"d:\\devel\\repositories\\pngj\\priv\\imgsets\\2\\**",
     * "d:\\devel\\repositories\\pngj\\src\\test\\resources\\testsuite1\\"
     * ,"d:\\devel\\repositories\\pngj\\src\\test\\resources\\test\\"});
     */
  }
}
