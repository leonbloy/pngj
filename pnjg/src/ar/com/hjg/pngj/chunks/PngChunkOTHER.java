package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;

public class PngChunkOTHER extends PngChunk { // unkown, custom or not
																							// implemented chunks
	private byte[] data;

	public PngChunkOTHER(String id, ImageInfo info) {
		super(id, info);
	}

	private PngChunkOTHER(PngChunkOTHER c, ImageInfo info) {
		super(c.id, info);
		System.arraycopy(c.data, 0, data, 0, c.data.length);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw p = createEmptyChunk(data.length, false);
		p.data = this.data;
		return p;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		data = c.data;
	}

	/* does not copy! */
	public byte[] getData() {
		return data;
	}

	/* does not copy! */
	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		// THIS SHOULD NOT BE CALLED IF ALREADY CLONED WITH COPY CONSTRUCTOR
		PngChunkOTHER c = (PngChunkOTHER) other;
		data = c.data; // not deep copy
	}
}
