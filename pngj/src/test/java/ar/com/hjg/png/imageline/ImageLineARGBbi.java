package ar.com.hjg.png.imageline;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineFactory;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;

/*
 * INCOMPLETE IMPLEMENTATION
 * 
 */
public class ImageLineARGBbi implements IImageLine {

	public final ImageInfo imgInfo;
	private final BufferedImage image;
	private int rowNumber = -1;
	private boolean hasAlpha;
	private int rowLength;
	private boolean bgrOrder;
	private byte[] bytes;

	public ImageLineARGBbi(ImageInfo imgInfo, BufferedImage bi, byte[] bytesdata) {
		this.imgInfo = imgInfo;
		this.image = bi;
		this.bytes = bytesdata;
		//bytes = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		hasAlpha = image.getColorModel().hasAlpha();
		if (hasAlpha) {
			rowLength = image.getWidth() * 4;
		} else {
			rowLength = image.getWidth() * 3;
		}
		bgrOrder = ((ComponentSampleModel) image.getSampleModel()).getBandOffsets()[0] != 0;
	}

	public static IImageLineFactory<ImageLineByte> getFactory(ImageInfo iminfo) {
		return new IImageLineFactory<ImageLineByte>() {
			public ImageLineByte createImageLine(ImageInfo iminfo) {
				return new ImageLineByte(iminfo);
			}
		};
	}

	public void readFromPngRaw(byte[] raw, final int len, final int offset, final int step) {
		throw new RuntimeException("not implemented");
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

	public void endReadFromPngRaw() {
		throw new RuntimeException("not implemented");
	}

	public int getRowNumber() {
		return rowNumber;
	}

	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

}
