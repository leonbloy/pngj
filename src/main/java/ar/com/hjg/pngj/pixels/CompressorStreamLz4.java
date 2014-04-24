package ar.com.hjg.pngj.pixels;

import java.io.OutputStream;
import java.util.zip.Deflater;

import ar.com.hjg.pngj.PngjOutputException;

/**
 * This class uses a quick compressor to get a rough estimate of deflate compression ratio.
 * 
 * This just ignores the outputStream, and the deflater related parameters
 */
public class CompressorStreamLz4 extends CompressorStream {

	private final DeflaterEstimatorLz4 lz4;

	// 3 cases:
	// large (bytesPerBlock>= MAX_BUFFER_SIZE), or 1 row, unbuffered (one shot)
	// small (totalBytes <= MAX_BUFFER_SIZE), all buffered
	// medium : buffered, buffer has a multiple of bytesPerBlock
	private boolean buffered;
	private byte[] buf; // lazily allocated, only if needed
	// bufpos=bytes in buffer yet not compressed; bytesIn does not include this!
	private int bufpos = 0;

	// actually in some cases it can be up to almost twice this
	private static final int MAX_BUFFER_SIZE = 4096;
	//
	private final int buffer_size;

	public CompressorStreamLz4(OutputStream os, int nblocks, int bytesPerBlock) {
		super(os, nblocks, bytesPerBlock);
		lz4 = new DeflaterEstimatorLz4();
		long totalLen = nblocks * (long) bytesPerBlock;
		int blen = 0;

		if (bytesPerBlock >= MAX_BUFFER_SIZE || nblocks == 1) {
			buffered = false;// not used, one shot
		} else if (totalLen <= MAX_BUFFER_SIZE) {
			blen = (int) totalLen;
			buffered = true;
		} else {
			blen = bytesPerBlock * (MAX_BUFFER_SIZE / bytesPerBlock);
			if (blen == bytesPerBlock)
				blen *= 2;
			buffered = true;
		}
		buffer_size = blen;
		if (buffered)
			buf = new byte[buffer_size];
	}

	public CompressorStreamLz4(OutputStream os, int nblocks, int bytesPerBlock, Deflater def) {
		this(os, nblocks, bytesPerBlock);// edlfater ignored
	}

	public CompressorStreamLz4(OutputStream os, int nblocks, int bytesPerBlock, int deflaterCompLevel,
			int deflaterStrategy) {
		this(os, nblocks, bytesPerBlock); // paramters ignored
	}

	public void write(byte[] b, int off) {
		if (done || closed)
			throw new PngjOutputException("write beyond end of stream");
		if (storeFirstByte) {
			if (firstBytes == null)
				firstBytes = new byte[nblocks];
			firstBytes[block] = b[0];
		}
		block++;
		if (!buffered) {
			bytesOut += lz4.compressEstim(b, off, bytesPerBlock);
			bytesIn += bytesPerBlock;
		} else { // buffer had content
			System.arraycopy(b, off, buf, bufpos, bytesPerBlock);
			bufpos += bytesPerBlock;
			if (bufpos == buffer_size || block == nblocks) {
				bytesOut += lz4.compressEstim(buf, 0, bufpos);
				bytesIn += bufpos;
				bufpos = 0;
			}
		}
		if (block == nblocks) {
			done = true;
			if (bufpos != 0)
				throw new PngjOutputException("??");
		}
	}

	@Override
	public void close() {
		if (!closed) {
			super.close();
			buf = null;
		}
	}

	@Override
	public void reset() {
		if (closed)
			throw new PngjOutputException("Cannot recycle discarded object");
		bufpos = 0;
		bytesIn = 0;
		bytesOut = 0;
		done = false;
	}

}
