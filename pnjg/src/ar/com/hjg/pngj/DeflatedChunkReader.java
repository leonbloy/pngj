package ar.com.hjg.pngj;


/**
 * 
 * Specialization of ChunkReader, for IDAT-like chunks. 
 * These chunks are part of a set of similar chunks
 * (contiguos normally, not necessariyl) which conforms a zlib stream
 */
public class DeflatedChunkReader extends ChunkReader {

	
	protected final DeflatedChunksSet deflatedChunksSet;
	protected boolean alsoBuffer = false;
	
	public DeflatedChunkReader(int clen, String chunkid, boolean checkCrc, long offsetInPng,
			DeflatedChunksSet iDatSet) {
		super(clen, chunkid, offsetInPng, ChunkReaderMode.HOT_PROCESS);
		this.deflatedChunksSet = iDatSet;
		iDatSet.appendNewChunk(this);
	}
	
	
	
    /**
     * Delegates to ChunkReaderDeflatedSet.processData()
     */
	@Override
	protected void processData(byte[] buf, int off, int len) {
		if (len > 0) { // delegate to idatSet
			deflatedChunksSet.processBytes(buf, off, len);
			if(alsoBuffer) { // very rare!
				System.arraycopy(buf, off, getChunkRaw().data, read, len);
			}
		}
	}

	/**
	 * Nothing to do
	 */
	@Override
	protected void chunkDone() {
		// nothing to do. you can override
	}

	/**
	 * In some rare cases you might want to also buffer the data?
	 */
	public void setAlsoBuffer() {
		if(read>0) throw new RuntimeException("too late");
		alsoBuffer=true;
		getChunkRaw().allocData();
	}

}
