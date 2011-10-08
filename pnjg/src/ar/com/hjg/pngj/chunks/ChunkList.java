package ar.com.hjg.pngj.chunks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All chunks that form an image, read or to be written
 * 
 * chunks include all chunks, but IDAT is a single pseudo chunk without data
 **/
public class ChunkList {
	// ref: http://www.w3.org/TR/PNG/#table53
	// 
	private List<PngChunk> chunks = new ArrayList<PngChunk>();
	// for each chunkid, says the position(s) in list
	private Map<String, List<Integer>> positions = new HashMap<String, List<Integer>>();
	private boolean positionsDirty = true; // positions need recalc

	public ChunkList() {
	}

	public void check() {
		// checks that the order is right
		// TODO: check
	}

	private void recalcPositions() {
		positions.clear();
		for (int i = 0; i < chunks.size(); i++) {
			String chunkid = chunks.get(i).id;
			if (!positions.containsKey(chunkid))
				positions.put(chunkid, new ArrayList<Integer>());
			positions.get(chunkid).add(i);
		}
		positionsDirty = false;
	}

	/**
	 * Adds chunk in next position. 
	 * This is used when reading
	 */
	public void appendChunk(PngChunk chunk) {
		chunks.add(chunk);
		positionsDirty = true;
	}

	public void insertChunk(PngChunk chunk, int afterPos) {
		chunks.add(afterPos + 1, chunk);
		positionsDirty = true;
	}

	public void insertChunk(PngChunk chunk) {
		// will be inserted according to type
		// TODO
		positionsDirty = true;
	}

	public void removeChunk(PngChunk chunk) {
		chunks.remove(chunk);
		positionsDirty = true;
	}

	public void removeChunk(int pos) {
		chunks.remove(pos);
		positionsDirty = true;
	}

	public List<PngChunk> getChunks() {
		if (positionsDirty)
			recalcPositions();
		return chunks;
	}

	public PngChunk getChunk(int i) {
		if (i < 0)
			return null;
		else
			return chunks.get(i);
	}

	public int nOcurrences(String chunkId) {
		if (positionsDirty)
			recalcPositions();
		return positions.containsKey(chunkId) ? positions.get(chunkId).size() : 0;
	}

	/**
	 * 
	 * @param chunkId
	 * @return -1 if not found
	 */
	public int firstOcurrence(String chunkId) {
		if (positionsDirty)
			recalcPositions();
		return positions.containsKey(chunkId) ? positions.get(chunkId).get(0) : -1;
	}

	@SuppressWarnings("unchecked")
	public List<Integer> allOcurrences(String chunkId) {
		if (positionsDirty)
			recalcPositions();
		return positions.containsKey(chunkId) ? positions.get(chunkId)
				: (List<Integer>) Collections.EMPTY_LIST;
	}

	public int positionIDAT() {
		return firstOcurrence(ChunkHelper.IDAT_TEXT);
	}

	public int positionIEND() {
		return firstOcurrence(ChunkHelper.IEND_TEXT);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (PngChunk chunk : chunks) {
			sb.append(chunk).append("\n");
		}
		return sb.toString();
	}
	
	/**
	 * @return dpi, -1 if not set
	 */
	public double getPHYSdpi() { // nan if not set
		int i=firstOcurrence(ChunkHelper.pHYs_TEXT);
		if(i<0) return -1.0;
		PngChunkPHYS chunk = (PngChunkPHYS)getChunk(i);
		return chunk.getAsDpi();
	}

}
