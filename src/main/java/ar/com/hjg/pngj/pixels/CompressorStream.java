package ar.com.hjg.pngj.pixels;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import ar.com.hjg.pngj.PngjOutputException;

/**
 * This is an OutputStream that compresses (via Deflater or a deflater-like object), and optionally passes the
 * compressed stream to another output stream.
 * 
 * It allows to compute in/out/ratio stats.
 * 
 * It works as a stream (similar to DeflaterOutputStream), but it's peculiar in that it expects that each writes has a
 * fixed length (other lenghts are accepted, but it's less efficient) and that the total amount of bytes is known (so it
 * can close itself, but it can also be closed on demand) In out app, the block is a row (including filter byte).
 * 
 * We use this to do the real compression (with Deflate) but also to compute tentative estimators
 * 
 * If not closed, it can be recicled via reset()
 * 
 * It has 3 states: - working: 0 or more bytes received - done: targetLen bytes received and proceseed, only here
 * getCompressionRatio() can be called - closed: object discarded
 * 
 */
public abstract class CompressorStream extends FilterOutputStream {

	protected OutputStream os; // can be null!
	public final int blockLen;
	public final long totalbytes;

	boolean closed = false;
	boolean done = false;
	protected long bytesIn = 0;
	protected long bytesOut = 0;
	protected int block = -1;

	/** stores the first byte of each row */
	private byte[] firstBytes;
	protected boolean storeFirstByte = false;

	public CompressorStream(OutputStream os, int blockLen, long totalbytes) {
		super(os);
		if (blockLen < 1 || totalbytes < 1)
			throw new RuntimeException(" maxBlockLen or totalLen invalid");
		this.os = os;
		this.blockLen = blockLen;
		this.totalbytes = totalbytes;
	}

	/** Releases resources. Does NOT close the OuputStream. Idempotent. Should be called when done */
	@Override
	public void close() {
		flush();
		closed = true;
	}

	public final void write(byte[] b, int off, int len) {
		int stride = len > blockLen ? blockLen : len;
		while (len > 0) {
			block++;
			mywrite(b, off, stride);
			if (storeFirstByte && block < firstBytes.length) {
				firstBytes[block] = b[off];
			}
			off += stride;
			len -= stride;
		}
	}

	/** same as write, but guarantedd to not exceed blockLen */
	protected abstract void mywrite(byte[] b, int off, int len);

	@Override
	public final void write(byte[] b) {
		write(b, 0, b.length);
	}

	@Override
	public void write(int b) throws IOException {
		throw new PngjOutputException("should not be used");
	}

	public void reset() {
		if (closed)
			throw new PngjOutputException("cannot reset, discarded object");
		bytesIn = 0;
		bytesOut = 0;
		done = false;
	}

	/**
	 * compressed/raw. This should be called only when done
	 */
	public final double getCompressionRatio() {
		return bytesOut == 0 ? 1.0 : bytesOut / (double) bytesIn;
	}

	/**
	 * raw (input) bytes. This should be called only when done
	 */
	public final long getBytesRaw() {
		return bytesIn;
	}

	/**
	 * compressed (out) bytes. This should be called only when done
	 */
	public final long getBytesCompressed() {
		return bytesOut;
	}

	/**
	 * @return the output stream : warning, it can be null
	 */
	public OutputStream getOs() {
		return os;
	}

	@Override
	public void flush() {
		if (os != null)
			try {
				os.flush();
			} catch (IOException e) {
				throw new PngjOutputException(e);
			}
	}

	public boolean isClosed() {
		return closed;
	}

	public boolean isDone() {
		return done;
	}

	public byte[] getFirstBytes() {
		return firstBytes;
	}

	public void setStoreFirstByte(boolean storeFirstByte, int nblocks) {
		this.storeFirstByte = storeFirstByte;
		if (this.storeFirstByte) {
			if (firstBytes == null || firstBytes.length < nblocks)
				firstBytes = new byte[nblocks];
		} else
			firstBytes = null;
	}

}
