package ar.com.hjg.png.imageline;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import ar.com.hjg.pngj.IImageLineSet;
import ar.com.hjg.pngj.ImageInfo;

public class ImageLineSetARGBbi implements IImageLineSet<ImageLineARGBbi> {

	BufferedImage image;
	private ImageInfo imginfo;
	private ImageLineARGBbi line; // one single line, operates like a cursor

	// TODO: check that bi is ARGB ? auto create imginfo? check consistency?
	public ImageLineSetARGBbi(BufferedImage bi, ImageInfo imginfo) {
		this.image = bi;
		this.imginfo = imginfo;
		line = new ImageLineARGBbi(imginfo, bi, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
	}

	public ImageLineARGBbi getImageLine(int n) {
		line.setRowNumber(n);
		return line;
	}

	public boolean hasImageLine(int n) {
		return n >= 0 && n < imginfo.rows;
	}

	public int size() {
		return 1; // 
	}

}
