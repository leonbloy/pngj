package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;

public class PngChunkIDAT extends PngChunk {
	// http://www.w3.org/TR/PNG/#11IDAT
	// This is dummy placeholder - we write/read this chunk (actually several)
	// by special code.
	public PngChunkIDAT(ImageInfo i) {
		super(ChunkHelper.IDAT_TEXT, i);
	}

	@Override
	public ChunkRaw createChunk() {// does nothing
		return null;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) { // does nothing
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
	}
}
