package ar.com.hjg.pngj;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ar.com.hjg.pngj.chunks.ChunkRaw;

/**
 * Sample implementation of a very basic reader that only loads the empty chunks
 */
public class PngReaderDummy {

	protected ChunkSeqReader chunkseq;
	protected final BufferedStreamFeeder streamFeeder;
	protected List<ChunkRaw> chunks = new ArrayList<ChunkRaw>();
	protected boolean includeIdat = true;

	public PngReaderDummy(InputStream inputStream) {
		streamFeeder = new BufferedStreamFeeder(inputStream);
	}

	public PngReaderDummy(File file) {
		this(PngHelperInternal.istreamFromFile(file));
		setShouldCloseStream(true);
	}

	public void setShouldCloseStream(boolean shouldCloseStream) {
		streamFeeder.setCloseStream(shouldCloseStream);
	}

	public void readAll() {
		chunkseq = createChunkSeqReader();
		try {
			while (!chunkseq.isDone())
				streamFeeder.feed(chunkseq);
		} finally {
			chunkseq.close();
			streamFeeder.close();
		}

	}

	protected boolean shouldStoreChunkOnList(ChunkRaw raw) {
		return raw.id.equals("IDAT") && !includeIdat ? false : true;
	}

	private ChunkSeqReader createChunkSeqReader() {
		return new ChunkSeqReader() {
			@Override
			protected void postProcessChunk(ChunkReader chunkR) {
				super.postProcessChunk(chunkR);
				if (shouldStoreChunkOnList(chunkR.getChunkRaw()))
					chunks.add(chunkR.getChunkRaw());
			}

			@Override
			protected boolean shouldSkipContent(int len, String id) {
				return true;
			}
		};
	}

	public List<ChunkRaw> getChunks() {
		return chunks;
	}

	public void setIncludeIdat(boolean includeIdat) {
		this.includeIdat = includeIdat;
	}

}
