package ar.com.hjg.pngj;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;

import ar.com.hjg.pngj.ChunkReader.ChunkReaderMode;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes a stream of bytes that consist of a series of PNG-like chunks.
 * <p>
 * This has little intelligence, it's quite low-level and general (it could even
 * be used for a MNG stream, for example). It supports signature recognition and
 * idat deflate
 */
public abstract class ChunkSeqReader implements IBytesConsumer, Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChunkSeqReader.class.getName());

	private final byte[] expectedSignature;
	private final int signatureLength;

	private byte[] buf0 = new byte[8]; // for signature or chunk starts
	private int buf0len = 0;

	protected boolean signatureDone = false;
	protected boolean endChunkDone = false;
	protected boolean closed = false; // ended, normally or not

	private int chunkCount = 0;

	private long bytesCount = 0;

	private DeflatedChunksSet curDeflatedSet; // one instance for each "idat-like set". Normally one.

	private ChunkReader curChunkReader;

	private long idatBytes; // this is only for the IDAT (not mrerely "idat-like")

	private ErrorBehaviour errorBehaviour = ErrorBehaviour.STRICT;

	/**
	 * Creates a ChunkSeqReader (with signature)
	 */
	public ChunkSeqReader() {
		this(PngHelperInternal.getPngIdSignature());
	}

	/**
	 * Pass _expectedSignature = null (or empty) for no signature
	 */
	public ChunkSeqReader(byte[] _expectedSignature) {
		this.expectedSignature = _expectedSignature;
		signatureLength = expectedSignature == null ? 0 : expectedSignature.length;
		signatureDone = signatureLength <= 0;
	}

	/**
	 * Consumes (in general, partially) a number of bytes. A single call never
	 * involves more than one chunk.
	 * 
	 * When the signature is read, it calls checkSignature()
	 * 
	 * When the start of a chunk is detected, it calls
	 * {@link #startNewChunk(int, String, long)}
	 * 
	 * When data from a chunk is being read, it delegates to
	 * {@link ChunkReader#consume(byte[], int, int)}
	 * 
	 * The caller might want to call this method more than once in succesion
	 * 
	 * This should rarely be overriden
	 * 
	 * @param buffer
	 * @param offset
	 *            Offset in buffer
	 * @param len
	 *            Valid bytes that can be consumed
	 * @return processed bytes, in the 1-len range. -1 if done. Only returns 0
	 *         if len=0.
	 **/
	@Override
	public int consume(byte[] buffer, int offset, int len) {
		if (closed)
			return -1;
		if (len == 0)
			return 0; // nothing to do (should not happen)
		if (len < 0)
			throw new PngjInputException("This should not happen. Bad length: " + len);
		int processed = 0;
		if (signatureDone) {
			if (curChunkReader == null || curChunkReader.isDone()) { // new chunk: read first 8 bytes
				int read0 = 8 - buf0len;
				if (read0 > len)
					read0 = len;
				System.arraycopy(buffer, offset, buf0, buf0len, read0);
				buf0len += read0;
				processed += read0;
				bytesCount += read0;
				// len -= read0;
				// offset += read0;
				if (buf0len == 8) { // end reading chunk length and id
					chunkCount++;
					int clen = PngHelperInternal.readInt4fromBytes(buf0, 0);
					String cid = ChunkHelper.idFromBytes(buf0, 4);
					startNewChunk(clen, cid, bytesCount - 8);
					buf0len = 0;
				}
			} else { // reading chunk, delegates to curChunkReader
				int read1 = curChunkReader.consume(buffer, offset, len);
				if (read1 < 0)
					return -1; // should not happen
				processed += read1;
				bytesCount += read1;
			}
		} else { // reading signature
			int read = signatureLength - buf0len;
			if (read > len)
				read = len;
			System.arraycopy(buffer, offset, buf0, buf0len, read);
			buf0len += read;
			if (buf0len == signatureLength) {
				checkSignature(buf0);
				buf0len = 0;
				signatureDone = true;
			}
			processed += read;
			bytesCount += read;
		}
		return processed;
	}

	/**
	 * Trys to feeds exactly <code>len</code> bytes, calling
	 * {@link #consume(byte[], int, int)} retrying if necessary.
	 * 
	 * This should only be used in callback mode
	 * 
	 * @return Excess bytes (not feed). Normally should return 0
	 */
	public int feedAll(byte[] buf, int off, int len) {
		while (len > 0) {
			int n = consume(buf, off, len);
			if (n < 1)
				return len;
			len -= n;
			off += n;
		}
		assert len == 0;
		return 0;
	}

	/**
	 * Called for all chunks when a chunk start has been read (id and length),
	 * before the chunk data itself is read. It creates a new ChunkReader (field
	 * accesible via {@link #getCurChunkReader()}) in the corresponding mode,
	 * and eventually a curReaderDeflatedSet.(field accesible via
	 * {@link #getCurDeflatedSet()})
	 * 
	 * To decide the mode and options, it calls
	 * {@link #shouldCheckCrc(int, String)},
	 * {@link #shouldSkipContent(int, String)}, {@link #isIdatKind(String)}.
	 * Those methods should be overriden in preference to this; if overriden,
	 * this should be called first.
	 * 
	 * The respective {@link ChunkReader#chunkDone()} method is directed to this
	 * {@link #postProcessChunk(ChunkReader)}.
	 * 
	 * Instead of overriding this, see also
	 * {@link #createChunkReaderForNewChunk(String, int, long, boolean)}
	 */
	protected void startNewChunk(int len, String id, long offset) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("New chunk: " + id + " " + len + " off:" + offset);
		// check id an length
		if (id.length() != 4 || !ChunkHelper.CHUNK_ID_PAT.matcher(id).matches())
			throw new PngjInputException("Bad chunk id: " + id);
		if (len < 0)
			throw new PngjInputException("Bad chunk len: " + len);
		if (id.equals(ChunkHelper.IDAT))
			idatBytes += len;
		boolean checkCrc = shouldCheckCrc(len, id);
		boolean skip = shouldSkipContent(len, id);
		boolean isIdatType = isIdatKind(id);
		// PngHelperInternal.debug("start new chunk id=" + id + " off=" + offset + "
		// skip=" + skip + " idat=" +
		// isIdatType);
		// first see if we should terminate an active curReaderDeflatedSet
		boolean forCurrentIdatSet = false;
		if (curDeflatedSet != null && !curDeflatedSet.isClosed())
			forCurrentIdatSet = curDeflatedSet.ackNextChunkId(id);
		if (isIdatType && !skip) { // IDAT non skipped: create a DeflatedChunkReader owned by a idatSet
			if (!forCurrentIdatSet) {
				if (curDeflatedSet != null && !curDeflatedSet.isDone())
					throw new PngjInputException("new IDAT-like chunk when previous was not done");
				curDeflatedSet = createIdatSet(id);
			}
			curChunkReader = new DeflatedChunkReader(len, id, checkCrc, offset, curDeflatedSet) {
				@Override
				protected void chunkDone() {
					super.chunkDone();
					postProcessChunk(this);
				}
			};

		} else { // for non-idat chunks (or skipped idat like)
			curChunkReader = createChunkReaderForNewChunk(id, len, offset, skip);
		}
		if (curChunkReader != null && !checkCrc)
			curChunkReader.setCrcCheck(false);
	}

	/**
	 * This will be called for all chunks (even skipped), except for IDAT-like
	 * non-skiped chunks
	 * 
	 * The default behaviour is to create a ChunkReader in BUFFER mode (or SKIP
	 * if skip==true) that calls {@link #postProcessChunk(ChunkReader)} (always)
	 * when done.
	 *
	 * @param id
	 *            Chunk id
	 * @param len
	 *            Chunk length
	 * @param offset
	 *            offset inside PNG stream , merely informative
	 * @param skip
	 *            flag: is true, the content will not be buffered (nor
	 *            processed)
	 * @return a newly created ChunkReader that will create the ChunkRaw and
	 *         then discarded
	 */
	protected ChunkReader createChunkReaderForNewChunk(String id, int len, long offset, boolean skip) {
		return new ChunkReader(len, id, offset, skip ? ChunkReaderMode.SKIP : ChunkReaderMode.BUFFER) {
			@Override
			protected void chunkDone() {
				postProcessChunk(this);
			}

			@Override
			protected void processData(int offsetinChhunk, byte[] buf, int off, int len) {
				throw new PngjExceptionInternal("should never happen");
			}
		};
	}

	/**
	 * This is called after a chunk is read, in all modes
	 * 
	 * This implementation only chenks the id of the first chunk, and process
	 * the IEND chunk (sets done=true)
	 ** 
	 * Further processing should be overriden (call this first!)
	 **/
	protected void postProcessChunk(ChunkReader chunkR) { // called after chunk is read
		if (chunkCount == 1) {
			// first chunk ?
			String cid = firstChunkId();
			if (cid != null && !cid.equals(chunkR.getChunkRaw().id)) {
				String msg = "Bad first chunk: " + chunkR.getChunkRaw().id + " expected: " + firstChunkId();
				if (errorBehaviour.c < ErrorBehaviour.SUPER_LENIENT.c)
					throw new PngjInputException(msg);
				else
					LOGGER.warn(msg);
			}
		}
		if (endChunkId() != null && chunkR.getChunkRaw().id.equals(endChunkId())) {
			// last chunk ?
			endChunkDone = true;
			close();
		}
	}

	/**
	 * DeflatedChunksSet factory. This implementation is quite dummy, it usually
	 * should be overriden.
	 */
	protected abstract DeflatedChunksSet createIdatSet(String id);

	/**
	 * Decides if this Chunk is of "IDAT" kind (in concrete: if it is, and if
	 * it's not to be skiped, a DeflatedChunksSet will be created to deflate it
	 * and process+ the deflated data)
	 * 
	 * This implementation always returns always false
	 * 
	 * @param id
	 */
	protected boolean isIdatKind(String id) {
		return false;
	}

	/**
	 * Chunks can be skipped depending on id and/or length. Skipped chunks are
	 * still processed, but their data will be null, and CRC will never checked
	 * 
	 * @param len
	 * @param id
	 */
	protected boolean shouldSkipContent(int len, String id) {
		return false;
	}

	protected boolean shouldCheckCrc(int len, String id) {
		return true;
	}

	/**
	 * Throws PngjInputException if bad signature
	 * 
	 * @param buf
	 *            Signature. Should be of length 8
	 */
	protected void checkSignature(byte[] buf) {
		if (!Arrays.equals(buf, PngHelperInternal.getPngIdSignature()))
			throw new PngjBadSignature("Bad signature:" + Arrays.toString(buf));
	}

	/**
	 * If false, we are still reading the signature
	 * 
	 * @return true if signature has been read (or if we don't have signature)
	 */
	public boolean isSignatureDone() {
		return signatureDone;
	}

	/**
	 * If true, we have processe the IEND chunk
	 */
	@Override
	public boolean isDone() {
		return endChunkDone;
	}

	/**
	 * Terminated, normally or not - If true, this will not accept more bytes 
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * total of bytes read (buffered or not)
	 */
	public long getBytesCount() {
		return bytesCount;
	}

	/**
	 * @return Chunks already read, including partial reading (currently
	 *         reading)
	 */
	public int getChunkCount() {
		return chunkCount;
	}

	/**
	 * Currently reading chunk, or just ended reading
	 * 
	 * @return null only if still reading signature
	 */
	public ChunkReader getCurChunkReader() {
		return curChunkReader;
	}

	/**
	 * The latest deflated set (typically IDAT chunks) reader. Notice that there
	 * could be several idat sets (eg for APNG)
	 */
	public DeflatedChunksSet getCurDeflatedSet() {
		return curDeflatedSet;
	}

	/**
	 * Closes this object and release resources. For normal termination or
	 * abort. Secure and idempotent.
	 */
	public void close() { // forced closing
		if (curDeflatedSet != null) {
			curDeflatedSet.close();
		}
		closed = true;
	}

	/**
	 * Returns true if we are not in middle of a chunk: we have just ended
	 * reading past chunk , or we are at the start, or end of signature, or we
	 * are done
	 */
	public boolean isAtChunkBoundary() {
		return bytesCount == 0 || bytesCount == 8 || closed || curChunkReader == null || curChunkReader.isDone();
	}

	/**
	 * Which should be the id of the first chunk. This will be checked
	 * 
	 * @return null if you don't want to check it
	 */
	protected String firstChunkId() {
		return "IHDR";
	}

	/**
	 * Helper method, reports amount of bytes inside IDAT chunks.
	 * 
	 * @return Bytes in IDAT chunks
	 */
	public long getIdatBytes() {
		return idatBytes;
	}

	/**
	 * Which should be the id of the last chunk
	 */
	protected String endChunkId() {
		return "IEND";
	}

	/**
	 * Reads all content from a file. Helper method, only for callback mode
	 */
	public void feedFromFile(File f) {
		try {
			feedFromInputStream(new FileInputStream(f), true);
		} catch (FileNotFoundException e) {
			throw new PngjInputException("Error reading from file '" + f + "' :" + e.getMessage());
		}
	}

	/**
	 * Reads all content from an input stream. Helper method, only for callback
	 * mode
	 * 
	 * Caller should call isDone() to assert all expected chunks have been read
	 * 
	 * Warning: this does not close this object, unless ended 
	 * 
	 * @param is
	 * @param closeStream
	 *            Closes the input stream when done (or if error)
	 */
	public void feedFromInputStream(InputStream is, boolean closeStream) {
		BufferedStreamFeeder sf = new BufferedStreamFeeder(is);
		sf.setCloseStream(closeStream);
		try {
			sf.feedAll(this);
		} finally {
			sf.close();
		}
	}

	public void feedFromInputStream(InputStream is) {
		feedFromInputStream(is, true);
	}

	public void setErrorBehaviour(ErrorBehaviour errorBehaviour) {
		this.errorBehaviour = errorBehaviour;
	}

}
