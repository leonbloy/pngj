package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;

/**
 * Methods of this class are designed for debug and testing PNGJ library, they
 * are not optimized
 */
public class TestMisc {

    /** intermixes two images (same size!) n rows from each */
    public static void mixInBands(File f1, File f2, File dest, int nrowsband, boolean increaseband) {
	PngReader pngr1 = new PngReader(f1);
	PngReader pngr2 = new PngReader(f2);
	if (!pngr2.imgInfo.equals(pngr1.imgInfo))
	    throw new RuntimeException("must be same type");
	PngWriter pngw = new PngWriter(dest, pngr1.imgInfo, false);
	pngw.copyChunksFrom(pngr1.getChunksList());
	int which = 1;
	int nr = 0;
	for (int i = 0; i < pngr1.imgInfo.rows; i++) {
	    IImageLine line1 = pngr1.readRow();
	    IImageLine line2 = pngr2.readRow();
	    pngw.writeRow(which == 1 ? line1 : line2);
	    nr++;
	    if (nr == nrowsband) {
		which = which == 2 ? 1 : which + 1;
		nr = 0;
		if (which == 1 && increaseband)
		    nrowsband++;
	    }
	}
	pngr1.end();
	pngr2.end();
	pngw.end();
	System.out.println("done: see " + dest);
    }

    public static void main(String[] args) {
	File f1 = TestSupport.absFile(new File("..\\..\\priv\\imgsetx\\l\\nosuave.png"));
	File f2 = TestSupport.absFile(new File("..\\..\\priv\\imgsetx\\l\\suave.png"));
	File f3 = TestSupport.absFile(new File("..\\..\\priv\\imgsetx\\l\\suavenosuave2.png"));
	mixInBands(f1, f2, f3, 1, true);
    }

}
