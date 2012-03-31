package ar.com.hjg.pngj.chunks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ar.com.hjg.pngj.ImageInfo;

/**
 * All chunks that form an image, read or to be written
 * 
 * chunks include all chunks, but IDAT is a single pseudo chunk without data
 **/
public class ChunkList {
	// ref: http://www.w3.org/TR/PNG/#table53
	public static final int CHUNK_GROUP_0_IDHR = 0; // required - single
	public static final int CHUNK_GROUP_1_AFTERIDHR = 1; // optional - multiple
	public static final int CHUNK_GROUP_2_PLTE = 2; // optional - single
	public static final int CHUNK_GROUP_3_AFTERPLTE = 3; // optional - multple
	public static final int CHUNK_GROUP_4_IDAT = 4; // required (single pseudo chunk)
	public static final int CHUNK_GROUP_5_AFTERIDAT = 5; // optional - multple
	public static final int CHUNK_GROUP_6_END = 6; // only 1 chunk - requried

	/**
	 * All chunks, read, written (does not include IHDR, IDAT, END for written)
	 */
	private List<PngChunk> chunks = new ArrayList<PngChunk>();

	/**
	 * chunks not yet writen - does not include IHDR, IDAT, END, perhaps yes PLTE
	 */
	private Set<PngChunk> queuedChunks = new LinkedHashSet<PngChunk>();

	final ImageInfo imageInfo; // only required for writing

	public ChunkList(ImageInfo imfinfo) {
		this.imageInfo = imfinfo;
	}

	/**
	 * Adds chunk in next position. This is used when reading
	 */
	public void appendReadChunk(PngChunk chunk, int chunkGroup) {
		chunk.setChunkGroup(chunkGroup);
		chunks.add(chunk);
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
		if (currentGroup == CHUNK_GROUP_2_PLTE)
			return c.id.equals(ChunkHelper.PLTE);
		if (currentGroup % 2 == 0)
			throw new RuntimeException("?");
		int minChunkGroup, maxChunkGroup;
		if (c.mustGoBeforePLTE())
			minChunkGroup = maxChunkGroup = ChunkList.CHUNK_GROUP_1_AFTERIDHR;
		else if (c.mustGoBeforeIDAT()) {
			maxChunkGroup = ChunkList.CHUNK_GROUP_3_AFTERPLTE;
			minChunkGroup = c.mustGoAfterPLTE() ? ChunkList.CHUNK_GROUP_3_AFTERPLTE : ChunkList.CHUNK_GROUP_1_AFTERIDHR;
		} else {
			maxChunkGroup = ChunkList.CHUNK_GROUP_5_AFTERIDAT;
			minChunkGroup = ChunkList.CHUNK_GROUP_1_AFTERIDHR;
		}

		int preferred = maxChunkGroup;
		if (c.isWritePriority())
			preferred = minChunkGroup;
		if (ChunkHelper.isUnknown(c) && c.getChunkGroup() > 0)
			preferred = c.getChunkGroup();
		if (currentGroup == preferred)
			return true;
		if (currentGroup > preferred && currentGroup <= maxChunkGroup)
			return true;
		return false;
	}

	public int writeChunks(OutputStream os, int currentGroup) {
		int cont = 0;
		Iterator<PngChunk> it = queuedChunks.iterator();
		while (it.hasNext()) {
			PngChunk c = it.next();
			if (!shouldWrite(c, currentGroup))
				continue;
			c.write(os);
			chunks.add(c);
			c.setChunkGroup(currentGroup);
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
		for (PngChunk chunk : chunks)
			if (ChunkHelper.isUnknown(chunk))
				l.add(chunk.id);
		return l;
	}

	/**
	 * returns a copy of queued (for write) chunks
	 */
	public List<PngChunk> getQueuedChunks() {
		return new ArrayList<PngChunk>(queuedChunks);
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
			sb.append(chunk).append(" G=" + chunk.getChunkGroup() + "\n");
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
