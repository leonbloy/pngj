package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngjException;

/**
 * tEXt chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11tEXt
 */
public class PngChunkTEXT extends PngChunkTextVar {
	public final static String ID = ChunkHelper.tEXt;

	public PngChunkTEXT(ImageInfo info) {
		super(ID, info);
		setPriority(true);
	}

	public PngChunkTEXT(ImageInfo info, String key, String val) {
		super(ID, info);
		setPriority(true);
		setKeyVal(key, val);
	}

	@Override
	public ChunkRaw createRawChunk() {
		if (key == null || key.trim().length() == 0)
			throw new PngjException("Text chunk key must be non empty");
		byte[] b = ChunkHelper.toBytesLatin1(key + "\0" + val);
		ChunkRaw chunk = createEmptyChunk(b.length, false);
		chunk.data = b;
		return chunk;
	}

	@Override
	public void parseFromRaw(ChunkRaw c) {
		int i;
		for (i = 0; i < c.data.length; i++)
			if (c.data[i] == 0)
				break;
		key = ChunkHelper.toStringLatin1(c.data, 0, i);
		i++;
		val = i < c.data.length ? ChunkHelper.toStringLatin1(c.data, i, c.data.length - i) : "";
	}

}
