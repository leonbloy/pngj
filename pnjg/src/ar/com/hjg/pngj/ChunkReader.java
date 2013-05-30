package ar.com.hjg.pngj;

import java.io.IOException;
import java.io.InputStream;

import ar.com.hjg.pngj.chunks.ChunkRaw;

/**
 * This object should be short lived -although the buffer content can be passed
 * to the long-live chunk object
 */
public abstract class ChunkReader {

	private int read = 0; // bytes read, data only
	private int crcLen = 0; // how many bytes have been read from crc 
	private final boolean crcCheck;
	protected boolean bufferFullContent = true;
	protected boolean skipContent = false;
	private final ChunkRaw chunkRaw;

	public ChunkReader(int clen, String id, boolean checkcrc, long offsetInPng) {
		chunkRaw = new ChunkRaw(clen, id, false);
		chunkRaw.setOffset(offsetInPng);
		this.crcCheck = checkcrc;
		if (crcCheck) {
			chunkRaw.updateCrc(chunkRaw.idbytes, 0, 4);
		}
	}

	/**
	 * 
	 * @param buf
	 *            This is totally ignored (can even be null) if skipContent=true
	 * @param off
	 * @param len
	 * @return
	 */
	public final int feed(byte[] buf, int off, int len) {
		int dataRead = chunkRaw.len - read;
		if (dataRead > len)
			dataRead = len;
		if (dataRead > 0) { // read some data
			if (!skipContent) {
				if (bufferFullContent) { // just copy the contents to the internal buffer
					chunkRaw.allocData();
					if (chunkRaw.data != buf) // if the buffer passed if the same as this one, we do nothing 
						System.arraycopy(buf, off, chunkRaw.data, read, dataRead);
				} else {
					processData(buf, off, dataRead);
				}
				if (crcCheck)
					chunkRaw.updateCrc(buf, off, dataRead);
			}
			read += dataRead;
			off += dataRead;
			len -= dataRead;
		}
		int crcRead = 0;
		if (read == chunkRaw.len) { // data done - read crc?
			crcRead = 4 - crcLen;
			if (crcRead > len)
				crcRead = len;
			if (crcRead > 0) {
				if (buf != chunkRaw.crcval && !skipContent)
					System.arraycopy(buf, off, chunkRaw.crcval, crcLen, crcRead);
				crcLen += crcRead;
				if (crcLen == 4) {
					if (crcCheck && !skipContent)
						chunkRaw.checkCrc();
					chunkDone();
				}
			}
		}
		return dataRead + crcRead;
	}

	/** Reads full chunk, blocking. Calls internally feed() */
	@Deprecated
	public final int read(InputStream is) {
		int cont = 0;
		int toRead = (chunkRaw.len - read); // data only
		try {
			int pos = 0;
			byte[] b = null;
			if (bufferFullContent && !skipContent) {
				chunkRaw.allocData();
				b = chunkRaw.data;
				pos = read;
			}
			int r = 0;
			while (toRead > 0) {
				if (!skipContent) {
					if (b == null)
						b = new byte[toRead > 8192 ? 8192 : toRead]; // allocated in first loop iteration - TODO: see length
					r = is.read(b, pos, toRead > b.length ? b.length : toRead);
				} else {
					r = (int) is.skip(toRead);
				}
				feed(b, pos, r);
				toRead -= r;
				cont += r;
			}
			//crc
			toRead = 4 - crcLen;
			if (toRead > 0) {
				if (skipContent)
					PngHelperInternal.skipBytes(is, toRead);
				else
					PngHelperInternal.readBytes(is, chunkRaw.crcval, 0, toRead);
				cont += toRead;
				feed(chunkRaw.crcval, 0, toRead);
			}
		} catch (IOException e) {
			throw new PngjInputException("Error reading, offset=" + (chunkRaw.getOffset() + read + crcLen), e);
		}
		return cont;
	}

	public final boolean isDone() {
		return crcLen == 4;
	}

	@Override
	public String toString() {
		return chunkRaw.toString();
	}

	/**
	 * guaranteed: the length to read is positive and corresponds only to data
	 * (no other chunks, no crc) Warning: if bufferFullContent==true this will
	 * not be called, parsing should be done in chunkDone()
	 */
	protected void processData(byte[] buf, int off, int len) {
	}

	/** will be called when full chunk, including crc, is read */
	protected void chunkDone() {
	}

	public ChunkRaw getChunkRaw() {
		return chunkRaw;
	}
}
