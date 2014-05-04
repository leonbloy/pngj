package ar.com.hjg.pngj.test;

import java.io.File;

import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.cli.RemoveChunks;

public class RemoveChunksNgTest {

  @Test
  public void test1() throws Exception {
    File ori = TestSupport.absFile(TestSupport.PNG_TEST_STRIPES2);
    File dest = newFile();

    RemoveChunks.run(new String[] {"-kPHYS", ori.toString(), dest.toString()});
    String oric = TestSupport.getChunksSummary(ori.toString(), true);
    String destc = TestSupport.getChunksSummary(dest.toString(), false);
    TestCase
        .assertEquals(
            "IHDR[13] pHYs[9] tEXt[19] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] tEXt[18] IEND[0] ",
            oric);
    TestCase.assertEquals("IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] IEND[0] ",
        destc);
  }

  @Test
  public void test3() throws Exception {
    // will work even with bad CRC
    File ori = TestSupport.absFile(TestSupport.PNG_TEST_BADCRC);
    File dest = newFile();

    RemoveChunks.run(new String[] {"-q", "-f", "-rTIME,PHYS", ori.toString(), dest.toString()});
    String oric = TestSupport.getChunksSummary(ori.toString(), true);
    String destc = TestSupport.getChunksSummary(dest.toString(), true);
    TestCase.assertEquals("IHDR[13] pHYs[9] IDAT[65] IEND[0] ", oric);
    TestCase.assertEquals("IHDR[13] IDAT[65] IEND[0] ", destc);
  }

  @Test(expected = PngjException.class)
  public void test4() throws Exception {
    // NON FAST MODE; CHECKS CRC
    File ori = TestSupport.absFile(TestSupport.PNG_TEST_BADCRC);
    File dest = newFile();

    RemoveChunks.run(new String[] {"-q", "-rTIME,PHYS", ori.toString(), dest.toString()});
  }

  private File newFile() {
    File f = new File(TestSupport.getTempDir(), "RemoveChunksNgTest.png");
    f.deleteOnExit();
    return f;
  }
}
