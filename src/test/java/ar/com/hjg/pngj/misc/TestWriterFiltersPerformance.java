package ar.com.hjg.pngj.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineSet;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;

public class TestWriterFiltersPerformance {
	public final File orig;
	private int times=1;
	private long msecs;
	private long bytesOrig;
	private long bytesResult;
	private Class<? extends PngWriter> writerClass;
	private boolean writeToFile=false;
	private static File  tempFile =new File("C:/tmp/res.png");
	public TestWriterFiltersPerformance(File orig, Class<? extends PngWriter>writerClass) {
		this.orig = orig;
		this.writerClass = writerClass;
		init();
	}
	
	private void init() {
		if(! orig.canRead()) throw new RuntimeException("Can't read " + orig);
		bytesOrig= orig.length();
		
	}
	
	private long reencode() {
		try {
			PngReader reader = new PngReader(orig);
			OutputStream os = writeToFile ? new FileOutputStream(tempFile) :new NullOs();
			PngWriter writer = writerClass.getDeclaredConstructor(OutputStream.class,ImageInfo.class).newInstance(os,reader.imgInfo);
			writer.copyChunksFrom(reader.getChunksList());
			IImageLineSet<? extends IImageLine> rows = reader.readRows();
			long t0=System.currentTimeMillis();
			writer.writeRows(rows);
			long t1=System.currentTimeMillis();
			reader.end();
			writer.end();
			os.close();
			if(writeToFile)
				bytesResult = tempFile.length();
			else 
				bytesResult = ((NullOs)os).getBytes();
			return t1-t0;
		} catch (Exception e) {
				throw new RuntimeException(e);
		}
	}

	public void doit() {
		msecs=0;
		for(int i=0;i<times;i++)
			msecs+=reencode();
		if(times>1) msecs/=(times);
	}

	public long getMsecs() {
		return msecs;
	}

	public long getBytesOrig() {
		return bytesOrig;
	}

	public long getBytesResult() {
		return bytesResult;
	}

	public void setRepeat(int repeat) {
		this.times = repeat;
	}
	
	
	public double getCompression() {
		return (double)bytesResult/(double)bytesOrig;
	}
	
	public String getSumm() {
		return String.format("%s %d %.3f %d\n",orig.getName(),bytesOrig,getCompression(),getMsecs());
	}

	public static String processDir(File dir, Class<? extends PngWriter>writerClass,int repeat) {
		StringBuilder sb=new StringBuilder();
		for(File f: dir.listFiles()) {
			if(! f.getName().endsWith("png")) continue;
			String res=processFile(f, writerClass, repeat);
			sb.append(res);
		}
		return sb.toString();
	}

	public static String processFile(File f, Class<? extends PngWriter>writerClass,int repeat) {
			TestWriterFiltersPerformance tp = new TestWriterFiltersPerformance(f, writerClass);
			tp.setRepeat(repeat);
			//tp.writeToFile=true;
			tp.doit();
			return tp.getSumm();
	}

}
