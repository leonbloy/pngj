package ar.com.hjg.pngj.chunks;

public interface ChunkPredicate {
	boolean match(PngChunk c);
}
