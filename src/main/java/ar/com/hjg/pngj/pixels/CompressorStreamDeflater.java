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

	public CompressorStreamDeflater(OutputStream os, int maxBlockLen, long totalLen) {
		this(os, maxBlockLen, totalLen, null);
	}

	/** if a deflater is passed, it must be already reset. It will not be released on close */
	public CompressorStreamDeflater(OutputStream os, int maxBlockLen, long totalLen, Deflater def) {
		super(os, maxBlockLen, totalLen);
		this.deflater = def == null ? new Deflater() : def;
		this.deflaterIsOwn = def == null;
	}

	public CompressorStreamDeflater(OutputStream os, int maxBlockLen, long totalLen, int deflaterCompLevel,
			int deflaterStrategy) {
		this(os, maxBlockLen, totalLen, new Deflater(deflaterCompLevel));
		this.deflaterIsOwn = true;
		deflater.setStrategy(deflaterStrategy);
	}

	@Override
	public void mywrite(byte[] b, int off, final int len) {
		if (deflater.finished() || done || closed)
			throw new PngjOutputException("write beyond end of stream");
		deflater.setInput(b, off, len);
		bytesIn += len;
		while (!deflater.needsInput())
			deflate();
	}

	protected void deflate() {
		int len = deflater.deflate(buf, 0, buf.length);
		if (len > 0) {
			bytesOut += len;
			try {
				if (os != null)
					os.write(buf, 0, len);
			} catch (IOException e) {
				throw new PngjOutputException(e);
			}
		}
	}

	/** automatically called when done */
	@Override
	public void done() {
		if(done) return;
		if (!deflater.finished()) {
			deflater.finish();
			while (!deflater.finished()) 	deflate();
		}
		done=true;
		flush();
	}

	public void close() {
		done();
		try {
			if (deflaterIsOwn ) {
				deflater.end();
			}
		} catch (Exception e) {
		}
		super.close();
	}

	@Override
	public void reset() {
		super.reset();
		deflater.reset();
	}

}
