package ar.com.hjg.pngj.test;


/**
 * reencodes a png image with a given filter and compression level
 */
public class TestLossy {


public static void main(String[] args) {
	byte b1,b2;
	for(int x=-666000;x<666600;x++ ){
		b1 = (byte)(x>>8);
		b2=(byte)((x & 0xFF00) >> 8);
		if(b1!=b2) throw new RuntimeException("!" + x);
	}
	System.out.println("ok!");
}


	
}
