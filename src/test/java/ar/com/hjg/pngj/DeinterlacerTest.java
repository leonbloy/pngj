package ar.com.hjg.pngj;

import java.util.Random;

import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.Deinterlacer;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.test.PngjTest;

public class DeinterlacerTest extends PngjTest {
	Random rand = new Random();

	void test1(int cols, int rows) {
		String msg = String.format("cols=%d rows=%d", cols, rows);
		ImageInfo imi = new ImageInfo(cols, rows, 8, true);
		Deinterlacer ih = new Deinterlacer(imi);
		int np = 0; // pixles
		int nr = 0;
		TestCase.assertFalse(msg, ih.isEnded());
		int[][] im = new int[rows][cols];
		do {
			nr++;
			// System.out.println("p="+ih.getPass() + " r="+ih.getCurrRowSubimg() + "/" +
			// ih.getRows()+
			// " cols="+ih.getCols());
			int colssub = ih.getCols();
			for (int cc = 0; cc < colssub; cc++) {
				int x = ih.getoX() + cc * ih.getdX();
				int y = ih.getCurrRowReal();
				TestCase.assertEquals(msg, im[y][x], 0);
				im[y][x]++;
			}
			np += colssub;
		} while (ih.nextRow());

		TestCase.assertEquals(msg, cols * rows, np);
		TestCase.assertEquals(msg, ih.getTotalRows(), nr);
		TestCase.assertTrue(msg, ih.isEnded());

	}

	@Test
	public void checkAll() {
		for (int r = 1; r < 77; r++)
			for (int c = 1; c < 88; c++)
				test1(c, r);
	}
}
