package ar.com.hjg.pngj;

/**
 * This loads the png as a plain sequence of chunks, buffering all
 * 
 * Useful to do things like insert or delete a ancilllary chunk.
 * This does not distinguish IDAT from others
 **/
public class ChunkSeqBasic extends ChunkSeqReader {
	public boolean checkCrc = true;

	public ChunkSeqBasic(boolean checkCrc) {
		this.checkCrc = checkCrc;
	}

	@Override
	protected void postProcessChunk(ChunkReader chunkR) {
		super.postProcessChunk(chunkR);
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
