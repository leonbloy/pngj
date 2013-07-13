package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngjException;

/**
 * iCCP chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11iCCP
 */
public class PngChunkICCP extends PngChunkSingle {
	public final static String ID = ChunkHelper.iCCP;

	// http://www.w3.org/TR/PNG/#11iCCP
	private String profileName;
	private byte[] compressedProfile; // copmression/decopmresion is done in getter/setter

	public PngChunkICCP(ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.BEFORE_PLTE_AND_IDAT;
	}

	@Override
	public ChunkRaw createRawChunk() {
		ChunkRaw c = createEmptyChunk(profileName.length() + compressedProfile.length + 2, true);
		System.arraycopy(ChunkHelper.toBytes(profileName), 0, c.data, 0, profileName.length());
		c.data[profileName.length()] = 0;
		c.data[profileName.length() + 1] = 0;
		System.arraycopy(compressedProfile, 0, c.data, profileName.length() + 2, compressedProfile.length);
		return c;
	}

	@Override
	public void parseFromRaw(ChunkRaw chunk) {
		int pos0 = ChunkHelper.posNullByte(chunk.data);
		profileName = new String(chunk.data, 0, pos0, PngHelperInternal.charsetLatin1);
		int comp = (chunk.data[pos0 + 1] & 0xff);
		if (comp != 0)
			throw new PngjException("bad compression for ChunkTypeICCP");
		int compdatasize = chunk.data.length - (pos0 + 2);
		compressedProfile = new byte[compdatasize];
		System.arraycopy(chunk.data, pos0 + 2, compressedProfile, 0, compdatasize);
	}

	@Override
	public PngChunk cloneForWrite(ImageInfo imgInfo) {
		PngChunkICCP other = new PngChunkICCP(imgInfo);
		other.raw = raw;
		other.profileName = profileName;
		other.compressedProfile = new byte[compressedProfile.length];
		System.arraycopy(compressedProfile, 0, other.compressedProfile, 0, compressedProfile.length); // deep
		return other;																												// copy
	}

	/**
	 * The profile should be uncompressed bytes
	 */
	public void setProfileNameAndContent(String name, byte[] profile) {
		profileName = name;
		compressedProfile = ChunkHelper.compressBytes(profile, true);
	}

	public void setProfileNameAndContent(String name, String profile) {
		setProfileNameAndContent(name, profile.getBytes(PngHelperInternal.charsetLatin1));
	}

	public String getProfileName() {
		return profileName;
	}

	/**
	 * uncompressed
	 **/
	public byte[] getProfile() {
		return ChunkHelper.compressBytes(compressedProfile, false);
	}

	public String getProfileAsString() {
		return new String(getProfile(), PngHelperInternal.charsetLatin1);
	}

}
