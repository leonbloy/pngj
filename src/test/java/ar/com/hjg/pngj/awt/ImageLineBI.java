package ar.com.hjg.pngj.awt;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;

public class ImageLineBI implements IImageLine {
  private final ImageLineSetBI imageLineSetBI;
  private final ImageInfo imgInfo;
  private final Png2BufferedImageAdapter adapter2bi;
  private final BufferedImage2PngAdapter adapter2png;
  private int rowNumber = -1;

  private boolean hasAlpha;
  private int rowLength;
  private boolean bgrOrder;
  private byte[] bytes;
  public final int datalen; // length in samples of the row

  public ImageLineBI(ImageLineSetBI imageLineBiSet, int rowInBI) {
    this.imageLineSetBI = imageLineBiSet;
    this.imgInfo = imageLineBiSet.iminfo;
    adapter2bi = imageLineBiSet.adapter2bi;
    adapter2png = imageLineBiSet.adapter2png;
    this.rowNumber = rowInBI;
    datalen = imgInfo.cols * imgInfo.channels;
  }

  // we copy as RGB and RGBA, we fix that if needed in endReadFromPngRaw()
  public void readFromPngRaw(byte[] raw, final int len, final int offset, final int step) {
    final WritableRaster raster = imageLineSetBI.image.getRaster();
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
    int idpmax = idp+datalen; // notincluded
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

  public void writeToPngRaw(byte[] raw) {
    // TODO: this should be checked elsewhere
    if (imgInfo.bytesPerRow != rowLength)
      throw new RuntimeException("??");
    if (rowNumber < 0 || rowNumber >= imgInfo.rows)
      throw new RuntimeException("???");

    int bytesIdx = rowLength * rowNumber;
    int i = 1;
    if (hasAlpha) {
      if (bgrOrder) {
        while (i <= rowLength) {
          final byte a = bytes[bytesIdx++];
          final byte b = bytes[bytesIdx++];
          final byte g = bytes[bytesIdx++];
          final byte r = bytes[bytesIdx++];
          raw[i++] = r;
          raw[i++] = g;
          raw[i++] = b;
          raw[i++] = a;
        }
      } else {
        while (i <= rowLength) {
          raw[i++] = bytes[bytesIdx++];
          raw[i++] = bytes[bytesIdx++];
          raw[i++] = bytes[bytesIdx++];
          raw[i++] = bytes[bytesIdx++];
        }
      }
    } else {
      if (bgrOrder) {
        while (i <= rowLength) {
          final byte b = bytes[bytesIdx++];
          final byte g = bytes[bytesIdx++];
          final byte r = bytes[bytesIdx++];
          raw[i++] = r;
          raw[i++] = g;
          raw[i++] = b;
        }
      } else {
        while (i <= rowLength) {
          raw[i++] = bytes[bytesIdx++];
          raw[i++] = bytes[bytesIdx++];
          raw[i++] = bytes[bytesIdx++];
        }
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

  public int getRowNumber() {
    return rowNumber;
  }


}
