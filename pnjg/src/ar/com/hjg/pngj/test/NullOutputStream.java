package ar.com.hjg.pngj.test;

import java.io.IOException;
import java.io.OutputStream;

/** trivial outputstream that writes to nowhere (like /dev/null ) */
class NullOutputStream extends OutputStream {

	@Override
	public void write(int arg0) throws IOException {
		// nothing!
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
	}

	@Override
	public void write(byte[] b) throws IOException {
	}
}
