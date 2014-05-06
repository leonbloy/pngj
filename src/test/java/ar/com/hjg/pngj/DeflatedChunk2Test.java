package ar.com.hjg.pngj;

import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import ar.com.hjg.pngj.test.PngjTest;
import ar.com.hjg.pngj.test.TestSupport;

/**
 */
public class DeflatedChunk2Test extends PngjTest {

  public static final byte[] inflated = new byte[] {0, 42, 43, 0, 44, 41};
  public static final byte[] compressed = new byte[] {120, -100, 99, -48, -46, 102, -48, -47, 4, 0,
      2, 5, 0, -85};

  public DeflatedChunk2Test() {

  }

  @Test
  public void inflate() throws DataFormatException { // just to check that our arrays are ok
    byte[] raw = new byte[inflated.length];
    Inflater inf = new Inflater();
    inf.setInput(compressed);
    int n;
    n = inf.inflate(raw);
    TestCase.assertEquals(inflated.length, n);
    TestCase.assertEquals(Arrays.toString(raw), Arrays.toString(inflated));
  }

  @Test
  public void testLowLevel() throws Exception {
    final int rowsize = 3;
    final int nrows = 2;
    int row = -1;
    DeflatedChunksSet c = new DeflatedChunksSet("XXXX", rowsize, rowsize);
    c.setCallbackMode(false);
    c.processBytes(compressed, 0, 4);
    StringBuilder sb = new StringBuilder();
    while (c.isRowReady()) {
      row++;
      sb.append(c.state + " " + c.getBytesIn() + " "
          + TestSupport.showRow(c.getInflatedRow(), c.getRowFilled(), row) + ", ");
      c.prepareForNextRow(row == nrows ? 0 : rowsize);
    }
    c.processBytes(compressed, 4, 2);
    while (c.isRowReady()) {
      row++;
      sb.append(c.state + " " + c.getBytesIn() + " "
          + TestSupport.showRow(c.getInflatedRow(), c.getRowFilled(), row) + ", ");
      c.prepareForNextRow(row == nrows ? 0 : rowsize);
    }
    c.processBytes(compressed, 6, 2);
    while (c.isRowReady()) {
      row++;
      sb.append(c.state + " " + c.getBytesIn() + " "
          + TestSupport.showRow(c.getInflatedRow(), c.getRowFilled(), row) + ", ");
      c.prepareForNextRow(row == nrows ? 0 : rowsize);
    }
    c.processBytes(compressed, 8, 2);
    while (c.isRowReady()) {
      row++;
      sb.append(c.state + " " + c.getBytesIn() + " "
          + TestSupport.showRow(c.getInflatedRow(), c.getRowFilled(), row) + ", ");
      c.prepareForNextRow(row == nrows - 1 ? 0 : rowsize);
    }
    c.processBytes(compressed, 10, 2);
    while (c.isRowReady()) {
      row++;
      sb.append(c.state + " " + c.getBytesIn() + " "
          + TestSupport.showRow(c.getInflatedRow(), c.getRowFilled(), row) + ", ");
      c.prepareForNextRow(row == nrows ? 0 : rowsize);
    }
    TestCase.assertEquals("ROW_READY 6 r=0[  0| 42  43], ROW_READY 10 r=1[  0| 44  41], ", sb.toString());
  }

  @Before
  public void setUp() {

  }

}
