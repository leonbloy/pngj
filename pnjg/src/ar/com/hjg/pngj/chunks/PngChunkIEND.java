package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;

public class PngChunkIEND extends PngChunkSingle {
	// http://www.w3.org/TR/PNG/#11IEND
	// this is a dummy placeholder
	public PngChunkIEND(ImageInfo info) {
		super(ChunkHelper.IEND, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.NA;
	}

	@Override
	public ChunkRaw createRawChunk() {
		ChunkRaw c = new ChunkRaw(0, ChunkHelper.b_IEND, false);
		return c;
	}

	@Override
	public void parseFromRaw(ChunkRaw c) {
		// this is not used
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
	}
}
