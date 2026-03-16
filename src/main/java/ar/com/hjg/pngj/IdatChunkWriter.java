package ar.com.hjg.pngj;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.zip.CRC32;

import ar.com.hjg.pngj.chunks.ChunkHelper;

/**
 * Outputs a sequence of IDAT-like chunks, filled progressively until the max
 * chunk length is reached (or until flush()).
 *
 * Uses a direct {@link ByteBuffer} internally so that:
 * <ul>
 *   <li>Deflater can write compressed output straight to off-heap memory via
 *       {@link #getWritableSlice()} + {@link java.util.zip.Deflater#deflate(ByteBuffer)}.</li>
 *   <li>Chunks are sent to the underlying stream via a {@link WritableByteChannel}.
 *       When the stream is a {@link java.io.FileOutputStream}, Java promotes the
 *       channel to a {@link java.nio.channels.FileChannel}, so the direct buffer
 *       goes to the OS in a single syscall without an intermediate heap copy.</li>
 * </ul>
 */
public class IdatChunkWriter {

	private static final int MAX_LEN_DEFAULT = 32768; // 32 K — data only

	private final OutputStream outputStream;
	/** For FileOutputStream this is a FileChannel; otherwise a generic wrapper. */
	private final WritableByteChannel channel;
	private final int maxChunkLen;

	/**
	 * Off-heap buffer that holds one in-progress IDAT chunk's payload.
	 * CompressorStreamDeflater deflates directly into this buffer via
	 * {@link #getWritableSlice()}.
	 */
	private ByteBuffer directBuf;

	private int offset = 0;
	private int availLen;
	private long totalBytesWriten = 0;
	private int chunksWriten = 0;

	// Pre-allocated framing buffers — only touched once per flush, not per row.
	private final ByteBuffer headerBuf = ByteBuffer.allocateDirect(8); // length(4) + type(4)
	private final ByteBuffer crcBuf = ByteBuffer.allocateDirect(4);
	private final CRC32 crc = new CRC32();

	public IdatChunkWriter(OutputStream outputStream) {
		this(outputStream, 0);
	}

	public IdatChunkWriter(OutputStream outputStream, int maxChunkLength) {
		this.outputStream = outputStream;
		this.channel = Channels.newChannel(outputStream);
		this.maxChunkLen = maxChunkLength > 0 ? maxChunkLength : MAX_LEN_DEFAULT;
		this.directBuf = ByteBuffer.allocateDirect(this.maxChunkLen);
		this.availLen = this.maxChunkLen;
		postReset();
	}

	/** Kept for source compatibility; {@code b} now only determines chunk size. */
	public IdatChunkWriter(OutputStream outputStream, byte[] b) {
		this(outputStream, b != null ? b.length : 0);
	}

	protected byte[] getChunkId() {
		return ChunkHelper.b_IDAT;
	}

	/**
	 * Writes a chunk if there is more than minLenToWrite bytes pending.
	 * Normally called internally, but can be called explicitly to force flush.
	 */
	public final void flush() {
		if (offset > 0 && offset >= minLenToWrite()) {
			writeChunkDirect(offset);
			totalBytesWriten += offset + 12;
			chunksWriten++;
			offset = 0;
			availLen = maxChunkLen;
			directBuf.clear();
			postReset();
		}
	}

	/** Writes a PNG chunk (length + type + data + CRC) via the NIO channel. */
	private void writeChunkDirect(int dataLen) {
		byte[] id = getChunkId();

		// CRC covers chunk type (4 bytes) + chunk data — matches PNG spec.
		crc.reset();
		crc.update(id);
		directBuf.position(0).limit(dataLen);
		crc.update(directBuf); // CRC32.update(ByteBuffer) — available since Java 8

		headerBuf.clear();
		headerBuf.putInt(dataLen);
		headerBuf.put(id);
		headerBuf.flip();

		crcBuf.clear();
		crcBuf.putInt((int) crc.getValue());
		crcBuf.flip();

		directBuf.position(0).limit(dataLen);

		try {
			channel.write(headerBuf); // length + type
			channel.write(directBuf); // compressed data (off-heap → OS)
			channel.write(crcBuf);    // CRC
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	public int getOffset() {
		return offset;
	}

	public int getAvailLen() {
		return availLen;
	}

	/**
	 * Returns the direct ByteBuffer positioned at the current write offset,
	 * limited to the available space. The caller writes into it (e.g. via
	 * {@link java.util.zip.Deflater#deflate(ByteBuffer)}) then calls
	 * {@link #incrementOffset(int)} with the number of bytes written.
	 */
	public ByteBuffer getWritableSlice() {
		return (ByteBuffer) directBuf.limit(offset + availLen).position(offset);
	}

	/** Triggers a flush+reset if the buffer becomes full. */
	public void incrementOffset(int n) {
		offset += n;
		availLen -= n;
		if (availLen < 0)
			throw new PngjOutputException("Anomalous situation");
		if (availLen == 0)
			flush();
	}

	/**
	 * Copies bytes into the direct buffer. The preferred path for
	 * CompressorStreamDeflater is {@link #getWritableSlice()} to avoid this copy;
	 * this method exists for other callers.
	 */
	public void write(byte[] b, int o, int len) {
		while (len > 0) {
			int n = len <= availLen ? len : availLen;
			directBuf.position(offset);
			directBuf.put(b, o, n);
			incrementOffset(n);
			len -= n;
			o += n;
		}
	}

	/** Called after reset; subclasses may override to add a prefix. */
	protected void postReset() {
	}

	protected int minLenToWrite() {
		return 1;
	}

	public void close() {
		flush();
		offset = 0;
		directBuf = null;
	}

	public long getTotalBytesWriten() {
		return totalBytesWriten;
	}

	public int getChunksWriten() {
		return chunksWriten;
	}
}
