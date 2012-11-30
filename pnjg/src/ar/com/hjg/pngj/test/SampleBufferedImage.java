package ar.com.hjg.pngj.test;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngWriter;

/**
 * NOT WORKING YET - DONT USE THIS
 */
public class SampleBufferedImage {

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
		boolean alpha = (buftype == BufferedImage.TYPE_INT_ARGB || buftype==  BufferedImage.TYPE_4BYTE_ABGR ||  buftype == BufferedImage.TYPE_4BYTE_ABGR_PRE || buftype == BufferedImage.TYPE_INT_ARGB_PRE);
		ImageInfo imi = new ImageInfo(buf.getWidth(), buf.getHeight(), 8, alpha);
		PngWriter pngw = new PngWriter(os, imi);
		ImageLine line = new ImageLine(imi);
		buf.getRaster();
		for(int row=0;row<imi.rows;row++) {
			for(int col=0;col<imi.cols;col++) {
				int p= buf.getRGB(col,row);
				if(col==imi.cols/2) System.out.println(row + " " + ((p>>24)&0xFF) +" " + ((p>>16)&0xFF) + "-" + ((p>>8)&0xFF) + " -" + (p&0xFF));
				if(!alpha)
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
		boolean alpha = (buftype == BufferedImage.TYPE_INT_ARGB || buftype==  BufferedImage.TYPE_4BYTE_ABGR ||  buftype == BufferedImage.TYPE_4BYTE_ABGR_PRE || buftype == BufferedImage.TYPE_INT_ARGB_PRE);
		ImageInfo imi = new ImageInfo(buf.getWidth(), buf.getHeight(), 8, alpha);
		PngWriter pngw = new PngWriter(os, imi);
		ImageLine line = new ImageLine(imi);
		for(int row=0;row<imi.rows;row++) {
			for(int col=0;col<imi.cols;col++) {
				int p= buf.getRGB(col,row);
				if(col==imi.cols/2) System.out.println(row + " " + ((p>>24)&0xFF) +" " + ((p>>16)&0xFF) + "-" + ((p>>8)&0xFF) + " -" + (p&0xFF));
				if(!alpha)
					ImageLineHelper.setPixelRGB8(line, col, p);
				else
					ImageLineHelper.setPixelRGBA8(line, col, p);
			}
			pngw.writeRow(line, row);
			
		}
		pngw.end();
	}

	static public String imageTypeName(BufferedImage img) {
		switch (img.getType()) {
		case BufferedImage.TYPE_3BYTE_BGR: return "TYPE_3BYTE_BGR";
		case BufferedImage.TYPE_4BYTE_ABGR: return "TYPE_4BYTE_ABGR";
		case BufferedImage.TYPE_4BYTE_ABGR_PRE: return "TYPE_4BYTE_ABGR_PRE";
		case BufferedImage.TYPE_BYTE_BINARY: return "TYPE_BYTE_BINARY";
		case BufferedImage.TYPE_BYTE_GRAY: return "TYPE_BYTE_GRAY";
		case BufferedImage.TYPE_BYTE_INDEXED: return "TYPE_BYTE_INDEXED";
		case BufferedImage.TYPE_CUSTOM: return "TYPE_CUSTOM";
		case BufferedImage.TYPE_INT_ARGB: return "TYPE_INT_ARGB";
		case BufferedImage.TYPE_INT_ARGB_PRE: return "TYPE_INT_ARGB_PRE";
		case BufferedImage.TYPE_INT_BGR: return "TYPE_INT_BGR";
		case BufferedImage.TYPE_INT_RGB: return "TYPE_INT_RGB";
		case BufferedImage.TYPE_USHORT_555_RGB: return "TYPE_USHORT_555_RGB";
		case BufferedImage.TYPE_USHORT_565_RGB: return "TYPE_USHORT_565_RGB";
		case BufferedImage.TYPE_USHORT_GRAY: return "TYPE_USHORT_GRAY";
		}
		return "unknown image type #" + img.getType();
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2 || args[0].equals(args[1])) {
			System.err.println("Arguments: [pngsrc] [pngdest]");
			System.exit(1);
		}
		BufferedImage im = ImageIO.read(new File(args[0]));
		int[] vals= im.getRaster().getPixel(im.getWidth()/2, im.getHeight()/2 ,(int[])null);
		int p = im.getRGB(im.getWidth()/2, im.getHeight()/2);
		System.out.printf("type:%s %x (raster: %s)\n",imageTypeName(im),p,Arrays.toString(vals));
		
		//writeRgb8(im, new FileOutputStream(args[1]));
		System.out.println("Done: " + args[1]);
	}
}
