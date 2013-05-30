package ar.com.hjg.pngj;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkIEND;
import ar.com.hjg.pngj.chunks.PngChunkIHDR;

public class PngReader2 implements IdatProcessRow { // ASYNC

	private byte[] buf0 = new byte[8]; // for signature or chunk starts
	private int buf0len = 0;
	private boolean headerDone = false;
	private boolean endDone = false;

	private long readBytes = 0;
	private ImageInfo imgInfo;
	private ChunkReader curChunkReader;
	private ChunkReaderIdatSet curIdatSetReader; // must be set to null when expecting a new idatset
	private boolean interlaced;
	private PngDeinterlacer deinterlacer;

	/** returns processed bytes */
	protected int feedBytesN(byte[] buf, int off, int len) { // each 
		if (len == 0 || endDone)
			return len; // nothing to do
		if (len < 0)
			throw new PngjInputException("Bad len: " + len);
		int processed = 0;
		if (headerDone) {
			if (curChunkReader == null || curChunkReader.isDone()) { // new chunk: read 8 bytes
				int t = 8 - buf0len;
				if (t > len)
					t = len;
				System.arraycopy(buf, off, buf0, buf0len, t);
				buf0len += t;
				processed += t;
				if (buf0len == 8) {
					startNewChunk(PngHelperInternal.readInt4fromBytes(buf0, 0), ChunkHelper.toString(buf0, 4, 4));
					buf0len = 0;
				}
			} else { // reading chunk 
				processed+=curChunkReader.feed(buf, off, len);
			}
		} else { // parsing header
			int read = 8 - buf0len;
			if (read > len)
				read = len;
			System.arraycopy(buf, off, buf0, buf0len, read);
			buf0len += read;
			readBytes += read;
			if (buf0len == 8)
				processSignature();
			processed += read;
		}
		return processed;
	}
	
	public void feedBytes(byte[] buf, int off, int len) { // each
		while(len >0) {
			int n=feedBytesN(buf, off, len);
			if(n<1) return;
			len-=n;
			off+=n;
		}
	}

	public class ChunkReaderAsync extends ChunkReader {

		public ChunkReaderAsync(int clen, String id, boolean checkCRC, long offsetInPng) {
			super(clen, id, checkCRC, offsetInPng);
		}

		@Override
		protected void chunkDone() {
			processChunk(this);
		}

	}

	private void processChunk(ChunkReader chunkR) { // This is onyl for the non-idat chunks
		PngChunk c = PngChunk.factoryFromId(chunkR.getChunkRaw().id, imgInfo); // notice that imgInfo can be null for IHDR
		System.out.println("processing read chunk " + c.id + " (dfgds68)");
		c.parseFromRaw(chunkR.getChunkRaw()); // TODO: lazy parse? what about those ignored
		// first IHDR is special
		if (c instanceof PngChunkIHDR) {
			if (imgInfo != null)
				throw new PngjInputException("IHDR duplicated?");
			PngChunkIHDR c1 = (PngChunkIHDR) c;
			imgInfo = c1.createImageInfo();
			interlaced = c1.getInterlaced() == 1;
			deinterlacer = interlaced ? new PngDeinterlacer(imgInfo) : null;
		} else {
			if (imgInfo == null)
				throw new PngjInputException("IHDR absent?");
			if(c instanceof PngChunkIEND) endDone=true;
		}
	}

	protected void startNewChunk(int len, String id) {
		boolean isIdatType = id.equals(ChunkHelper.IDAT) || id.equals("fdAT");
		boolean checkCrc = shouldCheckCrc(len, id);
		if (isIdatType) {
			if (curIdatSetReader == null || curIdatSetReader.isDone()) {
				curIdatSetReader = new ChunkReaderIdatSet(true);
				curIdatSetReader.setCallbackRow(this);
				curIdatSetReader.setNextRowSize(imgInfo.bytesPerRow+1); // TODO: only for IDAT and non-interlaced
			}
			curChunkReader = new ChunkReaderIdat(len, id, checkCrc, readBytes, curIdatSetReader);
			if(! id.equals(ChunkHelper.IDAT))
				curChunkReader.skipContent = true;
		} else {
			curChunkReader = new ChunkReader(len, id, checkCrc, readBytes) {
				protected void chunkDone() {
					processChunk(this);
				}
			};
			curChunkReader.skipContent = shouldSkipContent(len, id);
		}
		System.out.println("starting new chunk: " + curChunkReader);
		// TODO: set curIdatSetReader if corresponds
	}

	private boolean shouldSkipContent(int len, String id) {
		return false; // TODO implement
	}

	private boolean shouldCheckCrc(int len, String id) {
		return true; // TODO: change
	}

	private void processSignature() {
		if (!Arrays.equals(buf0, PngHelperInternal.getPngIdSignature()))
			throw new PngjInputException("Bad PNG signature");
		headerDone = true;
		buf0len = 0;
	}

	public void end() {

	}

	public int processRow(byte[] rowb, int off, int len, int rown) {

		return imgInfo.bytesPerRow+1; // TODO: only for non-interlaced
	}

	public static void test1(String filename) throws Exception {
		PngReader2 pngr = new PngReader2();
		FileInputStream is=new FileInputStream(filename);
		byte buf[] = new byte[128];
		Random r= new Random();
		while(! pngr.endDone) {
			int toRead =r.nextInt(buf.length-1)+1;
			int n = is.read(buf,0,toRead);
			if(n<1) throw new RuntimeException("premature ending");
			pngr.feedBytes(buf, 0, n);
		}
		is.close();
		System.out.println("IDAT rows= " + pngr.curIdatSetReader.rown + " done:" + pngr.curIdatSetReader.isDone());
	}
	
	public static void main(String[] args) throws Exception {
		test1("d:/temp/ps1200.png");
	}
}
