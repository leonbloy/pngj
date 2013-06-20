package ar.com.hjg.pngj;

import java.util.Arrays;

import ar.com.hjg.pngj.ChunkReader.ChunkReaderMode;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.PngChunkIEND;

/**
 * Consumes a stream of bytes from a PNG image.
 * 
 * It can operate in callback (async, nonblocking) mode, or in polling (sync) mode
 *  
 * 
 *
 */
public class ChunkReaderFullSequence implements IBytesConsumer {
	private byte[] buf0 = new byte[8]; // for signature or chunk starts
	private int buf0len = 0;
	protected boolean signatureDone = false;
	protected boolean done = false; // for end chunk, or error

	protected long bytesRead = 0;
	protected ChunkReader curChunkReader;

	protected ChunkReaderDeflatedSet curReaderDeflatedSet;
	protected IChunkProcessor chunkProcessor=null; // non null for callback mode

	public ChunkReaderFullSequence() {
	}

	public void setChunkProcessor(IChunkProcessor chunkProcessor) {
		this.chunkProcessor = chunkProcessor;
	}

	/** returns processed bytes, -1 if done */
	public int feed(byte[] buf, int off, int len) { // each 
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
				len -= read0;
				off += read0;
				if (buf0len == 8) {
					startNewChunk(PngHelperInternal.readInt4fromBytes(buf0, 0), ChunkHelper.toString(buf0, 4, 4));
					buf0len = 0;
				}
			} 
			else	// reading chunk, delegate to curChunkReader
				processed += curChunkReader.feedBytes(buf, off, len);
		} else { // reading signature
			int read = 8 - buf0len;
			if (read > len)
				read = len;
			System.arraycopy(buf, off, buf0, buf0len, read);
			buf0len += read;
			if (buf0len == 8) {
				processSignature(buf0);
				buf0len = 0;
				signatureDone = true;
			}
			processed += read;
		}
		bytesRead += processed;
		return processed;
	}

	/**
	 * Only in callback mode!
	 * 
	 * @param buf
	 * @param off
	 * @param len
	 */
	public void feedBytesFull(byte[] buf, int off, int len) { // each
		while (len > 0) {
			int n = feed(buf, off, len);
			if (n < 1)
				return;
			len -= n;
			off += n;
		}
	}

	protected void processChunk(ChunkReader chunkR) { // called after chunk is read
		if (chunkR.getChunkRaw().id.equals(PngChunkIEND.ID))
			done = true;
		chunkProcessor.processChunkEnd(chunkR);
	}

	protected void startNewChunk(int len, String id) { // this creates the ChunkReader
		boolean checkCrc = chunkProcessor.shouldCheckCrc(len, id);
		boolean skip = chunkProcessor.shouldSkipContent(len, id);
		boolean isIdatType = chunkProcessor.isIdatKind(id);
		if (isIdatType && !skip) { // IDAT with HOT PROCESS mode
			if (curReaderDeflatedSet == null || curReaderDeflatedSet.isAllDone()) {
				curReaderDeflatedSet = chunkProcessor.createNewIdatSetReader(id);
			}
			curChunkReader = new ChunkReaderDeflated(len, id, checkCrc, bytesRead, curReaderDeflatedSet) {
				@Override
				protected void chunkDone() {
					processChunk(this);
				}
			};
			curReaderDeflatedSet.newChunk((ChunkReaderDeflated) curChunkReader);
		} else { // BUFFER or SKIP (might include skipped Idat like chunks)
			if (curReaderDeflatedSet != null && !curReaderDeflatedSet.isAllDone()
					&& !curReaderDeflatedSet.allowOtherChunksInBetween())
				throw new PngjInputException("chunks interleaved with IDAT-like chunks not allowed");
			curChunkReader = new ChunkReader(len, id, bytesRead, skip ? ChunkReaderMode.SKIP : ChunkReaderMode.BUFFER) {
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
		chunkProcessor.processChunkStart(curChunkReader);
	}

	/**
	 * returns true if we have just ended reading past chunk - also if we are at
	 * the start or end of signature, or we are done
	 */
	public boolean isAtChunkBoundary() {
		return bytesRead == 0 || bytesRead == 8 || done || curChunkReader.isDone();
	}

	protected void processSignature(byte[] buf) {
		if (!Arrays.equals(buf, PngHelperInternal.getPngIdSignature()))
			throw new PngjInputException("Bad PNG signature");
	}

	public boolean isSignatureDone() {
		return signatureDone;
	}

	public boolean isDone() {
		return done;
	}

	public long getReadBytes() {
		return bytesRead;
	}

	public ChunkReader getCurChunkReader() {
		return curChunkReader;
	}

	public ChunkReaderDeflatedSet getCurReaderDeflatedSet() {
		return curReaderDeflatedSet;
	}
	
	public void close() { // forced closing
		if(curReaderDeflatedSet!=null) 
			curReaderDeflatedSet.end();
	}
}
