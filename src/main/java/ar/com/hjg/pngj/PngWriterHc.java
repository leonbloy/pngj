package ar.com.hjg.pngj;

import java.io.File;
import java.io.OutputStream;

import ar.com.hjg.pngj.pixels.PixelsWriter;
import ar.com.hjg.pngj.pixels.PixelsWriterMultiple;

/** Pngwriter with High compression */
public class PngWriterHc extends PngWriter {

  public PngWriterHc(File file, ImageInfo imgInfo, boolean allowoverwrite) {
    super(file, imgInfo, allowoverwrite);
  }

  public PngWriterHc(File file, ImageInfo imgInfo) {
    super(file, imgInfo);
  }

  public PngWriterHc(OutputStream outputStream, ImageInfo imgInfo) {
    super(outputStream, imgInfo);
  }

  @Override
  protected PixelsWriter createPixelsWriter(ImageInfo imginfo) {
    PixelsWriterMultiple pw = new PixelsWriterMultiple(imginfo);
    return pw;
  }

  public PixelsWriterMultiple getPixelWriterMultiple() {
    return (PixelsWriterMultiple) pixelsWriter;
  }

}
