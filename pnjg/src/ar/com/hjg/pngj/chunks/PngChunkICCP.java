package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;

/*
 */
public class PngChunkICCP extends PngChunk {
	// http://www.w3.org/TR/PNG/#11iCCP
	private String profile;
	// TODO: uncompress
	private byte[] compressedProfile;

	public PngChunkICCP(ImageInfo info) {
		super(ChunkHelper.iCCP_TEXT, info);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = createEmptyChunk(profile.length() + compressedProfile.length + 2, true);
		System.arraycopy(ChunkHelper.toBytes(profile), 0, c.data, 0, profile.length());
		c.data[profile.length()] = 0;
		c.data[profile.length() + 1] = 0;
		System.arraycopy(compressedProfile, 0, c.data, profile.length() + 2,
				compressedProfile.length);
		return c;
	}

	@Override
	public void parseFromChunk(ChunkRaw chunk) {
		int pos0 = ChunkHelper.posNullByte(chunk.data);
		profile = new String(chunk.data, 0, pos0, PngHelper.charsetLatin1);
		int comp = (chunk.data[pos0 + 1] & 0xff);
		if (comp != 0)
			throw new RuntimeException("bad compression for ChunkTypeICCP");
		int compdatasize = chunk.data.length - (pos0 + 2);
		compressedProfile = new byte[compdatasize];
		System.arraycopy(chunk.data, pos0 + 2, compressedProfile, 0, compdatasize);
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkICCP otherx = (PngChunkICCP) other;
		profile = otherx.profile;
		compressedProfile = otherx.compressedProfile; // non deep
	}
}
