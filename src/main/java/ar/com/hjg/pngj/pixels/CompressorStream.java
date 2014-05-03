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
 * It works as a stream (similar to DeflaterOutputStream), but it's peculiar in that it knows that it will be
 * written in blocks of know maximum length, and the total amount of bytes is known. In out app, the block is 
 * a row (including filter byte). We use this to do the real compression (with Deflate) but also to 
 * compute tentative estimators
 * 
 * If not closed, it can be recicled via reset()
 * 
 * It has 3 states: - working: 0 or more bytes received - done: targetLen bytes received and proceseed, only here
 * getCompressionRatio() can be called - closed: object discarded
 * 
 */
public abstract class CompressorStream extends FilterOutputStream {

	protected OutputStream os; // can be null!
	public final int maxBlockLen,maxBlocks;
	public final long totalLen;
	

	boolean closed = false;
	boolean done = false;
	protected long bytesIn = 0;
	protected long bytesOut = 0;
	protected int block=0;
	
	/** stores the first byte of each row */
	protected byte[] firstBytes;
	protected boolean storeFirstByte = false;

	public CompressorStream(OutputStream os, int maxBlockLen, int maxBlocks,long totalLen) {
		super(os);
		if (maxBlockLen < 1 || totalLen < 1)
			throw new RuntimeException(" maxBlockLen or totalLen invalid");
		this.os = os;
		this.maxBlockLen = maxBlockLen;
		this.maxBlocks = maxBlocks;
		this.totalLen = totalLen;
	}

	/** Releases resources. Does NOT close the OuputStream. Idempotent. Should be called when done */
	public void close() {
		closed = true;
	}

	public abstract void write(byte[] b, int off,int len);

	/**
	 * length is assumed: bytesPerBlock
	 */
	@Override
	public final void write(byte[] b) {
		write(b, 0,b.length);
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
		if (!done)
			throw new PngjOutputException("not done");
		return bytesOut == 0 ? 1.0 : bytesOut / (double) bytesIn;
	}

	/**
	 * raw (input) bytes. This should be called only when done
	 */
	public final long getBytesRaw() {
		if (!done)
			throw new PngjOutputException("not done");
		return bytesIn;
	}

	/**
	 * compressed (out) bytes. This should be called only when done
	 */
	public final long getBytesCompressed() {
		if (!done)
			throw new PngjOutputException("not done");
		return bytesOut;
	}

	/**
	 * @return the output stream : warning, it can be null
	 */
	public OutputStream getOs() {
		return os;
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

	public void setStoreFirstByte(boolean storeFirstByte) {
		this.storeFirstByte = storeFirstByte;
		if (this.storeFirstByte) {
			if (firstBytes == null || firstBytes.length < maxBlocks)
				firstBytes = new byte[maxBlocks];

		} else
			firstBytes = null;
	}

}
