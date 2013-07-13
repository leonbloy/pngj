package ar.com.hjg.pngj;

import java.util.Arrays;

import ar.com.hjg.pngj.ChunkReader.ChunkReaderMode;
import ar.com.hjg.pngj.chunks.ChunkHelper;

/**
 * Consumes a stream of bytes that consist of a series of PNG-like chunks
 * (eventually prefixed by a PNG signature)
 * 
 * This has very little intelligence, and is quite general, it could even be
 * used for a MNG stream
 * 
 */
public class ChunkSeqReader implements IBytesConsumer {

	protected final boolean withSignature;

	protected static final int SIGNATURE_LEN = 8;
	private byte[] buf0 = new byte[8]; // for signature or chunk starts
	private int buf0len = 0;
	protected boolean signatureDone = false;
	protected boolean done = false; // for end chunk,  error, or abort

	protected int chunkCount = 0; // chunks already found (including currently reading)
	/**
	 * total of bytes read (buffered or not)
	 */
	protected long bytesCount = 0;

	protected ChunkReader curChunkReader;
	protected DeflatedChunksSet curReaderDeflatedSet;

	public ChunkSeqReader() {
		this(true);
	}

	public ChunkSeqReader(boolean withSignature) {
		this.withSignature = withSignature;
		signatureDone = !withSignature;
	}

	/**
	 * Calls startNewChunk() before starting reading a new chunk data The
	 * reading of chunk data is delegated to curChunkReader.feedBytes()
	 * 
	 * Returns processed bytes, -1 if done. The caller might want to call this
	 * method more than once
	 **/
	public int feed(byte[] buf, int off, int len) {
		if (done)
			return -1;
		if (len == 0)
			return 0; // nothing to do
		if (len < 0)
			throw new PngjInputException("Bad len: " + len);
		int processed = 0;
		if (signatureDone) {
			if (curChunkReader == null || curChunkReader.isDone()) { // new chunk: read first 8 bytes
				int read0 = 8 - buf0len;
				if (read0 > len)
					read0 = len;
				System.arraycopy(buf, off, buf0, buf0len, read0);
				buf0len += read0;
				processed += read0;
				bytesCount += read0;
				len -= read0;
				off += read0;
				if (buf0len == 8) { // end reading chunk length and id
					chunkCount++;
					int clen = PngHelperInternal.readInt4fromBytes(buf0, 0);
					String cid = ChunkHelper.toString(buf0, 4, 4);
					startNewChunk(clen, cid, bytesCount - 8);
					buf0len = 0;
				}
			} else { // reading chunk, delegates to curChunkReader
				int read1 = curChunkReader.feedBytes(buf, off, len);
				processed += read1;
				bytesCount += read1;
			}
		} else { // reading signature
			int read = SIGNATURE_LEN - buf0len;
			if (read > len)
				read = len;
			System.arraycopy(buf, off, buf0, buf0len, read);
			buf0len += read;
			if (buf0len == SIGNATURE_LEN) {
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
	 * Called when a chunk start has been read, before the chunk data itself is
	 * read.
	 * 
	 * This creates a new ChunkReader (saved in curChunkReader field) in the
	 * corrsponding mode, and eventually a curReaderDeflatedSet.
	 * 
	 * The ChunkReader.chunkDone() method is directed to this.processChunk()
	 * method
	 * 
	 */
	protected void startNewChunk(int len, String id, long offset) {
		boolean checkCrc = shouldCheckCrc(len, id);
		boolean skip = shouldSkipContent(len, id);
		boolean isIdatType = isIdatKind(id);
		// first see if we should terminate an active curReaderDeflatedSet
		if (curReaderDeflatedSet != null && (!curReaderDeflatedSet.chunkid.equals(id))
				&& !curReaderDeflatedSet.allowOtherChunksInBetween(id)) {
			if (!curReaderDeflatedSet.isDone())
				throw new PngjInputException("unexpected chunk while reading IDAT(like) set " + id);
			curReaderDeflatedSet = null;
		}
		if (isIdatType && !skip) { // IDAT with HOT PROCESS mode
			if (curReaderDeflatedSet == null)
				curReaderDeflatedSet = createIdatSet(id); // new 
			curChunkReader = new DeflatedChunkReader(len, id, checkCrc, offset, curReaderDeflatedSet) {
				@Override
				protected void chunkDone() {
					processChunk(this);
				}
			};
		} else { // BUFFER or SKIP (might include skipped Idat like chunks)
			curChunkReader = new ChunkReader(len, id, offset, skip ? ChunkReaderMode.SKIP : ChunkReaderMode.BUFFER) {
				@Override
				protected void chunkDone() {
					processChunk(this);
				}

				@Override
				protected void processData(byte[] buf, int off, int len) {
					throw new PngjExceptionInternal("should never happen");
				}
			};
			if (!checkCrc)
				curChunkReader.setCrcCheck(false);
		}
	}

	/**
	 * Called after a chunk is read.
	 * 
	 * This implementation only takes care of IHDR (creates iamgeINfo and
	 * deinterlacer) and IEND (sets done=true)
	 ** 
	 * Further processing should be overriden (call this first!)
	 **/
	protected void processChunk(ChunkReader chunkR) { // called after chunk is read
		if (chunkCount == 1 && firstChunkId() != null && !firstChunkId().equals(chunkR.getChunkRaw().id))
			throw new PngjInputException("Bad first chunk: " + chunkR.getChunkRaw().id + " expected: " + firstChunkId());
		if (chunkR.getChunkRaw().id.equals(endChunkId()))
			done = true;
	}

	protected DeflatedChunksSet createIdatSet(String id) {
		return new DeflatedChunksSet(id, 1024, 1024); // sizes: arbitrary This should normally be overriden
	}

	protected boolean isIdatKind(String id) {
		return false; // override in ChunkSequencePng
	}

	protected boolean shouldSkipContent(int len, String id) {
		return false;
	}

	protected boolean shouldCheckCrc(int len, String id) {
		return true;
	}

	protected void checkSignature(byte[] buf) {
		if (!Arrays.equals(buf, PngHelperInternal.getPngIdSignature()))
			throw new PngjInputException("Bad PNG signature");
	}

	public boolean isSignatureDone() {
		return signatureDone;
	}

	public boolean isDone() {
		return done;
	}

	public long getBytesCount() {
		return bytesCount;
	}

	public int getChunkCount() {
		return chunkCount;
	}

	public ChunkReader getCurChunkReader() {
		return curChunkReader;
	}

	public DeflatedChunksSet getCurReaderDeflatedSet() {
		return curReaderDeflatedSet;
	}

	public void close() { // forced closing
		if (curReaderDeflatedSet != null)
			curReaderDeflatedSet.end();
		done = true;
	}

	/**
	 * returns true if we have just ended reading past chunk - also if we are at
	 * the start or end of signature, or we are done
	 */
	public boolean isAtChunkBoundary() {
		return bytesCount == 0 || bytesCount == 8 || done || curChunkReader == null || curChunkReader.isDone();
	}

	/**
	 * @return null if you don't want to check it
	 */
	protected String firstChunkId() {
		return "IHDR";
	}

	protected String endChunkId() {
		return "IEND";
	}

	protected boolean signatureOk(byte[] b) {
		return b.length == SIGNATURE_LEN && Arrays.equals(b, PngHelperInternal.getPngIdSignature());
	}
}
