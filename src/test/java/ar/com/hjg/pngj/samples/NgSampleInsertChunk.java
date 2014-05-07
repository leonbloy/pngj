package ar.com.hjg.pngj.samples;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import ar.com.hjg.pngj.BufferedStreamFeeder;
import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.ChunkSeqBuffering;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.chunks.PngChunkTEXT;

/**
 * This example shows how to insert a textual chunk efficiently, without decoding and reencoding the pixels, using just a ChunkSeqBuffering (This does not check for duplicated
 * textual chunks)
 * 
 * This could be made even more efficiently (not buffering the chunks to memory), by using a ChunkSeqSkipping, see for example class RemoveChunks.
 */
public class NgSampleInsertChunk {

  private final ChunkSeqBuffering cs;
  private BufferedStreamFeeder streamFeeder;
  private OutputStream os;

  private boolean inserted;
  private boolean beforeIdat;
  private String text;

  public NgSampleInsertChunk(InputStream inputStream) {
    streamFeeder = new BufferedStreamFeeder(inputStream);
    cs = new ChunkSeqBuffering() {
      @Override
      protected void postProcessChunk(ChunkReader chunkR) {
        super.postProcessChunk(chunkR);
        chunkR.getChunkRaw().writeChunk(os); // send the chunk straight to the os
      }

      @Override
      protected void startNewChunk(int len, String id, long offset) {
        super.startNewChunk(len, id, offset);
        insertMyChunkEventually(id); // insert the text chunk if appropiate
      }
    };
    cs.setCheckCrc(false);
  }

  private void insertMyChunkEventually(String nextChukn) {
    if (inserted)
      return;
    // this logic puts the text chunk just before first IDAT or just after it
    if ((beforeIdat && nextChukn.equals("IDAT")) || (nextChukn.equals("IEND") && !beforeIdat)) { // insert
      PngChunkTEXT t = new PngChunkTEXT(null);
      t.setKeyVal("zzz", text);
      t.createRawChunk().writeChunk(os);
      inserted = true;
    }

  }

  public void copyInsertingText(OutputStream os, String text, boolean beforeIdat) {
    this.os = os;
    this.inserted = false;
    this.beforeIdat = beforeIdat;
    this.text = text;
    PngHelperInternal.writeBytes(os, PngHelperInternal.getPngIdSignature());
    while (streamFeeder.hasMoreToFeed())
      // async feeding
      streamFeeder.feed(cs);
  }

  public static void insert(String orig, String to, boolean beforeIdat) throws Exception {
    NgSampleInsertChunk c = new NgSampleInsertChunk(new FileInputStream(orig));
    FileOutputStream oss = new FileOutputStream(to);
    c.copyInsertingText(oss, "Hi!!! after idat", false);
    oss.close();
    System.out.println("inserted text " + orig + " -> " + to + " "
        + (beforeIdat ? " before idat" : "after idat"));

  }

  public static void main(String[] args) throws Exception {
    insert("C:/temp/x.png", "C:/temp/zt1.png", false);
    insert("C:/temp/x.png", "C:/temp/zt2.png", true);

  }
}
