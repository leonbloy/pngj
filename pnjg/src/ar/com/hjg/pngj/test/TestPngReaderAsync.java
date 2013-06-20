package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

import org.junit.Test;

import ar.com.hjg.pngj.BufferedStreamFeeder;
import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.ChunkReaderDeflatedSet;
import ar.com.hjg.pngj.ChunkReaderFullSequence;
import ar.com.hjg.pngj.ChunkReaderFullSequence2;
import ar.com.hjg.pngj.ChunkReaderIdatSet;
import ar.com.hjg.pngj.IChunkProcessor;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngjBadCrcException;
import ar.com.hjg.pngj.chunks.PngChunkIDAT;

public class TestPngReaderAsync {
	Random rand = new Random();

	protected void feedFromStreamTest(ChunkReaderFullSequence as, File fs) throws Exception {
		BufferedStreamFeeder bf = new BufferedStreamFeeder(new FileInputStream(fs), 32);
		while (!as.isDone()) {
			if (!bf.hasMoreToFeed())
				throw new RuntimeException("premature ending");
			bf.feedAll(as);
		}
		PngHelperInternal.debug("done with fs");
	}

	private void feedFromStream2Sync(ChunkReaderFullSequence2 c, File file) throws Exception {
		BufferedStreamFeeder bf = new BufferedStreamFeeder(new FileInputStream(file), 32);
		int nbytes = 0;
		while (!c.isDone()) {
			bf.feed(c);
			ChunkReaderIdatSet idat = (ChunkReaderIdatSet) c.getCurReaderDeflatedSet();
			while (idat != null && idat.isDataPendingForConsumer()) {
				idat.advanceToNextRow();
				byte[] unfiltered = idat.getUnfilteredRow();
				nbytes += (idat.getRowFilled() - 1); //excluding filter byte
				if(idat.getRowFilled()<=1)
					throw new RuntimeException("??");
				/*if(idat.getDeinterlacer() != null)
					System.out.println("iterlaced bytes row:" + idat.getDeinterlacer().getBytesToRead());*/
				showUnfilteredRow(unfiltered, idat.getRowFilled(),idat.currentRow(),idat.currentdX(),idat.currentOffsetX());
				idat.advanceToNextRow();
			}
		}

		PngHelperInternal.debug("done with file" + file + " nbytes, IDAT: " + nbytes);
	}
	

	private void showUnfilteredRow(byte[] unfiltered, int nbytes, int rown, int dx, int ox) {
		System.out.print("nbytes=" + (nbytes - 1) + " f=" + unfiltered[0]);
		System.out.printf(" row:%d (dx=%d ox=%d)",rown,dx,ox);
		int n = nbytes > 10 ? 10 : nbytes;
		for (int i = 1; i < n; i++)
			System.out.printf(" %3d", unfiltered[i] & 0xff);
		
		System.out.println("");
	}

	IChunkProcessor createDummyChunkProcessor() { // Very dummy NO IDAT 
		IChunkProcessor sp = new IChunkProcessor() {
			public int processRow(byte[] rowb, int off, int len, int rown) {
				throw new RuntimeException("should not be used here");
			}

			public void processChunkStart(ChunkReader chunkReader) {
				PngHelperInternal.debug("dummy chunk start " + chunkReader);
			}

			public void processChunkEnd(ChunkReader chunkReader) {
				PngHelperInternal.debug("dummy chunk end " + chunkReader);
			}

			public boolean shouldCheckCrc(int len, String id) {
				return true;
			}

			public boolean shouldSkipContent(int len, String id) {
				return false;
			}

			public boolean isIdatLike(String id) {
				return false;
			}

			public ChunkReaderDeflatedSet createNewIdatSetReader(String id) {
				throw new RuntimeException("should not be used here");
			}

		};
		return sp;
	}

	IChunkProcessor createNoSoDummyChunkProcessor() { //  
		IChunkProcessor sp = new IChunkProcessor() {
			public int processRow(byte[] rowb, int off, int len, int rown) {
				throw new RuntimeException("should not be used here");
			}

			public void processChunkStart(ChunkReader chunkReader) {
				PngHelperInternal.debug("dummy chunk start " + chunkReader);
			}

			public void processChunkEnd(ChunkReader chunkReader) {
				PngHelperInternal.debug("dummy chunk end " + chunkReader);
			}

			public boolean shouldCheckCrc(int len, String id) {
				return true;
			}

			public boolean shouldSkipContent(int len, String id) {
				return false;
			}

			public boolean isIdatLike(String id) {
				return id.equals(PngChunkIDAT.ID);
			}

			public ChunkReaderDeflatedSet createNewIdatSetReader(String id) {
				return new ChunkReaderDeflatedSet(3, 3);
			}

		};
		return sp;
	}

	/**
	 * purely sync, no IDAT
	 */
	@Test
	public void testOkDummy() throws Exception {
		ChunkReaderFullSequence c = new ChunkReaderFullSequence();
		c.setChunkProcessor(createDummyChunkProcessor());
		feedFromStreamTest(c, new File("resources/samples/testg1.png"));
	}

	@Test(expected = PngjBadCrcException.class)
	public void testBadCrc() throws Exception {
		ChunkReaderFullSequence c = new ChunkReaderFullSequence();
		c.setChunkProcessor(createDummyChunkProcessor());
		feedFromStreamTest(c, new File("resources/samples/testg1badcrc.png"));
	}

	@Test
	public void testOkIdatBasicSync() throws Exception {
		//feedFromStream2Sync(new ChunkReaderFullSequence2(), new File("resources/samples/testg1.png"));
		//feedFromStream2Sync(new ChunkReaderFullSequence2(), new File("resources/samples/testg1i.png"));
		feedFromStream2Sync(new ChunkReaderFullSequence2(), new File("resources/samples/testg2i.png"));
	}

}
