package ar.com.hjg.pngj;

import ar.com.hjg.pngj.chunks.ChunkRaw;

/**
 * This object should be short lived -although the buffer content can be passed
 * to the long-live chunk object
 */
public abstract class ChunkReader {

	protected final ChunkReaderMode mode;
	private boolean crcCheck; // default: false for SKIP, true elsewhere

	private final ChunkRaw chunkRaw;

	private int read = 0; // bytes read, data only
	private int crcn = 0; // how many bytes have been read from crc 

	public enum ChunkReaderMode {
		HOT_PROCESS, // does not store content, calls processData 
		BUFFER, // stores full content in buffer, calls chunkDone at the end
		SKIP; // does not store nor process - implies crcCheck=false (default). it calls chunkDone
	}

	public ChunkReader(int clen, String id, long offsetInPng, ChunkReaderMode mode) {
		if (mode == null || id.length() != 4 || clen < 0)
			throw new PngjExceptionInternal("Bad chunk paramenters: " + mode);
		this.mode = mode;
		chunkRaw = new ChunkRaw(clen, id, mode == ChunkReaderMode.BUFFER);
		chunkRaw.setOffset(offsetInPng);
		this.crcCheck = mode == ChunkReaderMode.SKIP ? false : true; // can be changed with setter
	}

	/**
	 * 
	 * 
	 * @param buf 
	 *            
	 * @param off
	 * @param len
	 * @return
	 */
	public final int feedBytes(byte[] buf, int off, int len) {
		if(len==0) return 0;
		if(len<0) throw new PngjException("negative length??");
		if (read == 0 && crcn == 0 && crcCheck)
			chunkRaw.updateCrc(chunkRaw.idbytes, 0, 4);
		int dataRead = chunkRaw.len - read;
		if (dataRead > len)
			dataRead = len;
		if (dataRead > 0) { // read some data
			if (crcCheck && mode != ChunkReaderMode.BUFFER) // in buffer mode we compute the CRC at the end
				chunkRaw.updateCrc(buf, off, dataRead);
			if (mode == ChunkReaderMode.BUFFER) { // just copy the contents to the internal buffer
				if (chunkRaw.data != buf) // if the buffer passed if the same as this one, we don't copy 
					System.arraycopy(buf, off, chunkRaw.data, read, dataRead);
			} else if (mode == ChunkReaderMode.HOT_PROCESS) {
				processData(buf, off, dataRead);
			} else if (mode == ChunkReaderMode.SKIP) {
				// nothing to do
			}
			read += dataRead;
			off += dataRead;
			len -= dataRead;
		}
		int crcRead = 0;
		if (read == chunkRaw.len) { // data done - read crc?
			crcRead = 4 - crcn;
			if (crcRead > len)
				crcRead = len;
			if (crcRead > 0) {
				if (buf != chunkRaw.crcval)
					System.arraycopy(buf, off, chunkRaw.crcval, crcn, crcRead);
				crcn += crcRead;
				if (crcn == 4) {
					if (crcCheck) {
						if (mode == ChunkReaderMode.BUFFER) // in buffer mode we compute the CRC on one single call
							chunkRaw.updateCrc(chunkRaw.data, 0, chunkRaw.len);
						chunkRaw.checkCrc();
					}
					chunkDone();
				}
			}
		}
		return dataRead + crcRead;
	}

	public final boolean isDone() {
		return crcn == 4; // has read all 4 bytes from the crc
	}

	@Override
	public String toString() {
		return chunkRaw.toString();
	}

	public ChunkRaw getChunkRaw() {
		return chunkRaw;
	}

	public void setCrcCheck(boolean crcCheck) {
		if (read != 0 && crcCheck)
			throw new PngjInputException("cannot change this flag while reading!");
		this.crcCheck = crcCheck;
	}

	/**
	 * guaranteed: the length to read is positive and corresponds only to data
	 * (no other chunks, no crc) Warning: if bufferFullContent==true this will
	 * not be called, parsing should be done in chunkDone()
	 */
	protected void processData(byte[] buf, int off, int len) {
		throw new PngjException("processData not implemented");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chunkRaw == null) ? 0 : chunkRaw.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) { // delegates to chunkraw
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChunkReader other = (ChunkReader) obj;
		if (chunkRaw == null) {
			if (other.chunkRaw != null)
				return false;
		} else if (!chunkRaw.equals(other.chunkRaw))
			return false;
		return true;
	}

	/** will be called when full chunk, including crc, is read (for all modes) */
	protected abstract void chunkDone();
}
