package ar.com.hjg.pngj;

import java.util.Random;

class PngDeinterlacer {
	private final ImageInfo imi;
	private int pass; // 1-7
	private int rows, cols, dY, dX, oY, oX, oXsamples, dXsamples; // at current pass
	private int currRowSubimg = -1; // current row in the virtual subsampled image; this incrementes from 0 to cols/dy 7
									// times
	private int currRowReal = -1; // in the real image, this will cycle from 0 to im.rows in different steps, 7 times

	private int packedValsPerPixel;
	private int packedMask;
	private int packedShift;

	PngDeinterlacer(ImageInfo iminfo) {
		this.imi = iminfo;
		pass = 0;
		if (imi.packed) {
			packedValsPerPixel = 8 / imi.bitDepth;
			packedShift = imi.bitDepth;
			if (imi.bitDepth == 1)
				packedMask = 0x80;
			else if (imi.bitDepth == 2)
				packedMask = 0x80;
			else
				packedMask = 0xf0;
		}
		setPass(1);
		setRow(0);
	}

	/** this refers to the row currRowSubimg */
	public void setRow(int n) {
		currRowSubimg = n;
		currRowReal = n * dY + oY;
		if (currRowReal < 0 || currRowReal >= imi.rows)
			throw new RuntimeException("bad row");
	}

	public void setPass(int p) {
		if (this.pass == p)
			return;
		pass = p;
		switch (pass) {
		case 1:
			dY = dX = 8;
			oX = oY = 0;
			break;
		case 2:
			dY = dX = 8;
			oX = 4;
			oY = 0;
			break;
		case 3:
			dX = 4;
			dY = 8;
			oX = 0;
			oY = 4;
			break;
		case 4:
			dX = 4;
			dY = 4;
			oX = 2;
			oY = 0;
			break;
		case 5:
			dX = 2;
			dY = 4;
			oX = 0;
			oY = 2;
			break;
		case 6:
			dX = 2;
			dY = 2;
			oX = 1;
			oY = 0;
			break;
		case 7:
			dX = 1;
			dY = 2;
			oX = 0;
			oY = 1;
			break;
		default:
			throw new RuntimeException("bad interlace pass" + pass);
		}
		rows = (imi.rows - oY) / dY + 1;
		if ((rows - 1) * dY + oY >= imi.rows)
			rows--; // can be 0
		cols = (imi.cols - oX) / dX + 1;
		if ((cols - 1) * dX + oX >= imi.cols)
			cols--; // can be 0
		if (cols == 0)
			rows = 0;
		dXsamples = dX * imi.channels;
		oXsamples = oX * imi.channels;
	}

	public void fill(int[] src, int[] dst) {
		if (!imi.packed)
			for (int i = 0, j = oXsamples; i < cols*imi.channels; i += imi.channels, j += dXsamples)
				for (int k = 0; k < imi.channels; k++)
					dst[j + k] = src[i + k];
		else
			fillFromPacked(src, dst);
	}

	// this is very clumsy!
	private void fillFromPacked(int[] src, int[] dst) {
		int spos, smod, smask; // source byte position, bits to shift to left (01,2,3,4
		int tpos, tmod, p, d;
		spos = 0;
		smask = packedMask;
		smod = -1;
		//Arrays.fill(dst, 0);
		for (int i = 0, j = oX; i < cols; i++,j += dX) {
			spos = i / packedValsPerPixel;
			smod += 1;
			if (smod >= packedValsPerPixel)
				smod = 0;
			smask >>= packedShift; // the source mask cycles
			if (smod == 0)
				smask = packedMask;
			tpos = j / packedValsPerPixel;
			tmod = j % packedValsPerPixel;
			p = src[spos] & smask;
			d = tmod - smod;
			if (d > 0)
				p >>= (d * packedShift);
			else if (d < 0)
				p <<= ((-d) * packedShift);
			dst[tpos] |= p;
		}
	}

	/**
	 * Pixels to read in this row. Should not be zero
	 */
	public int getPixelsToRead() {
		return cols;
	}

	public boolean isAtLastRow() {
		return pass == 7 && currRowSubimg == rows - 1;
	}

	public int getCurrRowSubimg() {
		return currRowSubimg;
	}

	public int getPass() {
		return pass;
	}

	/** rows in the current pass */
	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}

	public int getdY() {
		return dY;
	}

	public int getdX() {
		return dX;
	}

	public int getoX() {
		return oX;
	}

	public int getoXsamples() {
		return oXsamples;
	}

	public int getdXsamples() {
		return dXsamples;
	}

	public int getCurrRowReal() {
		return currRowReal;
	}

	public static void test() {
		Random r = new Random();
		//PngInterlaceHelper ih = new PngInterlaceHelper(new ImageInfo(r.nextInt(35) + 1, r.nextInt(52) + 1, 8, true));
		PngDeinterlacer ih = new PngDeinterlacer(new ImageInfo(5, 3, 8, true));
		int np = ih.imi.cols * ih.imi.rows;
		System.out.println(ih.imi);
		for (int p = 1; p <= 7; p++) {
			ih.setPass(p);
			for (int row = 0; row < ih.getRows(); row++) {
				ih.setRow(row);
				int b = ih.getPixelsToRead();
				np -= b;
				System.out.printf("Read %d pixels. Pass:%d Realline:%d cols=%d dX=%d oX=%d last:%b\n", b, ih.pass,
						ih.currRowReal, ih.cols, ih.dX, ih.oX, ih.isAtLastRow());

			}
		}
		if (np != 0)
			throw new RuntimeException("??" + ih.imi);
	}

	/*public static void main(String[] args) {
		test();
	}*/

}
