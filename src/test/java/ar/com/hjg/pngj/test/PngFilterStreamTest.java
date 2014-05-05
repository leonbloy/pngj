package ar.com.hjg.pngj.test;

import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;

import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.PngReaderFilter;
import ar.com.hjg.pngj.chunks.PngChunk;

/** this shows how to use the Callback mode */
public class PngFilterStreamTest {

  @Test
  public void testBufferedImage() throws Exception {
    PngReaderFilter reader = new PngReaderFilter(TestSupport.istream("test/stripes.png"));
    BufferedImage image1 = ImageIO.read(reader);
    reader.readUntilEndAndClose();
    List<PngChunk> chunks = reader.getChunksList();
    /*
     * System.out.println(chunks.size()); System.out.println(reader.getChunksList());
     * System.out.println(TestSupport.showChunks(reader.getChunksList()));
     * System.out.println(chunks.size());
     */

    TestCase.assertTrue(reader.getChunkseq().isDone());
    TestCase.assertEquals("IHDR[13] pHYs[9] tIME[7] iTXt[30] IEND[0] ",
        TestSupport.showChunks(chunks));
    TestCase.assertEquals(reader.getChunkseq().getImageInfo().cols, image1.getWidth());
  }

}
