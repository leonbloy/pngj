package ar.com.hjg.pngj.misc;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Trivial output stream that discards written bytes. It keeps a count of sent
 * bytes.
 */
public class NullOs extends OutputStream {
	private long cont = 0;

	@Override
	public void write(int arg0) throws IOException {
		cont++;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		cont += len;
	}

	@Override
	public void write(byte[] b) throws IOException {
		cont += b.length;
	}

	public long getBytes() {
		return cont;
	}

}
