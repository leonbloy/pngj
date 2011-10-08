package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class TestDeflate {
	public static void test(String filename) throws IOException {
		FileOutputStream fs = new FileOutputStream(new File(filename), false);
		DeflaterOutputStream fs2 = new DeflaterOutputStream(fs, new Deflater(3));
		for (int i = 0; i < 50; i++)
			for (int j = 0; j < 40; j++)
				fs2.write((byte) (i + 0x30));
		fs2.close();
	}

	public static void testread(String filename) throws IOException {
		FileInputStream fs = new FileInputStream(new File(filename));
		InflaterInputStream fs2 = new InflaterInputStream(fs);
		int c, n = 0;
		while ((c = fs2.read()) >= 0) {
			System.out.print((char) c);
			if (n++ % 40 == 0)				System.out.println("");		
		}
		fs2.close();
	}

	public static void main(String[] args) throws IOException {
		//test("C:\\temp\\testdeflate2.bin");
		testread("C:\\temp\\testdeflate.bin");
	}
}
