package ar.com.hjg.pngj.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.NullOs;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;

public class Sandbox {
	public static void reencodeWithJavaIo(File s, File d) throws IOException {
		BufferedImage img = ImageIO.read(s);
		ImageIO.write(img, "PNG", d);
		System.out.printf("%s -> %s \n", s.getName(), d.getName());
	}

	public static void reencodeWithFilter(File s, FilterType ftype) throws IOException {
		PngReader p = new PngReader(s);
		NullOs nos = new NullOs();
		PngWriter pw = new PngWriter(nos, p.imgInfo);
		pw.setFilterType(ftype);
		pw.setCompLevel(6);
		pw.writeRows(p.readRows());
		p.end();
		pw.end();
		System.out.println(s + " f=" + ftype + " comp:" + pw.getPixelsWriter().getCompression() + " "
				+ pw.getPixelsWriter().getFiltersUsed());
	}

	public static void main(String[] args) throws Exception {
		// 24 target (paeth) 0.62 
		reencodeWithFilter(TestSupport.absFile(new File("..\\..\\priv\\pngimages\\imset9\\9395.png")),
				FilterType.FILTER_ADAPTIVE_FULL);
	}
}
