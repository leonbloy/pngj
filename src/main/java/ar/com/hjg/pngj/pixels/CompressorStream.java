package ar.com.hjg.pngj.pixels;

import java.io.OutputStream;

import ar.com.hjg.pngj.PngjOutputException;

/**
 * This is an OutputStream that compresses (via Deflater or a deflater-like object), and optionally passes the
 * compressed stream to another output stream.
 * 
 * It allows to compute in/out/ratio stats
 * 
 * It works as a stream (similar to DeflaterOutputStream), but it's peculiar in that it knows in advance that it will be
 * written with a (known) number of blocks of fixed (known) length. In out app, the block is a row (including filter
 * byte)
 * 
 * If not closed, it can be recicled via reset()
 * 
 * It has 3 states: - working: 0 or more bytes received - done: targetLen bytes received and proceseed, only here
 * getCompressionRatio() can be called - closed: object discarded
 * 
 */
public abstract class CompressorStream {

	protected OutputStream os;
	protected final int nblocks, bytesPerBlock;

	boolean closed = false;
	boolean done = false;
	protected long bytesIn = 0;
	protected long bytesOut = 0;
	protected int block = 0; // current block (row)
	/** stores the first byte of each row */
	protected byte[] firstBytes;
	protected boolean storeFirstByte = false;

	public CompressorStream(OutputStream os, int nblocks, int bytesPerBlock) {
		super();
		this.os = os;
		this.nblocks = nblocks;
		this.bytesPerBlock = bytesPerBlock;
	}

	/** Releases resources. Does NOT close the OuputStream. Idempotent. Should be called when done */
	public void close() {
		closed = true;
	}

	/**
	 * length is assumed: bytesPerBlock
	 */
	public abstract void write(byte[] b, int off);

	/**
	 * length is assumed: bytesPerBlock
	 */
	public final void write(byte[] b) {
		write(b, 0);
	}

	public void reset() {
		if (closed)
			throw new PngjOutputException("cannot reset, discarded object");
		bytesIn = 0;
		bytesOut = 0;
		block = 0;
		done=false;
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

	public int getNblocks() {
		return nblocks;
	}

	public int getBytesPerBlock() {
		return bytesPerBlock;
	}

	public void setStoreFirstByte(boolean storeFirstByte) {
		this.storeFirstByte = storeFirstByte;
		if (this.storeFirstByte) {
			if (firstBytes == null || firstBytes.length < nblocks)
				firstBytes = new byte[nblocks];

		} else
			firstBytes = null;
	}

}
