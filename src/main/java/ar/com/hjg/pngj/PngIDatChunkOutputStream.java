package ar.com.hjg.pngj;

import java.io.OutputStream;

import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkRaw;

/**
 * Outputs the stream for IDAT chunk , fragmented at fixed size (32K default).
 */
public class PngIDatChunkOutputStream extends ProgressiveOutputStream {
  private static final int SIZE_DEFAULT = 32768; // 32K rather arbitrary
  private final OutputStream outputStream;
  // for some special IDAT (fDAT) that prepend some bytes to each chunk (this
  // is not include in the size)
  private byte[] prefix = null;

  public PngIDatChunkOutputStream(OutputStream outputStream) {
    this(outputStream, 0);
  }

  public PngIDatChunkOutputStream(OutputStream outputStream, int size) {
    super(size > 0 ? size : SIZE_DEFAULT);
    this.outputStream = outputStream;
  }

  @Override
  protected final void flushBuffer(byte[] b, int len) {
    int len2 = prefix == null ? len : len + prefix.length;
    ChunkRaw c = new ChunkRaw(len2, ChunkHelper.b_IDAT, false);
    if (len == len2)
      c.data = b;
    else {

    }
    c.writeChunk(outputStream);
  }

  void setPrefix(byte[] pref) {
    if (pref == null)
      prefix = null;
    else {
      this.prefix = new byte[pref.length];
      System.arraycopy(pref, 0, prefix, 0, pref.length);
    }
  }
}
