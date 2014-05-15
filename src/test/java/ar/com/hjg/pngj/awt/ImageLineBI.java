package ar.com.hjg.pngj.awt;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngjException;

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

  public int getRowNumber() {
    return rowNumber;
  }

  // we copy as RGB and RGBA, we fix that if needed in endReadFromPngRaw()
  public void readFromPngRaw(byte[] raw, final int len, final int offset, final int step) {
    final WritableRaster raster = bi.getRaster();
    final byte[] datab =
        adapter2bi.getDatatype() == DataBuffer.TYPE_BYTE ? ((DataBufferByte) raster.getDataBuffer())
            .getData() : null;
    final short[] datas =
        adapter2bi.getDatatype() == DataBuffer.TYPE_USHORT ? ((DataBufferUShort) raster
            .getDataBuffer()).getData() : null;
    final int samples = ((len - 1) * 8) / imgInfo.bitDepth;
    final int pixels = samples / imgInfo.channels;
    final int step1 = (step - 1) * imgInfo.channels;
    int irp, idp; // irp=index in raw, idp:index in data, advances by pixel bordes; idp in data in samples, moves up and down
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
      if (rowNumber == 0)
        System.err.println("fixing order");

      if (adapter2bi.getDatatype() == DataBuffer.TYPE_BYTE) {
        byte b;
        final byte[] datab =
            ((DataBufferByte) imageLineSetBI.image.getRaster().getDataBuffer()).getData();
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
        final short[] datas =
            ((DataBufferUShort) imageLineSetBI.image.getRaster().getDataBuffer()).getData();
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

}
