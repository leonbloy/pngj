package ar.com.hjg.pngj.awt;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class ImageIoUtils {
  // only for testing, development

  static public String imageTypeNameShort(int imtype) {
    String t = imageTypeName(imtype);
    t = t.substring(t.indexOf('_') + 1);
    t = t.replaceAll("BYTE_", "b_");
    t = t.replaceAll("CUSTOM_", "cu_");
    t = t.replaceAll("INT_", "i_");
    t = t.replaceAll("USHORT_", "u_");
    t = t.replaceAll("_PRE", "pre");
    return t;
  }

  static public String imageTypeName(int imtype) {
    switch (imtype) {
      case BufferedImage.TYPE_3BYTE_BGR:
        return "TYPE_3BYTE_BGR";
      case BufferedImage.TYPE_4BYTE_ABGR:
        return "TYPE_4BYTE_ABGR";
      case BufferedImage.TYPE_4BYTE_ABGR_PRE:
        return "TYPE_4BYTE_ABGR_PRE";
      case BufferedImage.TYPE_BYTE_BINARY:
        return "TYPE_BYTE_BINARY";
      case BufferedImage.TYPE_BYTE_GRAY:
        return "TYPE_BYTE_GRAY";
      case BufferedImage.TYPE_BYTE_INDEXED:
        return "TYPE_BYTE_INDEXED";
      case BufferedImage.TYPE_CUSTOM:
        return "TYPE_CUSTOM";
      case BufferedImage.TYPE_INT_ARGB:
        return "TYPE_INT_ARGB";
      case BufferedImage.TYPE_INT_ARGB_PRE:
        return "TYPE_INT_ARGB_PRE";
      case BufferedImage.TYPE_INT_BGR:
        return "TYPE_INT_BGR";
      case BufferedImage.TYPE_INT_RGB:
        return "TYPE_INT_RGB";
      case BufferedImage.TYPE_USHORT_555_RGB:
        return "TYPE_USHORT_555_RGB";
      case BufferedImage.TYPE_USHORT_565_RGB:
        return "TYPE_USHORT_565_RGB";
      case BufferedImage.TYPE_USHORT_GRAY:
        return "TYPE_USHORT_GRAY";
    }
    return "TYPE_UNKNOWN_" + String.valueOf(imtype);
  }


  public static ImageWriter getJavaImageWriter(boolean preferBetter) {
    ImageWriter imwriter = null;
    // com.sun.media.imageioimpl.plugins.png.CLibPNGImageWriter (better)
    // com.sun.imageio.plugins.png.PNGImageWriter
    List<ImageWriter> list = new ArrayList<ImageWriter>();
    for (Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("PNG"); iter.hasNext();) {
      list.add(iter.next());
    }

    for (ImageWriter iw : list) {
      String cname = iw.getOriginatingProvider().getPluginClassName();
      if (cname.startsWith("com.sun.media.imageio") && preferBetter) {
        imwriter = iw;
        break;
      }
      if (cname.startsWith("com.sun.imageio.plugins.png.") && !preferBetter) {
        imwriter = iw;
        break;
      }
    }
    if (imwriter == null)
      imwriter = list.get(0); // whatever

    return imwriter;
  }

  public static ImageReader getJavaImageReader(boolean preferBetter) {
    ImageReader imwriter = null;
    // com.sun.media.imageioimpl.plugins.png.CLibPNGImageWriter (better)
    // com.sun.imageio.plugins.png.PNGImageWriter
    List<ImageReader> list = new ArrayList<ImageReader>();
    for (Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("PNG"); iter.hasNext();) {
      list.add(iter.next());
    }

    for (ImageReader iw : list) {
      String cname = iw.getOriginatingProvider().getPluginClassName();
      if (cname.startsWith("com.sun.media.imageio") && preferBetter) {
        imwriter = iw;
        break;
      }
      if (cname.startsWith("com.sun.imageio.plugins.png.") && !preferBetter) {
        imwriter = iw;
        break;
      }
    }
    if (imwriter == null)
      imwriter = list.get(0);

    return imwriter;
  }

  public static void writePng(File f, BufferedImage bi) {
    writePng(f, bi, false);
  }

  public static void writePng(File f, BufferedImage bi, boolean preferBetter) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(f);
      writePng(fos, bi, preferBetter);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (fos != null)
        try {
          fos.close();
        } catch (Exception e2) {
        }
    }


  }

  public static void writePng(OutputStream os, BufferedImage bi, boolean preferBetter)
      throws IOException {
    ImageWriter iw = getJavaImageWriter(preferBetter);
    ImageOutputStream ios = ImageIO.createImageOutputStream(os);
    iw.setOutput(ios);
    iw.write(bi);
    ios.close();
  }

  public static BufferedImage readPng(File f) {
    return readPng(f, false);
  }

  public static BufferedImage readPng(File f, boolean preferBetter) {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(f);
      return readPng(fis, preferBetter);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (fis != null)
        try {
          fis.close();
        } catch (Exception e2) {
        }
    }
  }

  // returns [R G B A R G B A ...] one int per sample (it really could fit in byte[] but we assume that does not matter
  // here)
  public static int[] getRowRgba8(BufferedImage im, int rown, int[] buf) {
    int pixels = im.getWidth();
    int samples = pixels * 4;
    if (buf == null || buf.length < samples)
      buf = new int[samples];
    im.getRGB(0, rown, pixels, 1, buf, 0, pixels);
    for (int p = pixels - 1, s = samples - 1; p >= 0; p--) {
      int v = buf[p];
      buf[s--] = ((v >> 24) & 0xff); // A
      buf[s--] = (v & 0xff); // B
      buf[s--] = ((v >> 8) & 0xff); // G
      buf[s--] = ((v >> 16) & 0xff); // R
    }
    return buf;
  }

  public static BufferedImage readPng(InputStream is, boolean preferBetter) throws IOException {
    ImageReader ir = getJavaImageReader(preferBetter);
    ImageInputStream iis = ImageIO.createImageInputStream(is);
    ir.setInput(iis);
    return ir.read(0);
  }
}
