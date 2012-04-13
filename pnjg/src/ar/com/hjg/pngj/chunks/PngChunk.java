package ar.com.hjg.pngj.chunks;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngjException;

/**
 * Represents a PNG chunk
 * 
 * see http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
 * 
 * <p>
 * Notes for extending classes: <br>
 * -
 * 
 * @author Hernan J Gonzalez
 * 
 */
public abstract class PngChunk {

	public final String id; // 4 letters
	protected final ImageInfo imgInfo;
	public final boolean crit, pub, safe; // autocomputed

	public enum ChunkOrderingConstraint {
		NONE, BEFORE_PLTE_AND_IDAT, AFTER_PLTE_BEFORE_IDAT, BEFORE_IDAT, NA;
		public boolean mustGoBeforePLTE() {
			return this == BEFORE_PLTE_AND_IDAT;
		}

		public boolean mustGoBeforeIDAT() {
			return this == BEFORE_IDAT || this == BEFORE_PLTE_AND_IDAT || this == AFTER_PLTE_BEFORE_IDAT;
		}

		public boolean mustGoAfterPLTE() {
			return this == AFTER_PLTE_BEFORE_IDAT;
		}
	}

	/**
	 * For writing. Queued chunks with high prioirty will be written as soon as possible
	 */
	private boolean writePriority = false;
	private int chunkGroup = -1; // chunk group where it was read or writen
	private int lenori = -1; // merely informational, for read chunks

	/**
	 * This static map defines which PngChunk class correspond to which ChunkID The client can add other chunks to this
	 * map statically, before reading
	 */
	public final static Map<String, Class<? extends PngChunk>> factoryMap = new HashMap<String, Class<? extends PngChunk>>();
	static {
		factoryMap.put(ChunkHelper.IDAT, PngChunkIDAT.class);
		factoryMap.put(ChunkHelper.IHDR, PngChunkIHDR.class);
		factoryMap.put(ChunkHelper.PLTE, PngChunkPLTE.class);
		factoryMap.put(ChunkHelper.IEND, PngChunkIEND.class);
		factoryMap.put(ChunkHelper.tEXt, PngChunkTEXT.class);
		factoryMap.put(ChunkHelper.iTXt, PngChunkITXT.class);
		factoryMap.put(ChunkHelper.zTXt, PngChunkZTXT.class);
		factoryMap.put(ChunkHelper.bKGD, PngChunkBKGD.class);
		factoryMap.put(ChunkHelper.gAMA, PngChunkGAMA.class);
		factoryMap.put(ChunkHelper.pHYs, PngChunkPHYS.class);
		factoryMap.put(ChunkHelper.iCCP, PngChunkICCP.class);
		factoryMap.put(ChunkHelper.tIME, PngChunkTIME.class);
		factoryMap.put(ChunkHelper.tRNS, PngChunkTRNS.class);
		factoryMap.put(ChunkHelper.cHRM, PngChunkCHRM.class);
		factoryMap.put(ChunkHelper.sBIT, PngChunkSBIT.class);
		factoryMap.put(ChunkHelper.sRGB, PngChunkSRGB.class);
		factoryMap.put(ChunkHelper.hIST, PngChunkHIST.class);
		factoryMap.put(ChunkHelper.sPLT, PngChunkSPLT.class);
	}

	static boolean isKnown(String id) {
		return factoryMap.containsKey(id);
	}

	protected PngChunk(String id, ImageInfo imgInfo) {
		this.id = id;
		this.imgInfo = imgInfo;
		this.crit = ChunkHelper.isCritical(id);
		this.pub = ChunkHelper.isPublic(id);
		this.safe = ChunkHelper.isSafeToCopy(id);
	}

	public static PngChunk factory(ChunkRaw chunk, ImageInfo info) {
		PngChunk c = factoryFromId(ChunkHelper.toString(chunk.idbytes), info);
		c.lenori = chunk.len;
		c.parseFromRaw(chunk);
		return c;
	}

	public static PngChunk factoryFromId(String cid, ImageInfo info) {
		PngChunk chunk = null;
		try {
			Class<? extends PngChunk> cla = factoryMap.get(cid);
			if (cla != null) {
				Constructor<? extends PngChunk> constr = cla.getConstructor(ImageInfo.class);
				chunk = constr.newInstance(info);
			}
		} catch (Exception e) {
			// this can happend for unkown chunks
		}
		if (chunk == null)
			chunk = new PngChunkUNKNOWN(cid, info);
		return chunk;
	}

	protected ChunkRaw createEmptyChunk(int len, boolean alloc) {
		ChunkRaw c = new ChunkRaw(len, ChunkHelper.toBytes(id), alloc);
		return c;
	}

	@Override
	public String toString() {
		return "chunk id= " + id + " (" + lenori + ") c=" + getClass().getSimpleName();
	}

	@SuppressWarnings("unchecked")
	public static <T extends PngChunk> T cloneChunk(T chunk, ImageInfo info) {
		PngChunk cn = factoryFromId(chunk.id, info);
		if (cn.getClass() != chunk.getClass())
			throw new PngjException("bad class cloning chunk: " + cn.getClass() + " " + chunk.getClass());
		cn.cloneDataFromRead(chunk);
		return (T) cn;
	}

	void setPriority(boolean highPrioriy) {
		writePriority = highPrioriy;
	}

	boolean hasPriority() {
		return writePriority;
	}

	public int getChunkGroup() {
		return chunkGroup;
	}

	public void setChunkGroup(int chunkGroup) {
		this.chunkGroup = chunkGroup;
	}

	void write(OutputStream os) {
		ChunkRaw c = createRawChunk();
		if (c == null)
			throw new PngjException("null chunk ! creation failed for " + this);
		c.writeChunk(os);
	}

	/**
	 * Creates the phsyical chunk. This is uses when writing and must be implemented for each chunk type
	 * 
	 * @return
	 */
	public abstract ChunkRaw createRawChunk();

	/**
	 * Fill inside data from raw chunk. This is uses when reading and must be implemented for each chunk type
	 */
	public abstract void parseFromRaw(ChunkRaw c);

	/**
	 * Makes a copy of the chunk
	 * 
	 * This is used when copying chunks from a reader to a writer
	 * 
	 * It should normally be a deep copy, and after the cloning this.equals(other) should return true
	 */
	public abstract void cloneDataFromRead(PngChunk other);

	/** must be overriden - only relevant for ancillary chunks */
	public abstract boolean allowsMultiple();

	/** mustGoBeforeXX/After must be overriden - only relevant for ancillary chunks */
	public abstract ChunkOrderingConstraint getOrderingConstraint();

}
