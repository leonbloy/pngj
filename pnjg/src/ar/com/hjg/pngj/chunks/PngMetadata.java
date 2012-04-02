package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.PngHelper;
import ar.com.hjg.pngj.PngjException;

/**
 * We consider "image metadata" every info inside the image except for the most basic image info (IHDR chunk - ImageInfo
 * class) and the pixels values.
 * 
 * This includes the palette (if present) and all the ancillary chunks
 * 
 * This class provides a wrapper over the collection of chunks of a image (read or to write) and provides some high
 * level methods to access them
 * 
 */
public class PngMetadata {
	private final ChunkList chunks;
	private final boolean readonly;

	public PngMetadata(ChunkList chunks, boolean readonly) {
		this.chunks = chunks;
		this.readonly = readonly;
	}

	public boolean setChunk(PngChunk c, boolean overwriteIfPresent) {
		if (readonly)
			throw new PngjException("cannot set chunk : readonly metadata");
		return chunks.setChunk(c, overwriteIfPresent);
	}

	public PngChunk getChunk1(String id) {
		return chunks.getChunk1(id);
	}

	public PngChunk getChunk1(String id, String innerid, boolean failIfMultiple) {
		return chunks.getChunk1(id, innerid, failIfMultiple);
	}

	// ///// high level utility methods follow ////////////

	// //////////// DPI

	/** returns -1 if not found or dimension unknown */
	public double[] getDpi() {
		PngChunk c = getChunk1(ChunkHelper.pHYs, null, true);
		if (c == null)
			return new double[] { -1, -1 };
		else
			return ((PngChunkPHYS) c).getAsDpi2();
	}

	public void setDpi(double x) {
		setDpi(x, x);
	}

	public void setDpi(double x, double y) {
		PngChunkPHYS c = new PngChunkPHYS(chunks.imageInfo);
		c.setAsDpi2(x, y);
		setChunk(c, true);
	}

	// //////////// TIME

	public void setTimeNow(int secsAgo) {
		PngChunkTIME c = new PngChunkTIME(chunks.imageInfo);
		c.setNow(secsAgo);
		setChunk(c, true);
	}

	public void setTimeYMDHMS(int yearx, int monx, int dayx, int hourx, int minx, int secx) {
		PngChunkTIME c = new PngChunkTIME(chunks.imageInfo);
		c.setYMDHMS(yearx, monx, dayx, hourx, minx, secx);
		setChunk(c, true);
	}

	public String getTimeAsString() {
		PngChunk c = getChunk1(ChunkHelper.tIME, null, true);
		return c != null ? ((PngChunkTIME) c).getAsString() : "";
	}

	// //////////// TEXT

	public void setText(String k, String val, boolean useLatin1, boolean compress) {
		if (compress && !useLatin1)
			throw new PngjException("cannot compress non latin text");
		PngChunkTextVar c;
		if (useLatin1) {
			if (compress) {
				c = new PngChunkZTXT(chunks.imageInfo);
			} else {
				c = new PngChunkTEXT(chunks.imageInfo);
			}
		} else {
			c = new PngChunkITXT(chunks.imageInfo);
			((PngChunkITXT) c).setLangtag(k); // we use the same orig tag (this is not quite right)
		}
		c.setKeyVal(k, val);
		setChunk(c, true);
	}

	public void setText(String k, String val) {
		setText(k, val, false, val.length() > 400);
	}

	/** tries all text chunks - returns null if not found */
	public String getTxtForKey(String k) {
		PngChunk c = getChunk1(ChunkHelper.tEXt, k, true);
		if (c == null)
			c = getChunk1(ChunkHelper.zTXt, k, true);
		if (c == null)
			c = getChunk1(ChunkHelper.iTXt, k, true);
		return c != null ? ((PngChunkTextVar) c).getVal() : null;
	}

}
