package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngjException;

/**
 * Pseudo chunk type, for chunks that were not buffered on reading (skip or idat-like)
 */
public class PngChunkUnbuffered extends PngChunk {

	public PngChunkUnbuffered(String id, ImageInfo info) {
		super(id, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.NONE;
	}

	@Override
	public ChunkRaw createRawChunk() {
		throw new PngjException("Non supported for a skipped chunk");
	}

	@Override
	public void parseFromRaw(ChunkRaw c) {
		throw new PngjException("Non supported for a skipped chunk");
	}

	@Override
	public PngChunk cloneForWrite(ImageInfo imgInfo) {
		throw new PngjException("Non supported for a skipped chunk");
	}

	@Override
	public boolean allowsMultiple() {
		return true;
	}

}
