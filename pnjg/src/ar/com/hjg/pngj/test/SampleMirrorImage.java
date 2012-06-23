package ar.com.hjg.pngj.test;

import java.io.File;
import java.util.List;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkPredicate;
import ar.com.hjg.pngj.chunks.PngChunk;

/**
 * Mirrors an image, along the rows.
 * This works for all image types, and is used for TestPngSuite
 */
public class SampleMirrorImage {

	public static void mirror(File orig, File dest,boolean overwrite) throws Exception {
		PngReader pngr = FileHelper.createPngReader(orig);
		PngWriter pngw = FileHelper.createPngWriter(dest, pngr.imgInfo, overwrite);
		pngw.setFilterType(FilterType.FILTER_CYCLIC); // just to test all filters
		int copyPolicy = ChunkCopyBehaviour.COPY_ALL;
		pngw.copyChunksFirst(pngr, copyPolicy);
		ImageLine lout = new ImageLine(pngw.imgInfo);
		int cols = pngr.imgInfo.cols;
		int channels = pngr.imgInfo.channels;
		int[] line = null;
		int aux;
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			line = l1.unpack(line, false);
			for (int c1 = 0, c2 = cols - 1; c1 < c2; c1++, c2--) {
				for (int i = 0; i < channels; i++) {
					aux = line[c1 * channels + i];
					line[c1 * channels + i] = line[c2 * channels + i];
					line[c2 * channels + i] = aux;
				}
			}
			lout.pack(line, false);
			pngw.writeRow(lout, row);
		}
		// pngr.end(); // not necessary now
		pngw.copyChunksLast(pngr, copyPolicy);
		pngw.end();
		//// print unknown chunks, just for information
		List<PngChunk> u = ChunkHelper.filterList(pngr.getChunksList().getChunks(), new ChunkPredicate() {
			public boolean match(PngChunk c) {
				return ChunkHelper.isUnknown(c);
			}
		});
		if (!u.isEmpty())
			System.out.println("Unknown chunks:" + u);
	}


	public static void main(String[] args) throws Exception {
		if (args.length != 2 || args[0].equals(args[1])) {
			System.err.println("Arguments: [pngsrc] [pngdest]");
			System.exit(1);
		}
		File file1 = new File(args[0]); 
		File file2 = new File(args[1]);
		mirror(file1,file2,false);
		System.out.printf("Done: %s -> %s\n", file1.toString(),file2.toString());
	}
}
