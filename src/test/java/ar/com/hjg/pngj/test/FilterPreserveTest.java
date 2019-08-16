package ar.com.hjg.pngj.test;

import java.io.File;

import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngReaderByte;
import ar.com.hjg.pngj.PngWriter;

public class FilterPreserveTest {

    private File origFile() {
	return new File(TestSupport.getResourcesDir(), "test/stripesoptim.png");
    }

    private File newFile() {
	return new File(TestSupport.getTempDir(), "stripes2.png");
    }

    @Test
    public void testDontPreserve1() {
	PngReader pngr = new PngReader(origFile());
	File dest = newFile();
	PngWriter pngw = new PngWriter(dest, pngr.imgInfo);

	for (int i = 0; i < pngr.imgInfo.rows; i++) {
	    pngw.writeRow(pngr.readRow());
	}
	pngr.end();
	pngw.end();
	String f1 = TestSupport.showFilters(origFile(), 30, false);
	String f2 = TestSupport.showFilters(dest, 30, false);
	TestCase.assertFalse(f1.equals(f2)); // must be different
    }

    @Test
    public void testPreserve1() {
	PngReader pngr = new PngReader(origFile());
	File dest = newFile();
	PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	pngw.setFilterPreserve(true);
	for (int i = 0; i < pngr.imgInfo.rows; i++) {
	    pngw.writeRow(pngr.readRow());
	}
	pngr.end();
	pngw.end();
	String f1 = TestSupport.showFilters(origFile(), 30, false);
	String f2 = TestSupport.showFilters(dest, 30, false);
	TestCase.assertEquals(f1, f2); // must be different
    }

    @Test
    public void testDontPreserve2() {
	PngReaderByte pngr = new PngReaderByte(origFile());
	File dest = newFile();
	PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	pngw.writeRows(pngr.readRows());
	pngr.end();
	pngw.end();
	String f1 = TestSupport.showFilters(origFile(), 30, false);
	String f2 = TestSupport.showFilters(dest, 30, false);
	TestCase.assertFalse(f1.equals(f2)); // must be different
    }

    @Test
    public void testPreserve2() {
	PngReaderByte pngr = new PngReaderByte(origFile());
	File dest = newFile();
	PngWriter pngw = new PngWriter(dest, pngr.imgInfo);
	pngw.setFilterPreserve(true);
	pngw.writeRows(pngr.readRows());
	pngr.end();
	pngw.end();
	String f1 = TestSupport.showFilters(origFile(), 30, false);
	String f2 = TestSupport.showFilters(dest, 30, false);
	TestCase.assertEquals(f1, f2);
    }

}
