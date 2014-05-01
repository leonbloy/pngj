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
	protected boolean deflaterIsOwn = true;

	public CompressorStreamDeflater(OutputStream os, int nblocks, int bytesPerBlock) {
		this(os, nblocks, bytesPerBlock, null);
	}

	/** if a deflater is passed, it must be already reset. It will not be released on close */
	public CompressorStreamDeflater(OutputStream os, int nblocks, int bytesPerBlock, Deflater def) {
		super(os, nblocks, bytesPerBlock);
		this.deflater = def == null ? new Deflater() : def;
		this.deflaterIsOwn = def == null;
	}

	public CompressorStreamDeflater(OutputStream os, int nblocks, int bytesPerBlock, int deflaterCompLevel,
			int deflaterStrategy) {
		this(os, nblocks, bytesPerBlock, new Deflater(deflaterCompLevel));
		this.deflaterIsOwn = true;
		deflater.setStrategy(deflaterStrategy);
	}

	public void write(byte[] b, int off) {
		if (deflater.finished() || done || closed)
			throw new PngjOutputException("write beyond end of stream");
		if (storeFirstByte) {
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
		if (!deflater.finished()) {
			deflater.finish();
			while (!deflater.finished()) {
				deflate();
			}
		}
		if (os != null) {
			try {
				os.flush();
			} catch (Exception ee) {
			}
		}
	}

	public void close() {
		super.close();
		try {
			if (deflaterIsOwn && deflater != null) {
				deflater.end();
				deflater = null;
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void reset() {
		super.reset();
		deflater.reset();
	}

}
