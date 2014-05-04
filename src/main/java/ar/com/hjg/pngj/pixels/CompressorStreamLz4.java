package ar.com.hjg.pngj.pixels;

import java.io.IOException;
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

	public CompressorStreamLz4(OutputStream os, int maxBlockLen, long totalLen) {
		super(os, maxBlockLen, totalLen);
		lz4 = new DeflaterEstimatorLz4();
		int blen = 0;

		if (maxBlockLen >= MAX_BUFFER_SIZE || maxBlockLen == totalLen) {
			buffered = false;// not used, one shot
		} else if (totalLen <= MAX_BUFFER_SIZE) {
			blen = (int) totalLen;
			buffered = true;
		} else {
			blen = maxBlockLen * (MAX_BUFFER_SIZE / maxBlockLen);
			if (blen == maxBlockLen)
				blen *= 2;
			buffered = true;
		}
		buffer_size = blen;
		if (buffered)
			buf = new byte[buffer_size];
	}

	public CompressorStreamLz4(OutputStream os, int maxBlockLen,  long totalLen, Deflater def) {
		this(os, maxBlockLen, totalLen);// edlfater ignored
	}

	public CompressorStreamLz4(OutputStream os, int maxBlockLen,  long totalLen, int deflaterCompLevel,
			int deflaterStrategy) {
		this(os, maxBlockLen, totalLen); // paramters ignored
	}

	@Override
	public void mywrite(byte[] b, int off, int len) {
		if (done || closed)
			throw new PngjOutputException("write beyond end of stream");
		bytesIn += len;
		if (!buffered) {
			bytesOut += lz4.compressEstim(b, off, len);
		} else { // buffer had content
			if (bufpos + len > buffer_size) {
				bytesOut += lz4.compressEstim(buf, 0, bufpos);
				bufpos = 0;
			}
			System.arraycopy(b, off, buf, bufpos, len);
			bufpos += len;
			if (bufpos == buffer_size || bytesIn == totalbytes) {
				bytesOut += lz4.compressEstim(buf, 0, bufpos);
				bufpos = 0;
			}
		}
		if (bytesIn == totalbytes) {
			done = true;
			if (bufpos != 0)// assert
				throw new PngjOutputException("??");
		}
	}

	@Override
	public void flush()  {
		if (bufpos >0) {
			bytesOut += lz4.compressEstim(buf, 0, bufpos);
			bufpos = 0;
		}
		super.flush();
	}

	@Override
	public void close() {
		flush();
		if (!closed) {
			super.close();
			buf = null;
		}
	}

	@Override
	public void reset() {
		super.reset();
		bufpos = 0;
	}

}
