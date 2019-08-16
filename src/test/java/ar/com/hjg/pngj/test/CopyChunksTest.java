package ar.com.hjg.pngj.test;

import java.io.File;
import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.ChunkLoadBehaviour;
import ar.com.hjg.pngj.chunks.ChunkPredicate;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkTIME;

/**
 *   
 */
public class CopyChunksTest extends PngjTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    /**
     * copy all chunks
     */
    @Test
    public void testCopyAll() {
	File dest = new File(TestSupport.getTempDir(), this.getClass().getSimpleName() + ".png");
	String chunks1;
	long crc1, bytes1;
	{
	    PngReader pngr = new PngReader(TestSupport.istream(TestSupport.PNG_TEST_STRIPES));
	    pngr.setShouldCloseStream(true);
	    pngr.getChunkseq().setIncludeNonBufferedChunks(false); // to no store IDAT chunks in list, so
								   // as to compare later
	    PngHelperInternal.initCrcForTests(pngr); // to verify pixels content
	    PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
	    pngw.writeRows(pngr.readRows());
	    pngr.end();
	    pngw.end();
	    bytes1 = pngr.getChunkseq().getBytesCount();
	    chunks1 = TestSupport.showChunks(pngr.getChunksList().getChunks());
	    crc1 = PngHelperInternal.getDigest(pngr);
	}
	File dest2 = TestSupport.addSuffixToName(dest, "XX");
	dest.renameTo(dest2);// to check that it's closed
	PngReader pngr2 = new PngReader(dest2);
	pngr2.getChunkseq().setIncludeNonBufferedChunks(false);
	PngHelperInternal.initCrcForTests(pngr2);
	for (int n = 0; n < pngr2.imgInfo.rows; n++)
	    // for a change, we here read line by line
	    pngr2.readRow();
	long crc2 = PngHelperInternal.getDigest(pngr2);
	long bytes2 = pngr2.getChunkseq().getBytesCount();
	pngr2.end();
	dest2.renameTo(dest);// to check that it's closed
	// System.out.println(chunks1);
	String chunks2 = TestSupport.showChunks(pngr2.getChunksList().getChunks());
	// System.out.println(chunks2);
	TestCase.assertEquals(crc2, crc1);
	TestCase.assertEquals(chunks1, chunks2);
	TestCase.assertTrue("This could coincide... but it should be very improbable!", bytes1 != bytes2);
    }

    /**
     * copy all chunks, remove tIME
     */
    @Test
    public void testCopyRemovingOne() {
	File dest = new File(TestSupport.getTempDir(), this.getClass().getSimpleName() + ".png");
	{
	    PngReader pngr = new PngReader(TestSupport.istream(TestSupport.PNG_TEST_STRIPES));
	    pngr.setShouldCloseStream(true);
	    pngr.getChunkseq().setIncludeNonBufferedChunks(false); // to no store IDAT chunks in list, so
								   // as to compare later
	    PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	    pngw.copyChunksFrom(pngr.getChunksList(), new ChunkPredicate() {
		public boolean match(PngChunk chunk) {
		    return !chunk.id.equals(PngChunkTIME.ID); // copy unles it's time
		}
	    });
	    pngw.writeRows(pngr.readRows());
	    pngr.end();
	    // REMOVE TIME chunk (expected after IDAT)
	    pngw.end();
	    int queued = pngw.getChunksList().getQueuedChunks().size();
	    int written = pngw.getChunksList().getChunks().size();
	    TestCase.assertEquals(0, queued);
	    TestCase.assertEquals(4, written); // not including IDAT

	}
	PngReader pngr2 = new PngReader(dest);
	pngr2.getChunkseq().setIncludeNonBufferedChunks(false);
	PngHelperInternal.initCrcForTests(pngr2);
	for (int n = 0; n < pngr2.imgInfo.rows; n++)
	    // for a change, we here read line by line
	    pngr2.readRow();
	pngr2.end();
	TestCase.assertEquals("same as original, but without tIME", "IHDR[13] pHYs[9] iTXt[30] IEND[0] ",
		TestSupport.showChunks(pngr2.getChunksList().getChunks()));
    }

    /**
     * copy all chunks, change TIME (overwriting)
     */
    @Test
    public void testCopyChangingTime1() {
	File dest = new File(TestSupport.getTempDir(), this.getClass().getSimpleName() + ".png");
	int secs = TestSupport.rand.nextInt(60);
	{
	    PngReader pngr = new PngReader(TestSupport.istream(TestSupport.PNG_TEST_STRIPES));
	    pngr.setShouldCloseStream(true);
	    pngr.getChunkseq().setIncludeNonBufferedChunks(false); // to no store IDAT chunks in list, so
								   // as to compare later
	    PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	    PngChunkTIME time = new PngChunkTIME(pngw.imgInfo);
	    time.setYMDHMS(2013, 1, 2, 3, 4, secs);
	    time.setPriority(true); // write it before idat
	    pngw.queueChunk(time);
	    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
	    pngw.writeRows(pngr.readRows());
	    pngr.end();
	    pngw.end();
	}
	ChunksList cdest = TestSupport.readAllChunks(dest, false);
	PngChunkTIME time = (PngChunkTIME) cdest.getById1("tIME");
	TestCase.assertEquals("[2013, 1, 2, 3, 4, " + Integer.valueOf(secs) + "]", Arrays.toString(time.getYMDHMS()));
	TestCase.assertEquals("IHDR[13] tIME[7] pHYs[9] iTXt[30] IEND[0] ", TestSupport.showChunks(cdest.getChunks()));
    }

    /**
     * same, but not priority
     */
    @Test
    public void testCopyChangingTime2() {
	File dest = new File(TestSupport.getTempDir(), this.getClass().getSimpleName() + ".png");
	int secs = TestSupport.rand.nextInt(60);
	{
	    PngReader pngr = new PngReader(TestSupport.istream(TestSupport.PNG_TEST_STRIPES));
	    pngr.setShouldCloseStream(true);
	    pngr.getChunkseq().setIncludeNonBufferedChunks(false); // to no store IDAT chunks in list, so
								   // as to compare later
	    PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	    PngChunkTIME time = new PngChunkTIME(pngw.imgInfo);
	    time.setYMDHMS(2013, 1, 2, 3, 4, secs);
	    time.setPriority(false); // write it after idat
	    pngw.queueChunk(time);
	    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
	    pngw.writeRows(pngr.readRows());
	    pngr.end();
	    pngw.end();
	}
	ChunksList cdest = TestSupport.readAllChunks(dest, false);
	PngChunkTIME time = (PngChunkTIME) cdest.getById1("tIME");
	TestCase.assertEquals("[2013, 1, 2, 3, 4, " + Integer.valueOf(secs) + "]", Arrays.toString(time.getYMDHMS()));
	TestCase.assertEquals("IHDR[13] pHYs[9] tIME[7] iTXt[30] IEND[0] ", TestSupport.showChunks(cdest.getChunks()));
    }

    /**
     * same, but not at the end
     */
    @Test
    public void testCopyChangingTime3() {
	File dest = new File(TestSupport.getTempDir(), this.getClass().getSimpleName() + ".png");
	int secs = TestSupport.rand.nextInt(60);
	{
	    PngReader pngr = new PngReader(TestSupport.istream(TestSupport.PNG_TEST_STRIPES));
	    pngr.setShouldCloseStream(true);
	    pngr.getChunkseq().setIncludeNonBufferedChunks(false); // to no store IDAT chunks in list, so
								   // as to compare later
	    PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
	    pngw.writeRows(pngr.readRows());
	    pngr.end();
	    PngChunkTIME time = new PngChunkTIME(pngw.imgInfo);
	    time.setYMDHMS(2013, 1, 2, 3, 4, secs);
	    pngw.queueChunk(time);
	    pngw.end();
	}
	ChunksList cdest = TestSupport.readAllChunks(dest, false);
	PngChunkTIME time = (PngChunkTIME) cdest.getById1("tIME");
	TestCase.assertEquals("[2013, 1, 2, 3, 4, " + Integer.valueOf(secs) + "]", Arrays.toString(time.getYMDHMS()));
	TestCase.assertEquals("IHDR[13] pHYs[9] tIME[7] iTXt[30] IEND[0] ", TestSupport.showChunks(cdest.getChunks()));
    }

    @Test
    public void testLoadBehaviour() {
	File dest = new File(TestSupport.getTempDir(), this.getClass().getSimpleName() + ".png");
	{ // ALL CHUNKS
	    PngReader pngr = new PngReader(TestSupport.absFile(TestSupport.PNG_TEST_STRIPES));
	    pngr.setChunkLoadBehaviour(ChunkLoadBehaviour.LOAD_CHUNK_ALWAYS);
	    PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
	    pngw.writeRows(pngr.readRows());
	    pngr.end();
	    pngw.end();
	    String clist1 = TestSupport.showChunks(TestSupport.readAllChunks(dest, false).getChunks());
	    TestCase.assertEquals("IHDR[13] pHYs[9] tIME[7] iTXt[30] IEND[0] ", clist1);
	}
	{ // ONLY SAFE TO COPY
	    PngReader pngr = new PngReader(TestSupport.absFile(TestSupport.PNG_TEST_STRIPES));
	    pngr.setChunkLoadBehaviour(ChunkLoadBehaviour.LOAD_CHUNK_IF_SAFE);
	    PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
	    pngw.writeRows(pngr.readRows());
	    pngr.end();
	    pngw.end();
	    String clist1 = TestSupport.showChunks(TestSupport.readAllChunks(dest, false).getChunks());
	    TestCase.assertEquals("IHDR[13] pHYs[9] iTXt[30] IEND[0] ", clist1);
	}
	{// NO ANCILLARY CHUNK
	    PngReader pngr = new PngReader(TestSupport.absFile(TestSupport.PNG_TEST_STRIPES));
	    pngr.setChunkLoadBehaviour(ChunkLoadBehaviour.LOAD_CHUNK_NEVER);
	    PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
	    pngw.writeRows(pngr.readRows());
	    pngr.end();
	    pngw.end();
	    String clist1 = TestSupport.showChunks(TestSupport.readAllChunks(dest, false).getChunks());
	    TestCase.assertEquals("IHDR[13] IEND[0] ", clist1);
	}

    }

}
