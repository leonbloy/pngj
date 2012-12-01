package ar.com.hjg.pngj.chunks;

/**
 * Decides if another chunk "matches", according to some criterion
 */
public interface ChunkPredicate {
	/**
	 * The other chunk matches with this one
	 * 
	 * @param chunk
	 * @return
	 */
	boolean match(PngChunk chunk);
}
