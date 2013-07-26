package ar.com.hjg.pngj.samples;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.imageline.ImageLineSetARGBbi;
import ar.com.hjg.pngj.test.TestSupport;

/**
 */
public class CreateFromBufferedImageTest {

	public static BufferedImage readBi(File filename, int type) {
		BufferedImage in;
		try {
			in = ImageIO.read(filename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (type >= 0 && in.getType() != type) {
			BufferedImage newImage = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g = newImage.createGraphics();
			g.drawImage(in, 0, 0, null);
			g.dispose();
			return newImage;
		} else
			return in;
	}

	@Test
	public void test1() throws IOException {
		File fori = new File(TestSupport.getResourcesDir(), "testsuite1/tp0n2c08.png");
		BufferedImage in = readBi(fori, BufferedImage.TYPE_4BYTE_ABGR);
		TestCase.assertEquals(BufferedImage.TYPE_4BYTE_ABGR, in.getType());
		ImageInfo iminfo = new ImageInfo(in.getWidth(), in.getHeight(), 8, true);
		File dest = new File(TestSupport.getTempDir(), "tp0n2c08__.png");
		PngWriter pngw = new PngWriter(dest, iminfo);
		ImageLineSetARGBbi lines = new ImageLineSetARGBbi(in, iminfo);
		pngw.writeRows(lines);
		pngw.end();
		System.out.println(fori + " -> " + dest); // Todo better check
	}

}
