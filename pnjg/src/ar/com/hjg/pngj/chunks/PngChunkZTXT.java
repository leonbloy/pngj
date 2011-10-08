package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;

public class PngChunkZTXT extends PngChunkTextVar {
	// http://www.w3.org/TR/PNG/#11zTXt
	public PngChunkZTXT(ImageInfo info) {
		super(ChunkHelper.zTXt_TEXT, info);
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
