package ar.com.hjg.pngj.chunks;

import java.io.OutputStream;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngjException;

// see http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
public abstract class PngChunk {
	public final String id; // 4 letters
	public final boolean crit, pub, safe, known, beforeIDAT, beforePLTE;
	private int lenori = -1; // merely informational, for read chunks
	// 0:queued ; 1: queued prioritary; 2: dont write yet; 3: already written
	private int writeStatus = 0;
	protected final ImageInfo imgInfo;

	protected PngChunk(String id, ImageInfo imgInfo) {
		this.id = id;
		this.imgInfo = imgInfo;
		this.crit = ChunkHelper.isCritical(id);
		this.pub = ChunkHelper.isPublic(id);
		this.safe = ChunkHelper.isSafeToCopy(id);
		this.known = ChunkHelper.isKnown(id);
		// beforeIDAT=true: MUST go before IDATA
		this.beforeIDAT = ChunkHelper.beforeIDAT(id);
		// beforePLTE=true: MUST go before PLTE (if present)
		this.beforePLTE = ChunkHelper.beforePLTE(id);
	}

	public abstract ChunkRaw createChunk();

	public abstract void parseFromChunk(ChunkRaw c);

	// override to make deep copy from read data to write
	public abstract void cloneDataFromRead(PngChunk other);

	@SuppressWarnings("unchecked")
	public static <T extends PngChunk> T cloneChunk(T chunk, ImageInfo info) {
		PngChunk cn = factoryFromId(chunk.id, info);
		if (cn.getClass() != chunk.getClass())
			throw new PngjException("bad class cloning chunk: " + cn.getClass() + " "
					+ chunk.getClass());
		cn.cloneDataFromRead(chunk);
		return (T) cn;
	}

	public static PngChunk factory(ChunkRaw chunk, ImageInfo info) {
		PngChunk c = factoryFromId(ChunkHelper.toString(chunk.idbytes), info);
		c.lenori = chunk.len;
		c.parseFromChunk(chunk);
		return c;
	}

	public static PngChunk factoryFromId(String cid, ImageInfo info) {
		PngChunk ctype = null;
		if (cid.equals(ChunkHelper.IDAT_TEXT))
			ctype = new PngChunkIDAT(info);
		else if (cid.equals(ChunkHelper.IHDR_TEXT))
			ctype = new PngChunkIHDR(info);
		else if (cid.equals(ChunkHelper.PLTE_TEXT))
			ctype = new PngChunkPLTE(info);
		else if (cid.equals(ChunkHelper.IEND_TEXT))
			ctype = new PngChunkIEND(info);
		else if (cid.equals(ChunkHelper.gAMA_TEXT))
			ctype = new PngChunkGAMA(info);
		else if (cid.equals(ChunkHelper.tEXt_TEXT))
			ctype = new PngChunkTEXT(info);
		else if (cid.equals(ChunkHelper.iTXt_TEXT))
			ctype = new PngChunkOTHER(cid, info);// new ChunkTypeITXT(info);
		else if (cid.equals(ChunkHelper.zTXt_TEXT))
			ctype = new PngChunkOTHER(cid, info);// new ChunkTypeZTXT(info);
		else if (cid.equals(ChunkHelper.pHYs_TEXT))
			ctype = new PngChunkPHYS(info);
		else if (cid.equals(ChunkHelper.bKGD_TEXT))
			ctype = new PngChunkBKGD(info);
		else if (cid.equals(ChunkHelper.iCCP_TEXT))
			ctype = new PngChunkICCP(info);
		else if (cid.equals(ChunkHelper.tIME_TEXT))
			ctype = new PngChunkTIME(info);
		else if (cid.equals(ChunkHelper.tRNS_TEXT))
			ctype = new PngChunkTRNS(info);
		else if (cid.equals(ChunkHelper.cHRM_TEXT))
			ctype = new PngChunkCHRM(info);
		else if (cid.equals(ChunkHelper.sBIT_TEXT))
			ctype = new PngChunkSBIT(info);
		else if (cid.equals(ChunkHelper.sRGB_TEXT))
			ctype = new PngChunkSRGB(info);
		else
			ctype = new PngChunkOTHER(cid, info);
		return ctype;
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
	 * should be called for ancillary chunks only Our write order is defined as
	 * (0:IDHR) 1: after IDHR (2:PLTE if present) 3: before IDAT (4:IDAT) 5: after
	 * IDAT (6:END)
	 */
	public int writeOrder() {
		if (id.equals(ChunkHelper.IHDR))
			return 0;
		if (id.equals(ChunkHelper.PLTE))
			return 2;
		if (id.equals(ChunkHelper.IDAT))
			return 4;
		if (id.equals(ChunkHelper.IEND))
			return 6;
		if (ChunkHelper.beforePLTE(id))
			return 1;
		if (ChunkHelper.beforeIDAT(id))
			return 3;
		else
			return 5;
	}

	public int getWriteStatus() {
		return writeStatus;
	}

	public void setWriteStatus(int writeStatus) {
		this.writeStatus = writeStatus;
	}

	public void writeAndMarkAsWrite(OutputStream os) {
		if (getWriteStatus() >= 2)
			throw new RuntimeException("bad write status");
		ChunkRaw c = createChunk();
		if (c != null)
			c.writeChunk(os);
		else
			System.err.println("null chunk ! for " + this);
		setWriteStatus(3);
	}
}
