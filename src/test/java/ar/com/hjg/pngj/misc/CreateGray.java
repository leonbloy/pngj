package ar.com.hjg.pngj.misc;

import java.io.File;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkGAMA;
import ar.com.hjg.pngj.chunks.PngChunkSRGB;
import ar.com.hjg.pngj.chunks.PngChunkTEXT;
import ar.com.hjg.pngj.test.TestSupport;

// to investigate sRGB and GAMA issues with grayscale images
// all images have same content, im(0,0)=128
public class CreateGray {

  public static void main(String[] args) {
    gray2(1, 8);
    gray2(2, 8);
    gray2(3, 8);
    gray2(4, 8);
    gray2(1, 16);
    gray2(2, 16);
    gray2(3, 16);
    gray2(4, 16);

  }

  private static void gray2(int channels, int bitdepth) {
    File dir = TestSupport.getTempDir();
    int size = 256;
    ImageInfo imi = new ImageInfo(size, size, bitdepth, channels % 2 == 0, channels < 3, false);
    if (imi.channels != channels)
      throw new RuntimeException("" + imi.channels + "!=" + channels);
    String name =
        String.format("%s%02d", (channels > 2 ? "RGB" : "G") + (imi.alpha ? "A" : ""), bitdepth)
            .toLowerCase();
    String desc =
        String.format("Gray in %s format, bitdepth=%d. ", (channels > 2 ? "RGB" : "G")
            + (imi.alpha ? "A" : ""), bitdepth);
    File dest1 = new File(dir, name + "-nosrgb.png");
    PngWriter pngw1 = new PngWriter(dest1, imi);
    pngw1.getChunksList().queue(new PngChunkTEXT(imi, "desc", desc + " No SRGB"));
    File dest2 = new File(dir, name + "-srgb.png");
    PngWriter pngw2 = new PngWriter(dest2, imi);
    pngw2.getChunksList().queue(new PngChunkTEXT(imi, "desc", desc + " With SRGB"));
    File dest3 = new File(dir, name + "-gama1.png");
    PngWriter pngw3 = new PngWriter(dest3, imi);
    pngw3.getChunksList().queue(new PngChunkTEXT(imi, "desc", desc + " With Gamma=1.0"));
    File dest4 = new File(dir, name + "-gama2_2.png");
    PngWriter pngw4 = new PngWriter(dest4, imi);
    pngw4.getChunksList().queue(new PngChunkTEXT(imi, "desc", desc + " With Gamma=2.2"));

    ImageLineInt line = new ImageLineInt(imi);
    PngChunkSRGB srgb = new PngChunkSRGB(imi);
    srgb.setIntent(PngChunkSRGB.RENDER_INTENT_Absolute_colorimetric); // not important?
    PngChunkGAMA gama1 = new PngChunkGAMA(imi);
    PngChunkGAMA gama22 = new PngChunkGAMA(imi);
    gama1.setGamma(1.0);
    gama22.setGamma(1.0 / 2.2);
    pngw2.getChunksList().queue(srgb);
    pngw3.getChunksList().queue(gama1);
    pngw4.getChunksList().queue(gama22);
    int[] data = line.getScanline();
    double esc = (1 << bitdepth);
    for (int r = 0; r < size; r++) {
      for (int c = 0; c < size; c++) {
        double v = grayAt(c, r, size);
        int b = (int) (v * esc);
        for (int t = 0; t < channels; t++) {
          data[c * channels + t] = b;
        }
      }
      pngw1.writeRow(line);
      pngw2.writeRow(line);
      pngw3.writeRow(line);
      pngw4.writeRow(line);
    }
    pngw1.end();
    pngw2.end();
    pngw3.end();
    pngw4.end();
  }


  static double grayAt(int col, int row, int size) {
    double x = col / (double) size, y = row / (double) size;
    double c2 = x;
    if (y > 0.5)
      c2 *= 4;
    double v = (c2 - 0.5 - y);
    if (y >= 0.5 && y < 0.5 + 24 / 256.0)
      v = ((int) (Math.floor(x * size / 2.0) + Math.floor(y * size / 2.0))) % 2;
    while (v < 0)
      v += 1.0;
    while (v > 1)
      v -= 1.0;
    return v * 255.999999 / 256.0;
  }

}
