package ar.com.hjg.pngj.misc;

import java.io.File;
import java.io.ObjectInputStream.GetField;
import java.util.zip.Deflater;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.test.TestSupport;

/**
 * reencodes a png image with a given filter and compression level
 */
public class TestFilters {
	
	/* returns compressed size and time in msecs */
	public static double[] reencode(File orig, FilterType filterType, int cLevel,boolean filteredStrategy) {
		long t0 = System.currentTimeMillis();
		File dest = new File(TestSupport.getTempDir(),TestSupport.addSuffixToName(orig.getName(), filterType + "_" + cLevel ));
		PngReader pngr = new PngReader(orig);
		PngWriter pngw = new PngWriter(dest, pngr.imgInfo, true);
		pngw.setFilterType(filterType);
		pngw.setCompLevel(cLevel);
		pngw.setDeflaterStrategy(filteredStrategy?Deflater.FILTERED :Deflater.DEFAULT_STRATEGY);
		pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLineInt l1 = (ImageLineInt) pngr.readRow();
			pngw.writeRow(l1);
		}
		pngr.end();
		pngw.end();
		double ratio=pngw.computeCompressionRatio();
		long t1 = System.currentTimeMillis();
		return new double[]{ratio,t1-t0};
	}

	public static void tryAllFilters(File file, int clevel,boolean filtered) {
		for (FilterType filter : FilterType.getAllStandardAndMainStrategies()) {
			double[] res=reencode(file, filter, clevel,filtered);
			System.out.printf("%s\tlevel=%d\tf=%s\tfilter=%s\tcomp=%.4f\ttime=%.1f\n",file.getName(),clevel,filtered?"f":"d",filter.toString(),res[0],res[1] );
		}
	}

	private static void warmup() {
		for (FilterType filter : FilterType.getAllStandardAndMainStrategies()) {
			double[] res=reencode(new File("src/test/resources/testcomp/bsas.png"), filter, 5,true);
		}
	
	}
	// TODO convert this to some decent extensive benchamark
	public static void main(String[] args) throws Exception {
		warmup();
		tryAllFilters(new File("src/test/resources/testcomp/25-c2-compression-strategy.png"), 7,false);
		tryAllFilters(new File("src/test/resources/testcomp/25-c2-compression-strategy.png"), 7,true);
		tryAllFilters(new File("src/test/resources/testcomp/bsas.png"), 7,false);
		tryAllFilters(new File("src/test/resources/testcomp/bsas.png"), 7,true);
		tryAllFilters(new File("src/test/resources/testcomp/hjg.png"), 7,false);
		tryAllFilters(new File("src/test/resources/testcomp/hjg.png"), 7,true);
	}
}
