package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;

public class PngChunkITXT extends PngChunkTextVar {
	// http://www.w3.org/TR/PNG/#11iTXt
	public PngChunkITXT(ImageInfo info) {
		super(ChunkHelper.tEXt_TEXT, info);
	}

	@Override
	public ChunkRaw createChunk() {
		// TODO Implement
		throw new RuntimeException("not implemented");
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		throw new RuntimeException("not implemented");
	}
}
