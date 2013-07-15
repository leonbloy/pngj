package ar.com.hjg.pngj;

import java.util.Arrays;
import java.util.zip.Inflater;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class DeflatedChunkTest2{

	public static final byte[] inflated=new byte[]{0,42,43,0,44,41};
	public static final byte[] compressed=new byte[]{120, -100, 99, -48, -46, 102, -48, -47, 4, 0, 2, 5, 0, -85};
	@Test
	public void inflate() throws Exception{ // just to check that our arrays are ok
		byte[] raw = new byte[inflated.length];
		Inflater inf= new Inflater();
		inf.setInput(compressed);
		int n = inf.inflate(raw);
		TestCase.assertEquals(inflated.length, n);
		TestCase.assertEquals(Arrays.toString(raw),Arrays.toString(inflated));
	}

	@Test
	public void testLowLevel() throws Exception{
		final int rowsize=3;
		final int nrows=2;
		int row=-1;
		DeflatedChunksSet c = new DeflatedChunksSet("XXXX", rowsize, rowsize);
		c.setCallbackMode(false);
		c.processBytes(compressed, 0, 4);
		StringBuilder sb = new StringBuilder();
		while(c.isRowReady()) {
			row++;
			sb.append(c.state + " " + c.getnFedBytes() + " " +  TestSupport.showRow(c.getInflatedRow(),c.getRowFilled(),row) +", ");
			c.prepareForNextRow(row==nrows? 0:rowsize);
		}
		c.processBytes(compressed, 4, 2);
		while(c.isRowReady()) {
			row++;
			sb.append(c.state + " " + c.getnFedBytes() + " " +  TestSupport.showRow(c.getInflatedRow(),c.getRowFilled(),row) +", ");
			c.prepareForNextRow(row==nrows? 0:rowsize);
		}
		c.processBytes(compressed, 6, 2);
		while(c.isRowReady()) {
			row++;
			sb.append(c.state + " " + c.getnFedBytes() + " " +  TestSupport.showRow(c.getInflatedRow(),c.getRowFilled(),row) +", ");
			c.prepareForNextRow(row==nrows? 0:rowsize);
		}
		c.processBytes(compressed, 8, 2);
		while(c.isRowReady()) {
			row++;
			sb.append(c.state + " " + c.getnFedBytes() + " " +  TestSupport.showRow(c.getInflatedRow(),c.getRowFilled(),row) +", ");
			c.prepareForNextRow(row==nrows-1? 0:rowsize);
		}
		c.processBytes(compressed, 10, 2);
		while(c.isRowReady()) {
			row++;
			sb.append(c.state + " " + c.getnFedBytes() + " " +  TestSupport.showRow(c.getInflatedRow(),c.getRowFilled(),row) +", ");
			c.prepareForNextRow(row==nrows? 0:rowsize);
		}
		TestCase.assertEquals("READY 6 r=0[  0| 42  43], READY 10 r=1[  0| 44  41], ", sb.toString());
	}
	

	
	@Test()
	public void read1PollBad() { // file has missing IDAT
	}


	@Before
	public void setUp() {

	}

	/**
	 * Tears down the test fixture. (Called after every test case method.)
	 */
	@After
	public void tearDown() {
		
	}
	
	

}
