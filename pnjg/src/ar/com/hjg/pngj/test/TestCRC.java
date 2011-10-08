package ar.com.hjg.pngj.test;

import java.util.zip.CRC32;

public class TestCRC {
	public static  void test() {
		CRC32 c = new CRC32();
		byte[] b = new byte[]{0,0,3,4,-124};
		c.update(b);
		int l1= (int)c.getValue();
		System.out.println(l1);
		c.reset();
		c.update(b);
		c.update(b);
		int l2= (int)c.getValue();
		System.out.println(l2);
		
	}

	public static void main(String[] args) {
		test();
	}
}
