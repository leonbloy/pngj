package ar.com.hjg.pngj;

import junit.framework.TestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *   
 */
public class ChunkSeqPngTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	public static class ChunkSeqPngCb extends ChunkSeqReaderPng {
		StringBuilder summary=new StringBuilder();
		public ChunkSeqPngCb() {
			super(true); //CB
		}

		@Override
		protected DeflatedChunksSet createIdatSet(String id) {
			IdatSet ids = new IdatSet(id,imageInfo, deinterlacer) {
				@Override
				protected int processRowCallback() {
					summary.append(TestSupport.showRow(getUnfilteredRow(), getRowFilled(), getRown()));
					return super.processRowCallback();
				}
			};
			ids.setCallbackMode(callbackMode);
			return ids;
		}
		
		
	}
	
	String stripesChunks = "IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] iTXt[30] IEND[0] ";
	
	
	/**
	 * just reads the chunks
	 */
	@Test
	public void testReadCallback1()  {
		ChunkSeqPngCb c = new ChunkSeqPngCb();
		TestSupport.feedFromStreamTest(c, "resources/test/stripes.png");
		String chunksSummary = TestSupport.showChunks(c.getChunks());
		TestCase.assertEquals(6785, c.getBytesCount());
		TestCase.assertEquals("IHDR[13] pHYs[9] IDAT[2000] IDAT[2000] IDAT[2000] IDAT[610] tIME[7] iTXt[30] IEND[0] ",
				chunksSummary);
	}

	@Test
	public void testReadCallback2()  {
		ChunkSeqPngCb c = new ChunkSeqPngCb();
		TestSupport.feedFromStreamTest(c, "resources/test/testg2.png");
		TestCase.assertEquals("r=0[  0|  0   1   2]r=1[  0|112 192 105]r=2[  0|255 238 220]", c.summary.toString());
	}
	
	private static String readRowsPoll(ChunkSeqReaderPng c,String file) {
		StringBuilder sb=new StringBuilder();
		BufferedStreamFeeder bf = new BufferedStreamFeeder(TestSupport.istream(file), TestSupport.randBufSize());
		bf.setFailIfNoFeed(true);
		while (c.firstChunksNotYetRead()) {
			bf.feed(c);
		}
		byte[][] im=new byte[c.getImageInfo().rows][c.getImageInfo().cols];
		RowInfo rowInfo = c.getIdatSet().rowinfo;
		boolean morerows=true;
		while(morerows) {
			while(! c.getIdatSet().isRowReady()) 
				bf.feed(c);
			int nbytes = c.getIdatSet().getRowFilled();
			//System.out.println("nbytes: "+nbytes + " row:"+rowInfo.rowNseq);
			if(nbytes!= rowInfo.colsSubImg+1)				
				throw new PngjInputException("bad bytes count");
			sb.append(	TestSupport.showRow(c.getIdatSet().getUnfilteredRow(), nbytes,rowInfo.rowNseq)).append(" ");
			c.getIdatSet().advanceToNextRow();
			morerows = ! c.getIdatSet().isDone();
		}
		while (!c.isDone()) {		bf.feed(c);	}	
		return sb.toString();
	}
	
	@Test
	public void testReadPoll1()  {
		ChunkSeqReaderPng c = new ChunkSeqReaderPng(false);
		String res = readRowsPoll(c, "resources/test/testg2.png");
		TestCase.assertEquals("r=0[  0|  0   1   2] r=1[  0|112 192 105] r=2[  0|255 238 220] ", 
				res);
		TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[59] IDAT[3] IDAT[17] IEND[0] ",
				TestSupport.showChunks(c.getChunks()));
		TestCase.assertEquals(181, c.getBytesCount());
	}

	@Test
	public void testReadPollInt1()  {//(interlaced
		ChunkSeqReaderPng c = new ChunkSeqReaderPng(false);
		String res = readRowsPoll(c, "resources/test/testg2i.png");
		TestCase.assertEquals("r=0[  0|  0] r=1[  0|  2] r=2[  0|255 220] r=3[  0|  1] r=4[  0|238] r=5[  0|112 192 105] ", 
				res);
		TestCase.assertEquals("IHDR[13] pHYs[9] tEXt[70] IDAT[23] IEND[0] ",
				TestSupport.showChunks(c.getChunks()));
		TestCase.assertEquals(183, c.getBytesCount());
	}
}
