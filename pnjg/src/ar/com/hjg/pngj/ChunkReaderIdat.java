package ar.com.hjg.pngj;


/**
 * For IDAT (and IDAT like) chunks, a set which conform a zlib stream
 */
public class ChunkReaderIdat extends ChunkReader {

	private final ChunkReaderIdatSet idatSet;
	
	
	public ChunkReaderIdat(int clen, String chunkid, boolean checkCrc, long offsetInPng,ChunkReaderIdatSet iDatSet) {
		super(clen, chunkid, checkCrc, offsetInPng);
		this.idatSet=iDatSet;
		iDatSet.reportNewChunk(chunkid, clen, offsetInPng);
	}

	@Override
	protected void processData(byte[] buf, int off, int len) {
		if (len > 0) {
			idatSet.inf.setInput(buf, off, len);
			idatSet.process();
		}
	}

	@Override
	protected void chunkDone() {
		//nothing to do here
	}



}
