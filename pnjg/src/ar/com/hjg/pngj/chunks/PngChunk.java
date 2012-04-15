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
 * New classes should extend PngChunkSingle or PngChunkMultiple
 */
public abstract class PngChunk {

	/**
	 * Chunk id: 4 letters
	 */
	public final String id;
	/**
	 * autocomputed at creation time
	 */
	public final boolean crit, pub, safe;
	protected final ImageInfo imgInfo;

	public enum ChunkOrderingConstraint {
		/**
		 * no ordering constraint
		 */
		NONE,
		/**
		 * Must go before PLTE (and hence, also before IDAT)
		 */
		BEFORE_PLTE_AND_IDAT,
		/**
		 * Must go after PLTE but before IDAT
		 */
		AFTER_PLTE_BEFORE_IDAT,
		/**
		 * Must before IDAT (before or after PLTE)
		 */
		BEFORE_IDAT,
		/**
		 * Not apply
		 */
		NA;

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
	 * For writing. Queued chunks with high priority will be written as soon as possible
	 */
	private boolean writePriority = false;

	private int chunkGroup = -1; // chunk group where it was read or writen
	private int lenori = -1; // merely informational, for read chunks

	/**
	 * This static map defines which PngChunk class correspond to which ChunkID
	 * <p>
	 * The client can add other chunks to this map statically, before reading an image, calling
	 * PngChunk.factoryRegister(id,class)
	 */
	private final static Map<String, Class<? extends PngChunk>> factoryMap = new HashMap<String, Class<? extends PngChunk>>();
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
		// extended
		factoryMap.put(PngChunkOFFS.ID, PngChunkOFFS.class);
		factoryMap.put(PngChunkSTER.ID, PngChunkSTER.class);
	}

	/**
	 * to be called by user code that wants to add some chunks to the factory
	 */
	public static void factoryRegister(String chunkId, Class<? extends PngChunk> chunkClass) {
		factoryMap.put(chunkId, chunkClass);
	}

	/**
	 * A chunks "is known" if we recognize its class, according with <code>factoryMap</code>
	 * 
	 * This is not necessarily the same as being "STANDARD"
	 * 
	 * @param id
	 *            chunkid
	 * @return true or false
	 */
	public static boolean isKnown(String id) {
		return factoryMap.containsKey(id);
	}

	protected PngChunk(String id, ImageInfo imgInfo) {
		this.id = id;
		this.imgInfo = imgInfo;
		this.crit = ChunkHelper.isCritical(id);
		this.pub = ChunkHelper.isPublic(id);
		this.safe = ChunkHelper.isSafeToCopy(id);
	}

	/**
	 * This factory creates the corresponding chunk and parses the raw chunk. This is used when reading.
	 */
	public static PngChunk factory(ChunkRaw chunk, ImageInfo info) {
		PngChunk c = factoryFromId(ChunkHelper.toString(chunk.idbytes), info);
		c.lenori = chunk.len;
		c.parseFromRaw(chunk);
		return c;
	}

	/**
	 * Creates one new blank chunk of the corresponding type, according to factoryMap (PngChunkUNKNOWN if not known)
	 */
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

	/**
	 * Makes a clone (deep copy) calling <tt>cloneDataFromRead</tt>
	 */
	@SuppressWarnings("unchecked")
	public static <T extends PngChunk> T cloneChunk(T chunk, ImageInfo info) {
		PngChunk cn = factoryFromId(chunk.id, info);
		if (cn.getClass() != chunk.getClass())
			throw new PngjException("bad class cloning chunk: " + cn.getClass() + " " + chunk.getClass());
		cn.cloneDataFromRead(chunk);
		return (T) cn;
	}

	/**
	 * For writing. Queued chunks with high priority will be written as soon as possible
	 */
	void setPriority(boolean highPrioriy) {
		writePriority = highPrioriy;
	}

	boolean hasPriority() {
		return writePriority;
	}

	/**
	 * In which "chunkGroup" (see ChunkList object for definition) this was read or written. -1 if not read or written
	 * (eg, queued)
	 */
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
	 * @return A newly allocated and filled raw chunk
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

	/** only relevant for ancillary chunks */
	public abstract boolean allowsMultiple();

	/**
	 * must be overriden - only relevant for ancillary chunks
	 */
	public abstract ChunkOrderingConstraint getOrderingConstraint();

}
