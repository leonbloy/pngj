package ar.com.hjg.pngj.chunks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;
import ar.com.hjg.pngj.PngjOutputException;

/**
 * All chunks that form an image, read or to be written
 * 
 * chunks include all chunks, but IDAT is a single pseudo chunk without data
 **/
public class ChunkList {
	// ref: http://www.w3.org/TR/PNG/#table53
	public static final int CHUCK_GROUP_0_IDHR = 0; // required - single
	public static final int CHUCK_GROUP_1_AFTERIDHR = 1; // optional - multiple
	public static final int CHUCK_GROUP_2_PLTE = 2; // optional - single
	public static final int CHUCK_GROUP_3_AFTERPLTE = 3; // optional - multple
	public static final int CHUCK_GROUP_4_IDAT = 4; // required (single pseudo chunk)
	public static final int CHUCK_GROUP_5_AFTERIDAT = 5; // optional - multple
	public static final int CHUCK_GROUP_6_END = 6; // only 1 chunk - requried

	/**
	 * All chunks, read, written (does not include IHDR, IDAT, END for written)
	 */
	private List<PngChunk> chunks = new ArrayList<PngChunk>();

	/**
	 * chunks not yet writen - does not include IHDR, IDAT, END, perhaps yes PLTE
	 */
	private List<PngChunk> queuedChunks = new ArrayList<PngChunk>();

	/**
	 * for read chunks, or already written, the corresponding CHUNK_GROUP
	 */
	private Map<PngChunk, Integer> chunksGroup = new HashMap<PngChunk, Integer>();

	final ImageInfo imageInfo; // only required for writing

	public ChunkList(ImageInfo imfinfo) {
		this.imageInfo = imfinfo;
	}

	/**
	 * Adds chunk in next position. This is used when reading
	 */
	public void appendReadChunk(PngChunk chunk, int chunkGroup) {
		chunks.add(chunk);
		chunksGroup.put(chunk, chunkGroup);
	}

	public List<PngChunk> getById(String id, boolean includeQueued, boolean includeProcessed) {
		List<PngChunk> list = new ArrayList<PngChunk>();
		if (includeQueued)
			for (PngChunk c : queuedChunks)
				if (c.id.equals(id))
					list.add(c);
		if (includeProcessed)
			for (PngChunk c : chunks)
				if (c.id.equals(id))
					list.add(c);
		return list;
	}

	/**
	 * Remove Chunk: only from queued
	 */
	public boolean remove(PngChunk c) {
		return queuedChunks.remove(c);
	}

	/**
	 * add chunk to write queue
	 */
	public void queueChunk(PngChunk chunk, boolean replace, boolean priority) {
		chunk.setPriority(priority);
		if (replace) {
			List<PngChunk> current = getById(chunk.id, true, false);
			for (PngChunk chunk2 : current) 
				remove(chunk2);
		}
		queuedChunks.add(chunk);
	}

	/**
	 * this should be called only for ancillary chunks and PLTE (groups 1 - 3 - 5)
	 **/
	private static boolean shouldWrite(PngChunk c, int currentGroup) {
		if(currentGroup == CHUCK_GROUP_2_PLTE)
			return c.id.equals(ChunkHelper.PLTE);
		if(currentGroup % 2==0) throw new RuntimeException("?");
		int preferred = c.isWritePriority() ? c.minChunkGroup : c.maxChunkGroup;
		if (currentGroup == preferred)
			return true;
		if (currentGroup > preferred && currentGroup <= c.maxChunkGroup)
			return true;
		return false;
	}

	public int writeChunks(int currentGroup, OutputStream os) {
		int cont = 0;
		Iterator<PngChunk> it = queuedChunks.iterator();
		while (it.hasNext()) {
			PngChunk c = it.next();
			if (!shouldWrite(c, currentGroup))
				continue;
			c.write(os);
			chunks.add(c);
			chunksGroup.put(c, currentGroup);
			it.remove();
			cont++;
		}
		return cont;
	}

	/**
	 * returns a copy of processed (read or writen) chunks
	 */
	public List<PngChunk> getChunks() {
		return new ArrayList<PngChunk>(chunks);
	}
	
	
	public List<String> getChunksUnkown() {
		List<String> l = new ArrayList<String>();
		for(PngChunk chunk: chunks)
			if(chunk instanceof PngChunkUNKNOWN)
				l.add(chunk.id);
		return l;
	}

	/**
	 * returns a copy of queued (for write) chunks
	 */
	public List<PngChunk> getQueuedChunks() {
		return new ArrayList<PngChunk>(queuedChunks);
	}

	/** 
	 * returns the group in which a chunk was read or written
	 * (-1 if not found)
	 */
	public int getChunkGroup(PngChunk c) {
		Integer g = chunksGroup.get(c);
		return g == null ? -1 : g.intValue();
	}

	
	public String toString() {
		return "ChunkList: processed: " + chunks.size() + " queue: " + queuedChunks.size();
	}

	/**
	 * for debugging
	 */
	public String toStringFull() {
		StringBuilder sb = new StringBuilder(toString());
		sb.append("\n Processed:\n");
		for (PngChunk chunk : chunks) {
			sb.append(chunk).append(" G=" + chunksGroup.get(chunk) + "\n");
		}
		if (!queuedChunks.isEmpty()) {
			sb.append(" Queued:\n");
			for (PngChunk chunk : chunks) {
				sb.append(chunk).append("\n");
			}

		}
		return sb.toString();
	}

	

}
