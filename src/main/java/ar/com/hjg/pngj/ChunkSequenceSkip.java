package ar.com.hjg.pngj;

import java.util.ArrayList;
import java.util.List;

import ar.com.hjg.pngj.chunks.ChunkRaw;

/**
 * This class is ultra dummy, skips all chunks contents. Useful to read chunks
 * structure
 */
public class ChunkSequenceSkip extends ChunkSeqReader {

	private List<ChunkRaw> chunks = new ArrayList<ChunkRaw>();

	public ChunkSequenceSkip() {
		super(true);
	}

	@Override
	protected void postProcessChunk(ChunkReader chunkR) {
		super.postProcessChunk(chunkR);
		chunks.add(chunkR.getChunkRaw());
	}

	@Override
	protected boolean isIdatKind(String id) {
		return false;
	}

	@Override
	protected boolean shouldSkipContent(int len, String id) {
		return true;
	}

	public List<ChunkRaw> getChunks() {
		return chunks;
	}

}
