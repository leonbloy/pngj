package ar.com.hjg.pngj;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ar.com.hjg.pngj.ChunkReader.ChunkReaderMode;
import ar.com.hjg.pngj.chunks.ChunkRaw;

/**
 * This class is ultra dummy, skips all chunks contents and stores the chunkRaw in a list Useful to
 * read chunks structure
 */
public class ChunkSeqReaderSimple extends ChunkSeqReader {

  private List<ChunkRaw> chunks = new ArrayList<ChunkRaw>();
  private boolean skip = true;

  /**
   * @param skipAll if true, contents will be truly skipped, and CRC will not be computed
   */
  public ChunkSeqReaderSimple(boolean skipAll) {
    super(true);
    skip = skipAll;
  }

  public ChunkSeqReaderSimple() {
    this(true);
  }

  protected ChunkReader createChunkReaderForNewChunk(String id, int len, long offset, boolean skip) {
    return new ChunkReader(len, id, offset, skip ? ChunkReaderMode.SKIP : ChunkReaderMode.PROCESS) {
      @Override
      protected void chunkDone() {
        postProcessChunk(this);
      }

      @Override
      protected void processData(int offsetinChhunk, byte[] buf, int off, int len) {
        processChunkContent(getChunkRaw(), offsetinChhunk, buf, off, len);
      }
    };
  }

  protected void processChunkContent(ChunkRaw chunkRaw, int offsetinChhunk, byte[] buf, int off,
      int len) {
    // does nothing
  }

  @Override
  protected void postProcessChunk(ChunkReader chunkR) {
    super.postProcessChunk(chunkR);
    chunks.add(chunkR.getChunkRaw());
  }

  @Override
  protected boolean shouldSkipContent(int len, String id) {
    return skip;
  }

  @Override
  protected boolean isIdatKind(String id) {
    return false;
  }

  public List<ChunkRaw> getChunks() {
    return chunks;
  }

  public void feedFromInputStream(InputStream is) {
    BufferedStreamFeeder sf = new BufferedStreamFeeder(is);
    while (sf.hasMoreToFeed())
      sf.feed(this);
    close();
  }
}
