package ar.com.hjg.pngj;

/**
 * For IDAT (and IDAT like) chunks, a chunk inside a set which conform a zlib
 * stream
 */
public class ChunkReaderDeflated extends ChunkReader {

	private final ChunkReaderDeflatedSet idatSet;

	public ChunkReaderDeflated(int clen, String chunkid, boolean checkCrc, long offsetInPng,
			ChunkReaderDeflatedSet iDatSet) {
		super(clen, chunkid, offsetInPng, ChunkReaderMode.HOT_PROCESS);
		this.idatSet = iDatSet;
		iDatSet.newChunk(this);
	}

	@Override
	protected void processData(byte[] buf, int off, int len) {
		if (len > 0) { // delegate to idatSet
			idatSet.processBytes(buf, off, len);
		}
	}

	@Override
	protected void chunkDone() {
		// nothing to do. you can override
	}

}
