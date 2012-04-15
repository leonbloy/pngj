package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelperInternal;

public class PngChunkTEXT extends PngChunkTextVar {
	public final static String ID = ChunkHelper.tEXt;

	public PngChunkTEXT(ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkRaw createRawChunk() {
		if (val.isEmpty() || key.isEmpty())
			return null;
		byte[] b = (key + "\0" + val).getBytes(PngHelperInternal.charsetLatin1);
		ChunkRaw chunk = createEmptyChunk(b.length, false);
		chunk.data = b;
		return chunk;
	}

	@Override
	public void parseFromRaw(ChunkRaw c) {
		String[] k = (new String(c.data, PngHelperInternal.charsetLatin1)).split("\0");
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
