package ar.com.hjg.pngj;

public interface IChunkProcessor {
	
	public void processChunkEnd(ChunkReader chunkReader);

	public void processChunkStart(ChunkReader chunkReader);

	public boolean shouldCheckCrc(int len, String id);

	public boolean shouldSkipContent(int len, String id);

	public boolean isIdatKind(String id);

	public ChunkReaderDeflatedSet createNewIdatSetReader(String id);
}