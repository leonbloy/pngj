package ar.com.hjg.pngj;

/** 
 * This loads the png as a plain sequence of chunks, buffering all
 * 
 * Useful to do things like insert or delete a ancilllary chunk 
 **/
public class ChunkSeqBasic extends ChunkSeqReader {
	public boolean checkCrc=true;
	
	public ChunkSeqBasic(boolean checkCrc) {
		this.checkCrc = checkCrc;
	}

	@Override
	protected void processChunk(ChunkReader chunkR) {
		super.processChunk(chunkR);
	}

	@Override
	protected boolean isIdatKind(String id) {
		return false;
	}

	@Override
	protected boolean shouldCheckCrc(int len, String id) {
		return checkCrc;
	}
	
}
