package ar.com.hjg.pngj.samples;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;

public class SampleCreateOrangeGradient {

	private static void create(OutputStream outputStream, int cols, int rows) {
		ImageInfo imi = new ImageInfo(cols, rows, 8, false); // 8 bits per channel, no alpha
		// open image for writing to a output stream
		PngWriter png = new PngWriter(outputStream, imi);
		// add some optional metadata (chunks)
		png.getMetadata().setDpi(100.0);
		png.getMetadata().setTimeNow(0); // 0 seconds fron now = now
		png.getMetadata().setText(PngChunkTextVar.KEY_Title, "just a text image");
		png.getMetadata().setText("my key", "my text");
		ImageLineInt iline = new ImageLineInt(imi);
		for (int col = 0; col < imi.cols; col++) { // this line will be written to all rows
			int r = 255;
			int g = 127;
			int b = 255 * col / imi.cols;
			ImageLineHelper.setPixelRGB8(iline, col, r, g, b); // orange-ish gradient
		}
		for (int row = 0; row < png.imgInfo.rows; row++) {
			png.writeRow(iline);
		}
		png.end();
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.err.println("Arguments: [pngdest] [cols] [rows]");
			System.exit(1);
		}
		int cols = Integer.parseInt(args[1]);
		int rows = Integer.parseInt(args[2]);
		File file = new File(args[0]);
		if (file.exists())
			throw new Exception("Cowardly refusing to overwrite " + file);
		create(new FileOutputStream(file), cols, rows);
		System.out.println(file + " created");
	}

}
