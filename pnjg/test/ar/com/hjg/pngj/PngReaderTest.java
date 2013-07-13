package ar.com.hjg.pngj;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PngReaderTest {

	StringBuilder sb=new StringBuilder();

	@Test
	public void testRead1()  {
		
		PngReaderInt pngr = new PngReaderInt(TestSupport.istream("resources/test/testg2.png"));
		for(int i=0;i<pngr.imgInfo.rows;i++) {
			ImageLine line = pngr.readRow(i);
			sb.append("r="+i).append(TestSupport.showLine(line)).append(" ");
		}
		TestCase.assertEquals("r=0[  0   1   2] r=1[112 192 105] r=2[255 238 220] ",sb.toString());
		TestCase.assertTrue(pngr.chunkseq.getIdatSet().isDone());
		pngr.end();
		TestCase.assertEquals(181,pngr.chunkseq.getBytesCount());
		String chunks = TestSupport.showChunks(pngr.getChunksList().getChunks());
		//PngHelperInternal.LOGGER.info("chunks: " + chunks);
		TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[59] IDAT[3] IDAT[17] IEND[0] ",
				TestSupport.showChunks(pngr.getChunksList().getChunks()));
	}

	@Test
	public void testReadInt()  {
		PngReaderInt pngr = new PngReaderInt(TestSupport.istream("resources/test/testg2i.png"));
		for(int i=0;i<pngr.imgInfo.rows;i++) {
			ImageLine line = pngr.readRow(i);
			sb.append("r="+i).append(TestSupport.showLine(line)).append(" ");
		}
		TestCase.assertEquals("r=0[  0   1   2] r=1[112 192 105] r=2[255 238 220] ",sb.toString());
		TestCase.assertTrue(pngr.chunkseq.getIdatSet().isDone());
		pngr.end();
		TestCase.assertEquals(183,pngr.chunkseq.getBytesCount());
		TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[70] IDAT[23] IEND[0] ",
				TestSupport.showChunks(pngr.getChunksList().getChunks()));
		
	}


    @Before
    public void setUp() {
    	sb.setLength(0);
    }

    /**
     * Tears down the test fixture. 
     * (Called after every test case method.)
     */
    @After
    public void tearDown() {
        TestSupport.cleanAll();
    }

}
