package ar.com.hjg.pngj.samples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import ar.com.hjg.pngj.BufferedStreamFeeder;
import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.ChunkSeqReader;
import ar.com.hjg.pngj.DeflatedChunksSet;
import ar.com.hjg.pngj.NullOs;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngIDatChunkOutputStream;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.PngjOutputException;
import ar.com.hjg.pngj.test.PngjTest;
import ar.com.hjg.pngj.test.TestSupport;
/**
 * This shows how to read at low level a PNG, without PngReader, to do some processing to the IDAT stream.
 * 
 */
public class SampleRecompressIdat extends PngjTest {

	public static class ChunkSeqReaderIdatRaw1 extends ChunkSeqReader { // callback mode
		final int rowsize;
		private final OutputStream rawOs;
		private final OutputStream idatOs;
		private InputStream is;

		/**
		 * This reader writes the chunks directly to the rawOs output, except that the IDAT
		 * stream is decompressed and writen to idatOs
		 * 
		 * @param is
		 * @param rawOs
		 * @param idatOs
		 * @param rowsize 
		 */
		public ChunkSeqReaderIdatRaw1(InputStream is, OutputStream rawOs,OutputStream idatOs) {
			super();
			this.is=is;
			this.rowsize = 8192; // prety arbitrary
			this.rawOs = rawOs;// here we'll write all data outside IDAT chunk
			this.idatOs = idatOs;// here we'll write all IDAT data
			
		}

		public void readAll() {
			BufferedStreamFeeder feeder = new BufferedStreamFeeder(is);
			while(feeder.hasMoreToFeed()) {
				feeder.feed(this);
			}
			try {
			is.close();
			idatOs.close();
			rawOs.close();
			} catch(Exception e){
				throw new PngjException(e);
			}
		}
		
		@Override
		protected void postProcessChunk(ChunkReader chunkR) {
			super.postProcessChunk(chunkR);
			if(!isIdatKind(chunkR.getChunkRaw().id))
			chunkR.getChunkRaw().writeChunk(rawOs);
		}

		@Override
		protected void checkSignature(byte[] buf) {
			super.checkSignature(buf);
			PngHelperInternal.writeBytes(rawOs, PngHelperInternal.getPngIdSignature());
		}

		@Override
		protected DeflatedChunksSet createIdatSet(String id) {
			DeflatedChunksSet cc = new DeflatedChunksSet(id, rowsize, rowsize) {
				@Override
				protected int processRowCallback() {
					byte[] rawidat=getInflatedRow(); 
					int bytes=getRowFilled();
					PngHelperInternal.writeBytes(idatOs, rawidat,0,bytes);
					return rowsize;
				}
				
				@Override
				protected void finished() {
					try {
						idatOs.close();
					} catch (IOException e) {
						throw new PngjOutputException(e);
					}
				}
			};
			cc.setCallbackMode(true);
			return cc;
		}

		@Override
		protected boolean isIdatKind(String id) {
			return id.equals("IDAT");
		}
	}
	
	/** reads a PNG and writes to a file the raw (unfiltered) IDAT stream */
	public static void writeRawIdatToFile(File png,File rawIdat) {
		try {
			InputStream  is = new FileInputStream(png);
			OutputStream osIdat = new FileOutputStream(rawIdat);
			OutputStream  osRaw = new NullOs();
			ChunkSeqReaderIdatRaw1 reader=new ChunkSeqReaderIdatRaw1(is, osRaw, osIdat);
			reader.readAll();
			is.close();
			osIdat.close();
			osRaw.close();
		} catch (IOException e) {
			throw new PngjException(e);
		}
	}

	/** reads a PNG and rewrites it recompressing the IDAT stream */
	public static void recompressPng(InputStream is,OutputStream os,Deflater deflater,int idatMaxSize) {
		try {
			PngIDatChunkOutputStream idatSt=new PngIDatChunkOutputStream(os,idatMaxSize);
			DeflaterOutputStream deflaterOs= new DeflaterOutputStream(idatSt, deflater);
			ChunkSeqReaderIdatRaw1 reader=new ChunkSeqReaderIdatRaw1(is, os, deflaterOs);
			reader.readAll();
			is.close();
			deflaterOs.close();
			os.close();
		} catch (IOException e) {
			throw new PngjException(e);
		}
	}


	
	public static void main(String[] args) {
		TestSupport.getResourcesDir();
		String src = "d:\\devel\\repositories\\pngj\\priv\\imgsets\\2\\l\\l209.png";
		File dest = new File("d:\\temp\\l290raw");
		writeRawIdatToFile(new File(src),dest);
		System.out.println("recompressed in " + dest);

	}
}
