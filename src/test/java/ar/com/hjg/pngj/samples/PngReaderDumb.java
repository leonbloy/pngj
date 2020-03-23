package ar.com.hjg.pngj.samples;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ar.com.hjg.pngj.BufferedStreamFeeder;
import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.ChunkSeqReader;
import ar.com.hjg.pngj.ChunkSeqSkipping;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.PngChunkIHDR;

/**
 * Sample implementation of a very basic reader that only loads the empty chunks
 * (except the IHDR). The IDAT are optional.
 */
public class PngReaderDumb {

	protected ChunkSeqReader chunkseq;
	protected final BufferedStreamFeeder streamFeeder;
	protected List<ChunkRaw> chunks = new ArrayList<ChunkRaw>();
	protected boolean includeIdat = true;
	protected ImageInfo imgInfo;
	private boolean interlaced = false;

	public PngReaderDumb(InputStream inputStream) {
		streamFeeder = new BufferedStreamFeeder(inputStream);
	}

	public PngReaderDumb(File file) {
		this(PngHelperInternal.istreamFromFile(file));
	}

	public void readAll() {
		chunkseq = createChunkSeqReader();
		try {
			streamFeeder.feedAll(chunkseq);
		} finally {
			close();
		}
	}

	protected ChunkSeqReader createChunkSeqReader() {
		ChunkSeqSkipping cs = new ChunkSeqSkipping(false) { // don't check CRC
			@Override
			protected void postProcessChunk(ChunkReader chunkR) {
				super.postProcessChunk(chunkR);
				if (!(chunkR.getChunkRaw().id.equals(ChunkHelper.IDAT) && !includeIdat))
					chunks.add(chunkR.getChunkRaw());
			}

			@Override
			protected void startNewChunk(int len, String id, long offset) {
				super.startNewChunk(len, id, offset);
				//
			}

			@Override
			protected boolean shouldSkipContent(int len, String id) {
				return !id.equals(ChunkHelper.IHDR); // we skip everything
			}
		};
		return cs;
	}

	public ImageInfo getImageInfo() {
		if (imgInfo == null) {
			if (chunks.size() > 0) {
				PngChunkIHDR ihdr = new PngChunkIHDR(null);
				ihdr.parseFromRaw(chunks.get(0));
				imgInfo = ihdr.createImageInfo();
				interlaced = ihdr.isInterlaced();
			}
		}
		return imgInfo;
	}

	public ChunkSeqReader getChunkseq() {
		return chunkseq;
	}

	public List<ChunkRaw> getChunks() {
		return chunks;
	}

	public void setIncludeIdat(boolean includeIdat) {
		this.includeIdat = includeIdat;
	}

	protected boolean shouldStoreChunkOnList(ChunkRaw raw) {
		return raw.id.equals("IDAT") && !includeIdat ? false : true;
	}

	public void setShouldCloseStream(boolean shouldCloseStream) {
		streamFeeder.setCloseStream(shouldCloseStream);
	}

	public void close() {
		if (chunkseq != null)
			chunkseq.close();
		streamFeeder.close();
	}

	public String toStringCompact() {
		return imgInfo.toStringBrief() + (interlaced ? "i" : "");
	}

}
