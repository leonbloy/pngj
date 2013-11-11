package ar.com.hjg.pngj.misc;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngWriter;

public class TestWriterFiltersPerformanceSet {
	private Collection<File> orig;
	private Class<? extends PngWriter> writerClass;
	private double timeAv = 0;
	private double timeWeighted = 0;
	private double performanceAv = 0;
	private double performanceWAv = 0;
	private int nFiles;
	private long bytesOrig;
	private String desc = "";

	public TestWriterFiltersPerformanceSet(Collection<File> orig,
			Class<? extends PngWriter> writerClass) {
		this.orig = orig;
		this.writerClass = writerClass;
	}

	public String doit(int nrepeat) {
		StringBuilder sb = new StringBuilder(desc + "\n");
		if(nFiles!=0) throw new RuntimeException("?");
		for (File f : orig) {
			nFiles++;
			bytesOrig += (f.length() - 66);
			TestWriterFiltersPerformance p = new TestWriterFiltersPerformance(
					f, writerClass);
			p.setRepeat(nrepeat);
			p.doit();
			sb.append(p.getSumm());
			double msecs = p.getMsecs();
			timeAv += msecs;
			timeWeighted +=  msecs*1000.0/(p.getBytesOrig() - 66.0);
			performanceAv += p.getCompression() ;
			performanceWAv += (p.getBytesOrig() - 66.0) * p.getCompression();
		}
		timeAv /= nFiles;
		timeWeighted /= nFiles;
		performanceAv /= nFiles;
		performanceWAv /= bytesOrig;
		return sb.toString();
	}

	public String getSummary() {
		return desc
				+ String.format(" %.3f %.3f %d %.1f", performanceAv,
						performanceWAv, (int) timeAv,
						 timeWeighted);
	}

	public void setDesc(String string) {
		desc = string;

	}

	public static TestWriterFiltersPerformanceSet createFromDir(File dir,
			Class<? extends PngWriter> writerClass) {
		List<File> li = new ArrayList<File>();
		for (File f : dir.listFiles())
			if (f.getName().endsWith("png"))
				li.add(f);
		return new TestWriterFiltersPerformanceSet(li, writerClass);

	}

	public static class PngWriter1 extends PngWriter {

		public PngWriter1(OutputStream outputStream, ImageInfo imgInfo) {
			super(outputStream, imgInfo);
			setCompLevel(9);
			setFilterType(FilterType.FILTER_AGGRESSIVE);
		}

	}

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);
		TestWriterFiltersPerformanceSet tp = createFromDir(new File(
				"d:\\hjg\\wokspace\\Varios\\resources\\img"), PngWriter1.class);
		String res = tp.doit(1);
		tp.setDesc("test");
		// String res = processFile(new
		// File("d:\\hjg\\wokspace\\Varios\\resources\\img\\transparency.png"),
		// PngWriter1.class, 1);
		System.out.println(res);
		System.out.println(tp.getSummary());

	}

}
