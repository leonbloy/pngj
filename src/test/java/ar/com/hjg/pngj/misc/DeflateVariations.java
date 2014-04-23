package ar.com.hjg.pngj.misc;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.ChunkSeqBasic;
import ar.com.hjg.pngj.ChunkSeqReader;
import ar.com.hjg.pngj.NullOs;
import ar.com.hjg.pngj.PngReaderDumb;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.test.TestSupport;

/**
 * given a PNG, re-compress the IDAT stream with an alternate deflater and evaluates the loss
 *
 */
public class DeflateVariations {
	
	public static class DeflaterNullStreamDummy extends OutputStream{
		protected long in=0,out=0;
		@Override
		public void write(int b) throws IOException {
			in++;
			out++;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			in +=len;
			out+=len;
		}

		@Override
		public void close() {
		}

		
		public long getBytesIn() {
			return in;
		}
		public long getBytesOut() {
			return out;
		}
		
		public double getRatio() {
			return in>0? out/(double)in : 1.0;
		}
	}

	
	public static class DeflaterSNDeflater extends DeflaterNullStreamDummy{

		private Deflater def;
		private NullOs nullOs;
		private DeflaterOutputStream os;

		public DeflaterSNDeflater(int level) {
			def = new Deflater(level);
			nullOs = new NullOs();
			os = new DeflaterOutputStream(nullOs,def);
		}

		@Override
		public void write(int b) throws IOException {
			in++;
			os.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			in +=len;
			os.write(b);
		}

		@Override
		public void close() {
			try {
				os.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			out = nullOs.getBytes();
		}
	}

	public static class DeflaterSNDeflaterSegmented extends DeflaterNullStreamDummy{

		private Deflater def;
		private NullOs nullOs;
		private DeflaterOutputStream os;
		private long segmentsize;
		private int level;
		private long inseg=0;
		public DeflaterSNDeflaterSegmented(long segmentsize,int level) {
			this.segmentsize = segmentsize;
			this.level=level;
			def = new Deflater(level);
			nullOs = new NullOs();
			os = new DeflaterOutputStream(nullOs,def);
		}

		@Override
		public void write(int b) throws IOException {
			throw new RuntimeException("?");
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if(len+inseg>segmentsize) {
				int len2 = (int) (segmentsize - inseg);
				write(b,off,len2);
				write(b,off+len2,len-len2);
			}
			else {
				in +=len;
				os.write(b);
				if(inseg==segmentsize) {
					os.flush();
					os.close();
					def = new Deflater(level);
					os = new DeflaterOutputStream(nullOs,def);
					inseg=0;
				}
			}
		}

		@Override
		public void close() {
			try {
				os.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			out = nullOs.getBytes();
		}
	}

	
	protected final DeflaterNullStreamDummy deflater;
	protected final File file;
	protected Inflater inflater =new Inflater();
	byte[] buf = new byte[1024];
	private long compressedBytesIn=0;
	private long rawBytes=0;
	public DeflateVariations(File file, DeflaterNullStreamDummy deflater) {
		this.file = file;
		this.deflater = deflater;
	}

	private void feed(byte[] data, int i, int len) {
		compressedBytesIn+=len;
		try {
			inflater.setInput(data, i, len);
			while(!inflater.needsInput()) {
				int n = inflater.inflate(buf);
				if(n >0) {
					deflater.write(buf, 0, n);
					rawBytes += n;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public double compress() {
		PngReaderDumb pngreader = new PngReaderDumb(file) {
			@Override
			protected ChunkSeqReader createChunkSeqReader() {
				ChunkSeqBasic cs = new ChunkSeqBasic(false) { // don't check CRC
					@Override
					protected void postProcessChunk(ChunkReader chunkR) {
						super.postProcessChunk(chunkR);
						if ((chunkR.getChunkRaw().id.equals(ChunkHelper.IDAT) ))
							feed(chunkR.getChunkRaw().data,0,chunkR.getChunkRaw().len);
					}
					@Override
					protected boolean shouldSkipContent(int len, String id) {
						return !(id.equals(ChunkHelper.IHDR)||id.equals(ChunkHelper.IDAT))  ; // we skip everything except IHDR and IDAT
					}
				};
				return cs;
			}

		};
		pngreader.readAll();
		deflater.close();
		double ratio=compressedBytesIn/(double)deflater.out;
		return ratio;
	}
	
	
	public long getCompressedBytesIn() {
		return compressedBytesIn;
	}

	public long getCompressedBytesOut() {
		return deflater.out;
	}

	public long getRawBytes() {
		return rawBytes;
	}
	
	public double getCompression() {
		return getCompressedBytesOut()/(double)getRawBytes();
	}
	
	/** percernt of *bytes*/
	public double getLoss() {
		return (getCompressedBytesOut()-getCompressedBytesIn())/(double)getCompressedBytesIn();
	}
	public double getLossRaw() {
		return (getCompressedBytesOut()-getCompressedBytesIn())/(double)getRawBytes();
	}


	public static void testSegmented(File dir) {
		List<File> files = TestSupport.getPngsFromDir(TestSupport.absFile(dir));
		System.out.printf("%s\t%s\t%s\t%s\t%s\n","filename","level","segment","segexp","loss","rawsize");
		for(File file:files) {
			for(int level = 6 ;level <=9;level+=3) {
				for(int segment=662;segment<=3000;segment=(segment*3)/2) {
					DeflateVariations df = new DeflateVariations(file, new DeflaterSNDeflaterSegmented(segment,level));
					df.compress();
					System.out.printf("%s\t%d\t%d\t%d\t%.5f\t%d\n",file.getName(),level,segment,(int)(segment/df.getCompression()),df.getLoss(),df.getRawBytes());
				}
				DeflateVariations df = new DeflateVariations(file, new DeflaterSNDeflater(level));
				df.compress();
				System.out.printf("%s\t%d\t%d\t%d\t%.5f\t%d\n",file.getName(),level,-1,-1,df.getLoss(),df.getRawBytes());
			}
		}
		
	}
	
	public static void main(String[] args) {
		testSegmented(new File("..\\..\\priv\\imgsets\\1\\m\\m115.png"));
		testSegmented(new File("..\\..\\priv\\imgsets\\1\\m\\m116.png"));
	}
}
