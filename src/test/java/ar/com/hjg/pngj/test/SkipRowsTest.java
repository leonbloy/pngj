package ar.com.hjg.pngj.test;

import java.io.File;
import java.util.Random;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;

public class SkipRowsTest extends PngjTest {
    private final Logger LOGGER = Logger.getLogger(getClass().getName());
    
    boolean verbose = false;
    private int cols = 1300, rows = 1300;

    /*
     * several ways of reading opnly metatada
     */
    @Test
    public void testSkipRows() {
	// creates big image
	ImageInfo imi = new ImageInfo(cols, rows, 8, false);
	String suffix = "raw";
	File file = TestSupport.getTmpFile(suffix);
	LOGGER.info(file.toString());
	{
	    long t0 = System.currentTimeMillis();
	    PngWriter png = TestSupport.prepareFileTmp(file, imi);
	    IImageLine[] lines = new IImageLine[3];
	    lines[0] = TestSupport.generateNoiseLine(imi);
	    lines[1] = TestSupport.generateNoiseLine(imi);
	    lines[2] = TestSupport.generateNoiseLine(imi);
	    png.setFilterType(FilterType.FILTER_NONE);
	    // add two text chunks, one before IDAT, onther after
	    png.getMetadata().setText("chunk1", "test1").setPriority(true);
	    png.getMetadata().setText("chunk2", "test2").setPriority(false);
	    Random r = new Random();
	    for (int i = 0; i < imi.rows; i++) {
		png.writeRow(lines[r.nextInt(3)], i);
	    }
	    png.end();
	    t0 = System.currentTimeMillis() - t0;

	}
	String chunks1, chunks2, chunks3;
	// read last row : inefficient
	// System.out.print("testing " + this.getClass().getSimpleName() + " : ");
	{
	    long t0 = System.currentTimeMillis();
	    PngReader pngr = new PngReader(file);
	    pngr.readRow(imi.rows - 1);
	    pngr.end();
	    t0 = System.currentTimeMillis() - t0;
	    chunks1 = pngr.getChunksList().toStringFull();
	    System.out.print(t0 + " msecs (normal) " + chunks1);
	}

	// skipping rows: most efficient
	{
	    long t0 = System.currentTimeMillis();
	    PngReader pngr = new PngReader(file);
	    pngr.readSkippingAllRows();
	    pngr.end();
	    t0 = System.currentTimeMillis() - t0;
	    chunks2 = pngr.getChunksList().toStringFull();
	    System.out.print(t0 + " msecs (skipping) "+chunks2);
	}
	// end prematurely. works , not so efficient
	{
	    long t0 = System.currentTimeMillis();
	    PngReader pngr = new PngReader(file);
	    pngr.end();
	    t0 = System.currentTimeMillis() - t0;
	    chunks3 = pngr.getChunksList().toStringFull();
	    // System.out.print(t0 + " msecs (end) ");
	}
	TestCase.assertEquals(chunks2, chunks1);
	TestCase.assertEquals(chunks3, chunks1);
	if (verbose)
	    System.out.println(chunks1);
    }
}
