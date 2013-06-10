package ar.com.hjg.pngj;

import java.util.Arrays;

import ar.com.hjg.pngj.ChunkReader.ChunkReaderMode;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.PngChunkIEND;

public class ChunkReaderFullSequence implements IBytesConsumer {
	private byte[] buf0 = new byte[8]; // for signature or chunk starts
	private int buf0len = 0;
	protected boolean signatureDone = false;
	protected boolean done = false; // for end chunk, or error

	protected long readBytes = 0;
	protected ChunkReader curChunkReader;
	public ChunkReader getCurChunkReader() {
		return curChunkReader;
	}

	public ChunkReaderDeflatedSet getCurIdatSetReader() {
		return curIdatSetReader;
	}

	protected ChunkReaderDeflatedSet curIdatSetReader;
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
			if (curChunkReader == null || curChunkReader.isDone()) { // new chunk: read 8 bytes
				int t = 8 - buf0len;
				if (t > len)
					t = len;
				System.arraycopy(buf, off, buf0, buf0len, t);
				buf0len += t;
				processed += t;
				len -= t;
				off += t;
				if (buf0len == 8) {
					startNewChunk(PngHelperInternal.readInt4fromBytes(buf0, 0), ChunkHelper.toString(buf0, 4, 4));
					buf0len = 0;
				}
				return processed + feed(buf, off, len);
			} 
			// reading chunk 
			processed += curChunkReader.feedBytes(buf, off, len);
		} else { // parsing signature
			int read = 8 - buf0len;
			if (read > len)
				read = len;
			System.arraycopy(buf, off, buf0, buf0len, read);
			buf0len += read;
			readBytes += read;
			if (buf0len == 8) {
				processSignature(buf0);
				buf0len = 0;
				signatureDone = true;
			}
			processed += read;
		}
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
		boolean isIdatType = chunkProcessor.isIdatLike(id);
		if (isIdatType && !skip) {
			if (curIdatSetReader == null || curIdatSetReader.isAllDone()) {
				curIdatSetReader = chunkProcessor.createNewIdatSetReader(id);
			}
			curChunkReader = new ChunkReaderDeflated(len, id, checkCrc, readBytes, curIdatSetReader) {
				@Override
				protected void chunkDone() {
					processChunk(this);
				}
			};
			curIdatSetReader.newChunk((ChunkReaderDeflated) curChunkReader);
		} else {
			if (curIdatSetReader != null && !curIdatSetReader.isAllDone()
					&& !curIdatSetReader.allowOtherChunksInBetween())
				throw new PngjInputException("chunks interleaved with IDAT not supported");
			curChunkReader = new ChunkReader(len, id, readBytes, skip ? ChunkReaderMode.SKIP : ChunkReaderMode.BUFFER) {
				@Override
				protected void chunkDone() {
					processChunk(this);
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
		return readBytes == 0 || readBytes == 8 || done || curChunkReader.isDone();
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
		return readBytes;
	}

}
