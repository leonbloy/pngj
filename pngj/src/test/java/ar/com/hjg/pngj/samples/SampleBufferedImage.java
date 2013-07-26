package ar.com.hjg.pngj.samples;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;

/**
 * NOT WORKING YET - DONT USE THIS
 */
public class SampleBufferedImage {

	public static BufferedImage readAsBufferedImage(PngReader png1) throws Exception {
		return null;
	}

	public static void write(BufferedImage buf, OutputStream os) {
		int buftype = buf.getType();
		boolean alpha = false, indexed = false, alphapre = false, gray = false, depth16 = false;
		switch (buftype) {
		case BufferedImage.TYPE_INT_ARGB:
		case BufferedImage.TYPE_4BYTE_ABGR:
			alpha = true;
		case BufferedImage.TYPE_4BYTE_ABGR_PRE: // fallback!
		case BufferedImage.TYPE_INT_ARGB_PRE:
			alphapre = true;
			break;
		case BufferedImage.TYPE_BYTE_GRAY:
		case BufferedImage.TYPE_BYTE_BINARY:
			gray = true;
		case BufferedImage.TYPE_USHORT_GRAY: // fallback!
			depth16 = true;
			break;
		case BufferedImage.TYPE_BYTE_INDEXED:
			if (buf.getColorModel() instanceof IndexColorModel)
				indexed = true;
			break;
		default:
			;
		}
		ImageInfo imi = new ImageInfo(buf.getWidth(), buf.getHeight(), depth16 ? 16 : 8, alpha, gray, indexed);

	}

	public static void writeRgb8(BufferedImage buf, OutputStream os) {
		int buftype = buf.getType();
		boolean alpha = (buftype == BufferedImage.TYPE_INT_ARGB || buftype == BufferedImage.TYPE_4BYTE_ABGR
				|| buftype == BufferedImage.TYPE_4BYTE_ABGR_PRE || buftype == BufferedImage.TYPE_INT_ARGB_PRE);
		ImageInfo imi = new ImageInfo(buf.getWidth(), buf.getHeight(), 8, alpha);
		PngWriter pngw = new PngWriter(os, imi);
		ImageLineInt line = new ImageLineInt(imi);
		buf.getRaster();
		for (int row = 0; row < imi.rows; row++) {
			for (int col = 0; col < imi.cols; col++) {
				int p = buf.getRGB(col, row);
				if (col == imi.cols / 2)
					System.out.println(row + " " + ((p >> 24) & 0xFF) + " " + ((p >> 16) & 0xFF) + "-"
							+ ((p >> 8) & 0xFF) + " -" + (p & 0xFF));
				if (!alpha)
					ImageLineHelper.setPixelRGB8(line, col, p);
				else
					ImageLineHelper.setPixelRGBA8(line, col, p);
			}
			pngw.writeRow(line, row);

		}
		pngw.end();
	}

	public static void writeRgb8Ori(BufferedImage buf, OutputStream os) {
		int buftype = buf.getType();
		boolean alpha = (buftype == BufferedImage.TYPE_INT_ARGB || buftype == BufferedImage.TYPE_4BYTE_ABGR
				|| buftype == BufferedImage.TYPE_4BYTE_ABGR_PRE || buftype == BufferedImage.TYPE_INT_ARGB_PRE);
		ImageInfo imi = new ImageInfo(buf.getWidth(), buf.getHeight(), 8, alpha);
		PngWriter pngw = new PngWriter(os, imi);
		ImageLineInt line = new ImageLineInt(imi);
		for (int row = 0; row < imi.rows; row++) {
			for (int col = 0; col < imi.cols; col++) {
				int p = buf.getRGB(col, row);
				if (col == imi.cols / 2)
					System.out.println(row + " " + ((p >> 24) & 0xFF) + " " + ((p >> 16) & 0xFF) + "-"
							+ ((p >> 8) & 0xFF) + " -" + (p & 0xFF));
				if (!alpha)
					ImageLineHelper.setPixelRGB8(line, col, p);
				else
					ImageLineHelper.setPixelRGBA8(line, col, p);
			}
			pngw.writeRow(line, row);
		}
		pngw.end();
	}
	
	/**
	 * 
	 * @param bi BufferedImage of TYPE_INT_ARGB or TYPE_INT_RGB
	 * @param os
	 * @param useAlpha
	 */
	public static void writeARGB(BufferedImage bi, OutputStream os) {
		if(bi.getType() != BufferedImage.TYPE_INT_ARGB) throw new PngjException("This method expects  BufferedImage.TYPE_INT_ARGB" );
		ImageInfo imi = new ImageInfo(bi.getWidth(), bi.getHeight(), 8, true);
		PngWriter pngw = new PngWriter(os, imi);
		// pngw.setCompLevel(6); // tuning
		// pngw.setFilterType(FilterType.FILTER_PAETH); // tuning
		DataBufferInt db =((DataBufferInt) bi.getRaster().getDataBuffer());
		if(db.getNumBanks()!=1) throw new PngjException("This method expects one bank");
		SinglePixelPackedSampleModel samplemodel =  (SinglePixelPackedSampleModel) bi.getSampleModel();
		ImageLineByte line = new ImageLineByte(imi);
		int[] dbbuf = db.getData();
		byte[] scanline = line.getScanline();
		for (int row = 0; row < imi.rows; row++) {
			int elem=samplemodel.getOffset(0,row);
			for (int col = 0,j=0; col < imi.cols; col++) {
				int sample = dbbuf[elem++];
				scanline[j++] =  (byte) ((sample & 0xFF0000)>>16); // R
				scanline[j++] =  (byte) ((sample & 0xFF00)>>8); // G
				scanline[j++] =  (byte) (sample & 0xFF); // B
				scanline[j++] =  (byte) (((sample & 0xFF000000)>>24)&0xFF); // A
			}
			pngw.writeRow(line, row);
		}
		pngw.end();
	}
	
	
	public static void writeTYPE_4BYTE_ABGR(BufferedImage bi, OutputStream os) {
		if(bi.getType() != BufferedImage.TYPE_4BYTE_ABGR) throw new PngjException("This method expects  BufferedImage.TYPE_4BYTE_ABGR" );
		ImageInfo imi = new ImageInfo(bi.getWidth(), bi.getHeight(), 8, true);
		PngWriter pngw = new PngWriter(os, imi);
		// pngw.setCompLevel(6); // tuning
		// pngw.setFilterType(FilterType.FILTER_PAETH); // tuning
		DataBufferByte db =((DataBufferByte) bi.getRaster().getDataBuffer());
		ComponentSampleModel samplemodel =  (ComponentSampleModel) bi.getSampleModel();
		ImageLineByte line = new ImageLineByte(imi);
		if(db.getNumBanks()!=1) throw new PngjException("This method expects one bank");
		byte[] dbbuf = db.getData();
		byte[] scanline = line.getScanline();
		for (int row = 0; row < imi.rows; row++) {
			int elem=samplemodel.getOffset(0,row);
			for (int col = 0,j=0; col < imi.cols; col++,elem+=7) {
				scanline[j++] =  dbbuf[elem--];
				scanline[j++] =  dbbuf[elem--];
				scanline[j++] =  dbbuf[elem--];
				scanline[j++] =  dbbuf[elem];
			}
			pngw.writeRow(line, row);
		}
		pngw.end();
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
		return "unknown image type #" + imtype;
	}


	public static BufferedImage readAsBiType(File file,int bufferedImageType) throws IOException {
		BufferedImage bi = ImageIO.read(file);
		BufferedImage bi2 = bi;
		System.out.println("Type: " + imageTypeName(bi.getType()) + " -> " + imageTypeName(bufferedImageType));
		if(bi.getType() != bufferedImageType) {
			bi2 = new BufferedImage(bi.getWidth(), bi.getHeight(), bufferedImageType);
			Graphics2D g = bi2.createGraphics();
			g.drawImage(bi, 0, 0, null);
			g.dispose();
		}
		return bi2; 
	}

	public static void testTYPE_4BYTE_ABGR() throws IOException {
		File dest = new File("C:/temp/big2.png");
		OutputStream fos = new BufferedOutputStream(new FileOutputStream(dest));
		BufferedImage bi = ImageIO.read(new File("C:/temp/big.png"));
		long t0=System.currentTimeMillis();
		writeTYPE_4BYTE_ABGR(bi,fos);
		long t1=System.currentTimeMillis();
		System.out.printf("Done %s (%d msecs)\n",dest.getAbsolutePath(),t1-t0);
	}

	public static void testARGB() throws IOException {
		File dest = new File("C:/temp/big2.png");
		OutputStream fos = new BufferedOutputStream(new FileOutputStream(dest));
		BufferedImage bi = readAsBiType(new File("C:/temp/big.png"),BufferedImage.TYPE_INT_ARGB);
		long t0=System.currentTimeMillis();
		writeARGB(bi,fos);
		long t1=System.currentTimeMillis();
		System.out.printf("Done %s (%d msecs)\n",dest.getAbsolutePath(),t1-t0);
	}

	
	public static void main(String[] args) throws Exception {
		testTYPE_4BYTE_ABGR();
	}
}
