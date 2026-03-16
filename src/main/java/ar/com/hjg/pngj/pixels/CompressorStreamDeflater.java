package ar.com.hjg.pngj.pixels;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;

import ar.com.hjg.pngj.IdatChunkWriter;
import ar.com.hjg.pngj.PngjOutputException;

/**
 * CompressorStream backed by a Deflater.
 *
 * Note that the Deflater is not disposed after done, you should either recycle
 * this with reset() or dispose it with close()
 *
 */
public class CompressorStreamDeflater extends CompressorStream {

	protected Deflater deflater;
	protected boolean deflaterIsOwn = true;

	/**
	 * Pre-allocated direct ByteBuffer for Deflater input (Java 11+).
	 * Direct memory avoids JNI array pinning when feeding row data to native ZLIB.
	 */
	private final ByteBuffer directIn;

	/**
	 * Pre-allocated direct ByteBuffer used in estimation mode (idatChunkWriter ==
	 * null). Avoids on-demand byte[] allocation; output bytes are discarded.
	 */
	private final ByteBuffer directOut;

	/**
	 * if a deflater is passed, it must be already reset. It will not be
	 * released on close
	 */
	public CompressorStreamDeflater(IdatChunkWriter idatCw, int maxBlockLen, long totalLen, Deflater def) {
		super(idatCw, maxBlockLen, totalLen);
		this.deflater = def == null ? new Deflater() : def;
		this.deflaterIsOwn = def == null;
		this.directIn = ByteBuffer.allocateDirect(this.blockLen);
		this.directOut = ByteBuffer.allocateDirect(this.blockLen);
	}

	public CompressorStreamDeflater(IdatChunkWriter idatCw, int maxBlockLen, long totalLen) {
		this(idatCw, maxBlockLen, totalLen, null);
	}

	public CompressorStreamDeflater(IdatChunkWriter idatCw, int maxBlockLen, long totalLen, int deflaterCompLevel,
			int deflaterStrategy) {
		this(idatCw, maxBlockLen, totalLen, new Deflater(deflaterCompLevel));
		this.deflaterIsOwn = true;
		deflater.setStrategy(deflaterStrategy);
	}

	@Override
	public void mywrite(byte[] data, int off, final int len) {
		if (deflater.finished() || done || closed)
			throw new PngjOutputException("write beyond end of stream");
		// Copy row into direct buffer so native ZLIB gets a stable off-heap pointer
		// without JNI array pinning.
		directIn.clear();
		directIn.put(data, off, len);
		directIn.flip();
		deflater.setInput(directIn);
		bytesIn += len;
		while (!deflater.needsInput())
			deflate();
	}

	protected void deflate() {
		int len;
		if (idatChunkWriter != null) {
			// Deflate directly into the IdatChunkWriter's off-heap buffer.
			// No intermediate byte[] copy; for FileOutputStream-backed writers the
			// channel.write() in flush() is also copy-free (direct → FileChannel).
			ByteBuffer slice = idatChunkWriter.getWritableSlice();
			len = deflater.deflate(slice);
			if (len > 0) {
				idatChunkWriter.incrementOffset(len);
				bytesOut += len;
			}
		} else {
			// Estimation mode: compress into directOut to drive bytesOut, discard output.
			directOut.clear();
			len = deflater.deflate(directOut);
			bytesOut += len;
		}
	}

	/** automatically called when done */
	@Override
	public void done() {
		if (done)
			return;
		if (!deflater.finished()) {
			deflater.finish();
			while (!deflater.finished())
				deflate();
		}
		done = true;
		if (idatChunkWriter != null)
			idatChunkWriter.close();
	}

	public void close() {
		done();
		try {
			if (deflaterIsOwn) {
				deflater.end();
			}
		} catch (Exception e) {
		}
		super.close();
	}

	@Override
	public void reset() {
		deflater.reset();
		super.reset();
	}

}
