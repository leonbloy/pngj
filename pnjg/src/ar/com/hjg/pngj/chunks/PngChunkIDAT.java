package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;

public class PngChunkIDAT extends PngChunkMultiple {
	// http://www.w3.org/TR/PNG/#11IDAT
	// This is dummy placeholder - we write/read this chunk (actually several)
	// by special code.
	public PngChunkIDAT(ImageInfo i) {
		super(ChunkHelper.IDAT, i);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.NA;
	}

	@Override
	public ChunkRaw createRawChunk() {// does nothing
		return null;
	}

	@Override
	public void parseFromRaw(ChunkRaw c) { // does nothing
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
	}
}
