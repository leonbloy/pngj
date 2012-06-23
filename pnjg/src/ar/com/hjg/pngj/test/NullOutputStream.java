package ar.com.hjg.pngj.test;

import java.io.IOException;
import java.io.OutputStream;

/** trivial outputstream that writes to nowhere (like /dev/null ) */
class NullOutputStream extends OutputStream {
	private int cont=0;
	@Override
	public void write(int arg0) throws IOException {
		// nothing!
		cont++;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		cont+=len;
	}

	@Override
	public void write(byte[] b) throws IOException {
		cont+=b.length;
	}

	public int getCont() {
		return cont;
	}
	
}
