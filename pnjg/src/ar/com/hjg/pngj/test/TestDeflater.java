package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class TestDeflater {
	public static void main(String[] args) throws Exception {
		int complevel = 6;
		int t = test2("C:/temp/pragad.bmp", complevel, Deflater.FILTERED);
		System.out.println(t + " msecs");
	}

	public static int test(String filename, int complevel, int deflaterStrat) throws Exception {
		boolean varsize = false;
		int buflen = 96;
		int buflen2 = buflen / 2;
		byte[] buf1 = new byte[buflen];
		byte[] buf2 = new byte[buflen];
		byte[] buf3 = new byte[buflen];
		for (int i = 0; i < buflen; i++) {
			buf1[i] = i == buflen - 1 ? (byte) 0x0a : (byte) (i + 32);
			buf2[i] = i == buflen - 1 ? (byte) 0x0a : (byte) (((i * 7) % 96) + 32);
			buf3[i] = i == buflen - 1 ? (byte) 0x0a : (byte) (((i * 31) % 96) + 32);
		}

		Deflater defl = new Deflater(complevel);
		defl.setStrategy(deflaterStrat);
		FileOutputStream pos = new FileOutputStream(filename);
		DeflaterOutputStream dos = new DeflaterOutputStream(pos, defl);
		OutputStream os = dos;
		int times = 500000;
		long t0 = System.currentTimeMillis();
		for (int t = 0; t < times; t++) {
			os.write(buf1, 0, varsize ? (t % buflen2) + buflen2 : buflen * 2 / 3);
			os.write(buf2, 0, varsize ? ((t + buflen2) % buflen2) + buflen2 : buflen * 2 / 3);
			os.write(buf3, 0, varsize ? ((t + buflen) % buflen2) + buflen2 : buflen * 2 / 3);
		}
		long t1 = System.currentTimeMillis();
		os.close();
		return (int) (t1 - t0);
	}
	
	public static int test2(String filename, int complevel, int deflaterStrat) throws Exception {
		FileInputStream fin = new FileInputStream(new File(filename));
		Deflater defl = new Deflater(complevel);
		defl.setStrategy(deflaterStrat);
		NullOutputStream pos = new NullOutputStream();
		DeflaterOutputStream dos = new DeflaterOutputStream(pos, defl);
		OutputStream os = dos;
		
		byte[] buf = new byte[3000];
		long t0 = System.currentTimeMillis();
		int c = 0;
        int total = 0;
         for (int k = 0; k < 4; k++)
         {
             while ((c = fin.read(buf, 0, 3000)) > 0)
             {
                 os.write(buf, 0, c);
                 total += c;
             }
     		fin.close();
     		fin = new FileInputStream(new File(filename));
         }
		long t1 = System.currentTimeMillis();
		os.close();
		fin.close();
		System.out.println(String.format("%.2f%%  %d msecs", (pos.getCont()* 100.0) / total, t1 - t0));
          
		return (int) (t1 - t0);
	}
}
