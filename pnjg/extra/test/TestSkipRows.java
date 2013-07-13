package ar.com.hjg.pngj.test;

import java.io.File;
import java.util.Random;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;

public class TestSkipRows {

	void testSkipRows(boolean verbose) {
		// creates big image
		ImageInfo imi = new ImageInfo(4000, 9000, 8, false);
		String suffix = "raw";
		File f = TestsHelper.getTmpFile(suffix);
		{
			long t0 = System.currentTimeMillis();
			PngWriter png = TestsHelper.prepareFileTmp(suffix, imi);
			ImageLine[] lines = new ImageLine[3];
			lines[0] = TestsHelper.generateNoiseLine(imi);
			lines[1] = TestsHelper.generateNoiseLine(imi);
			lines[2] = TestsHelper.generateNoiseLine(imi);
			png.setFilterType(FilterType.FILTER_NONE);
			// add two text chunks, one before IDAT, onther after
			png.getMetadata().setText("chunk1", "test1").setPriority(true);
			png.getMetadata().setText("chunk2", "test2").setPriority(false);
			Random r = new Random();
			for (int i = 0; i < imi.rows; i++) {
				png.writeRow(lines[r.nextInt(3)], i);
			}
			png.end();
			t0 = System.currentTimeMillis() - t0;

			if (verbose)
				System.out.println("Created " + f + " (" + f.length() / (1024) + " KB) will be removed . " + t0
						+ " msecs");
		}
		String chunks1, chunks2;
		// skipping read
		System.out.print("testing " + this.getClass().getSimpleName() + " : ");
		{
			long t0 = System.currentTimeMillis();
			PngReader pngr = TestsHelper.getReaderTmp(suffix);
			pngr.readRowByte(pngr.imgInfo.rows - 1);
			pngr.end();
			t0 = System.currentTimeMillis() - t0;
			chunks1 = pngr.getChunksList().toStringFull();
			System.out.print(t0 + " msecs (normal)  ");
		}

		// skipping read
		{
			long t0 = System.currentTimeMillis();
			PngReader pngr = TestsHelper.getReaderTmp(suffix);
			pngr.readSkippingAllRows();
			pngr.end();
			t0 = System.currentTimeMillis() - t0;
			chunks2 = pngr.getChunksList().toStringFull();
			System.out.print(t0 + " msecs (skipping) ");
		}
		System.out.println("done");
		if (!chunks2.equals(chunks1))
			throw new RuntimeException("Error: chunks are different" + chunks2);
		if (verbose)
			System.out.println(chunks1);
		f.delete();
	}

	public static void main(String[] args) {
		new TestSkipRows().testSkipRows(true);
	}
}
