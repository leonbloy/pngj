package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;

public class PngChunkTEXT extends PngChunkTextVar {
	public PngChunkTEXT(ImageInfo info) {
		super(ChunkHelper.tEXt_TEXT, info);
	}

	@Override
	public ChunkRaw createChunk() {
		if (val.isEmpty() || key.isEmpty())
			return null;
		byte[] b = (key + "\0" + val).getBytes(PngHelper.charsetLatin1);
		ChunkRaw chunk = createEmptyChunk(b.length, false);
		chunk.data = b;
		return chunk;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		String[] k = (new String(c.data, PngHelper.charsetLatin1)).split("\0");
		key = k[0];
		val = k[1];
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkTEXT otherx = (PngChunkTEXT) other;
		key = otherx.key;
		val = otherx.val;
	}
}
