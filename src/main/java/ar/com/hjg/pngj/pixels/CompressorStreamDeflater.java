package ar.com.hjg.pngj.pixels;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

import ar.com.hjg.pngj.PngjOutputException;

/**
 * CompressorStream backed by a Deflater.
 * 
 * Note that the Deflater is not disposed after done, you should either recycle this with reset() or dispose it with
 * close()
 * 
 */
public class CompressorStreamDeflater extends CompressorStream {

	protected Deflater deflater;
	protected byte[] buf = new byte[4092]; // temporary storage of compressed bytes
	protected boolean discardWhenDone = false;

	public CompressorStreamDeflater(OutputStream os, int nblocks, int bytesPerBlock) {
		this(os, nblocks, bytesPerBlock, null);
	}

	/** if a deflater is passed, it must be already reset */
	public CompressorStreamDeflater(OutputStream os, int nblocks, int bytesPerBlock, Deflater def) {
		super(os, nblocks, bytesPerBlock);
		this.deflater = def == null ? new Deflater() : def;
	}

	public CompressorStreamDeflater(OutputStream os, int nblocks, int bytesPerBlock, int deflaterCompLevel,
			int deflaterStrategy) {
		this(os, nblocks, bytesPerBlock, new Deflater(deflaterCompLevel));
		deflater.setStrategy(deflaterStrategy);
	}

	// TODO: REMOVE THIS
	public void write(byte[] b, int off, int len) {
		if (len == 0)
			return;
		if (deflater.finished() || done || closed)
			throw new PngjOutputException("write beyond end of stream");
		int stride = buf.length;
		for (int i = 0; i < len; i += stride) {
			deflater.setInput(b, off + i, Math.min(stride, len - i));
			while (!deflater.needsInput())
				deflate();
		}
		bytesIn += len;
	}

	public void write(byte[] b, int off) {
		if (deflater.finished() || done || closed)
			throw new PngjOutputException("write beyond end of stream");
		if (storeFirstByte) {
			if (firstBytes == null)
				firstBytes = new byte[nblocks];
			firstBytes[block] = b[0];
		}
		int stride = buf.length;
		for (int i = 0; i < bytesPerBlock; i += stride) {
			deflater.setInput(b, off + i, Math.min(stride, bytesPerBlock - i));
			while (!deflater.needsInput())
				deflate();
		}
		bytesIn += bytesPerBlock;
		block++;
		if (block == nblocks) {
			done = true;
			finish();
		}
	}

	protected void deflate() {
		int len = deflater.deflate(buf, 0, buf.length);
		if (len > 0) {
			try {
				if (os != null)
					os.write(buf, 0, len);
			} catch (IOException e) {
				throw new PngjOutputException(e);
			}
			bytesOut += len;
		}
	}

	/** automatically called when done */
	protected void finish() {
		try {
			if (!deflater.finished()) {
				deflater.finish();
				while (!deflater.finished()) {
					deflate();
				}
				if (discardWhenDone)
					deflater.end();
			}
			if (os != null)
				os.flush();
		} catch (Exception e) {
			throw new PngjOutputException(e);
		}
	}

	public void close() {
		if (!closed) {
			super.close();
			finish();
			deflater.end();
		}
	}

	@Override
	public void reset() {
		if (closed)
			throw new PngjOutputException("cannot reset, discarded object");
		deflater.reset();
		bytesIn = 0;
		bytesOut = 0;
	}

	public void setDiscardWhenDone(boolean discardWhenDone) {
		this.discardWhenDone = discardWhenDone;
	}

}
