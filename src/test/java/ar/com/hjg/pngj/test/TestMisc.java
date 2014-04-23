package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import junit.framework.TestCase;
import ar.com.hjg.pngj.BufferedStreamFeeder;
import ar.com.hjg.pngj.ChunkSeqReader;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineArray;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngReaderByte;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.PngjInputException;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.PngChunk;

/**
 * Methods of this class are designed for debug and testing PNGJ library, they
 * are not optimized
 */
public class TestMisc {

	/** intermixes two images (same size!) n rows from each */
	public static void mixInBands(File f1,File f2,File dest,int nrowsband,boolean increaseband) {
		PngReader pngr1=new PngReader(f1);
		PngReader pngr2=new PngReader(f2);
		if(!pngr2.imgInfo.equals(pngr1.imgInfo)) throw new RuntimeException("must be same type");
		PngWriter pngw = new PngWriter(dest,pngr1.imgInfo,false);
		pngw.copyChunksFrom(pngr1.getChunksList());
		int which =1;
		int nr=0;
		for(int i=0;i<pngr1.imgInfo.rows;i++) {
			IImageLine line1 = pngr1.readRow();
			IImageLine line2 = pngr2.readRow();
			pngw.writeRow(which==1? line1:line2);
			nr++;
			if(nr==nrowsband) {
				which = which==2 ? 1: which+1;
				nr=0;
				if(which==1&&increaseband) nrowsband++;
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
		mixInBands(f1, f2, f3, 1,true);
	}

}
