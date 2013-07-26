package ar.com.hjg.pngj;

import ar.com.hjg.pngj.chunks.ChunkRaw;

/**
 * Parses a PNG chunk, consuming bytes in one of three modes
 * (BUFFER,HOT_PROCESS,SKIP).
 * 
 * It calls chunkDone() when done, and processData() if HOT_PROCESS Apart from
 * this, it's totally agnostic (it doesn't know about IDAT chunks, or PNG
 * general structure)
 * 
 * It wraps a ChunkRaw instance (content filled only if BUFFER mode)
 * 
 * This object should be short lived (one instance created for each chunk, and
 * discarded after reading), but the wrapped chunkRaw can be long lived.
 */
public abstract class ChunkReader {

	protected final ChunkReaderMode mode;
	private final ChunkRaw chunkRaw;

	private boolean crcCheck; // by default, this is false for SKIP, true elsewhere

	/**
	 * How many bytes have been read for this chunk, data only
	 */
	protected int read = 0;
	private int crcn = 0; // how many bytes have been read from crc 

	/**
	 * Modes of ChunkReader chunk processing.
	 */
	public enum ChunkReaderMode {
		/**
		 * Stores full chunk data in buffer
		 */
		BUFFER,
		/**
		 * Does not store content, processes on the fly, calling processData()
		 * for each partial read
		 */
		PROCESS,
		/**
		 * Does not store nor process - implies crcCheck=false (by default).
		 */
		SKIP;
	}

	/**
	 * The constructor creates also a chunkRaw, preallocated if mode =
	 * ChunkReaderMode.BUFFER
	 * 
	 * @param clen
	 * @param id
	 * @param offsetInPng
	 *            Informational, is stored in chunkRaw
	 * @param mode
	 */
	public ChunkReader(int clen, String id, long offsetInPng, ChunkReaderMode mode) {
		if (mode == null || id.length() != 4 || clen < 0)
			throw new PngjExceptionInternal("Bad chunk paramenters: " + mode);
		this.mode = mode;
		chunkRaw = new ChunkRaw(clen, id, mode == ChunkReaderMode.BUFFER);
		chunkRaw.setOffset(offsetInPng);
		this.crcCheck = mode == ChunkReaderMode.SKIP ? false : true; // can be changed with setter
	}

	/**
	 * Returns raw chunk (data can be empty or not, depending on
	 * ChunkReaderMode)
	 * 
	 * @return Raw chunk - never null
	 */
	public ChunkRaw getChunkRaw() {
		return chunkRaw;
	}

	/**
	 * Consumes data for the chunk (data and CRC). This never consumes more
	 * bytes than for this chunk.
	 * 
	 * In HOT_PROCESS can call processData() (not more than once)
	 * 
	 * If this ends the chunk (included CRC) it checks CRC (if checking) and
	 * calls chunkDone()
	 * 
	 * @param buf
	 * @param off
	 * @param len
	 * @return How many bytes have been consumed
	 */
	public final int feedBytes(byte[] buf, int off, int len) {
		if (len == 0)
			return 0;
		if (len < 0)
			throw new PngjException("negative length??");
		if (read == 0 && crcn == 0 && crcCheck)
			chunkRaw.updateCrc(chunkRaw.idbytes, 0, 4); // initializes crc calculation with the Chunk ID
		int dataRead = chunkRaw.len - read; // dataRead : bytes to be actually read from chunk data
		if (dataRead > len)
			dataRead = len;
		if (dataRead > 0) { // read some data
			if (crcCheck && mode != ChunkReaderMode.BUFFER) // in buffer mode we compute the CRC at the end
				chunkRaw.updateCrc(buf, off, dataRead);
			if (mode == ChunkReaderMode.BUFFER) { // just copy the contents to the internal buffer
				if (chunkRaw.data != buf) // if the buffer passed if the same as this one, we don't copy  (the caller should know what he's doing
					System.arraycopy(buf, off, chunkRaw.data, read, dataRead);
			} else if (mode == ChunkReaderMode.PROCESS) {
				processData(buf, off, dataRead);
			} else {
				//mode == ChunkReaderMode.SKIP; nothing to do
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

	/**
	 * Chunks has been read
	 * 
	 * @return true if we have read all chunk, including trailing CRC
	 */
	public final boolean isDone() {
		return crcn == 4; // has read all 4 bytes from the crc
	}

	/**
	 * Determines if CRC should be checked. This should be called before
	 * starting reading.
	 * 
	 * @param crcCheck
	 */
	public void setCrcCheck(boolean crcCheck) {
		if (read != 0 && crcCheck && !this.crcCheck)
			throw new PngjException("too late!");
		this.crcCheck = crcCheck;
	}

	/**
	 * This method will only be called in PROCESS mode, probably several times,
	 * each time with a new fragment of data
	 * 
	 * It's guaranteed that the data to read has non-zero length and it
	 * corresponds exclusively to this chunk data (no crc, no data from no other
	 * chunks, )
	 */
	protected abstract void processData(byte[] buf, int off, int len);

	/**
	 * This method will be called (in all modes) when the full chunk -including
	 * crc- has been read
	 */
	protected abstract void chunkDone();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((chunkRaw == null) ? 0 : chunkRaw.hashCode());
		return result;
	}

	/**
	 * Equality (and hash) is basically delegated to the ChunkRaw
	 */
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

	@Override
	public String toString() {
		return chunkRaw.toString();
	}

}
