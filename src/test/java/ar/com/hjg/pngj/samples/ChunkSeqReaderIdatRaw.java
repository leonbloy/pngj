package ar.com.hjg.pngj.samples;

import java.io.IOException;
import java.io.OutputStream;

import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.PngjOutputException;
import ar.com.hjg.pngj.chunks.ChunkRaw;

public class ChunkSeqReaderIdatRaw extends ChunkSeqReaderOpaque {
  private final OutputStream rawOs;
  private final OutputStream idatOs;
  public boolean checkCrc = true;
  public boolean omitFilterByte = false;
  /**
   * This reader writes the chunks directly to the rawOs output, except that the IDAT stream is decompressed and writen to idatOs
   * 
   * @param is
   * @param rawOs
   * @param idatOs
   * @param rowsize
   */
  public ChunkSeqReaderIdatRaw(OutputStream rawOs, OutputStream idatOs) {
    super();
    this.rawOs = rawOs;
    this.idatOs = idatOs;
    bufferChunks = false; // we force processing for all chunks

  }

  @Override
  protected void checkSignature(byte[] buf) {
    super.checkSignature(buf);
    // we also write this to rawOs
    writeToOs(rawOs, buf, 0, buf.length);
  }

  @Override
  protected void processChunkContent(ChunkRaw chunkRaw, int offInChunk, byte[] buf, int off,
      int len) {
    if (offInChunk == 0 && rawOs != null) { // this can be called several times for a single
                                            // chunk, we do this only the first time
      chunkRaw.writeChunkHeader(rawOs);
    }
    writeToOs(rawOs, buf, off, len);
    // PngHelperInternal.debug("processChunkContent " + chunkRaw);

  }

  @Override
  protected void startNewChunk(int len, String id, long offset) {
    // PngHelperInternal.debug("startNewChunk " + id);
    super.startNewChunk(len, id, offset);
  }

  @Override
  protected void postProcessChunk(ChunkReader chunkR) {
    if (!isIdatKind(chunkR.getChunkRaw().id))
      chunkR.getChunkRaw().writeChunkCrc(rawOs);
    // PngHelperInternal.debug("postprocessing " + chunkR.getChunkRaw());
    super.postProcessChunk(chunkR);
  }

  @Override
  protected void processIdatInflatedData(byte[] inflatedRow, int off, int len) {
    if (len > 0)
      writeToOs(idatOs, inflatedRow, off, len);
  }

  @Override
  protected void processIdatDone() {
    try {
      if (idatOs != null)
        idatOs.close();
    } catch (IOException e) {
      throw new PngjOutputException(e);
    }
  }

  @Override
  protected boolean shouldCheckCrc(int len, String id) {
    return checkCrc;
  }

  protected void writeToOs(OutputStream o, byte[] buf, int off, int len) {
    try {
      if (o != null)
        o.write(buf, off, len);
    } catch (IOException e) {
      throw new PngjOutputException(e);
    }
  }

}