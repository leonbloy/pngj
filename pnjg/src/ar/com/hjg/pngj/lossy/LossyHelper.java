package ar.com.hjg.pngj.lossy;

import java.io.File;
import java.util.Arrays;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunksToWrite;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkTEXT;
import ar.com.hjg.pngj.nosandbox.FileHelper;

/**
 */
public class LossyHelper {

	// parameters
	private int parLossy = 0; // 0 : no lossyness 50 : noticeable distortion
	private double parTableQuantA = 0.0; // quantization : 2^a x^(k+1)
	private double parTableQuantK = 0.0;
	private double parMemory = 0.87; // memory factor tau ~ -1/log(parAlfa)
	private double parGradient = 0.5; // between 0-1 - influences parActivityThreshold
	private double parActivityThreshold = 0.0; // regions with activity (average r0) below this value will not be
												// qunatized if |r|<=2 (0: never, 0.25: careful with gradients)
	private double parDetail = 0.5; // between 0-1
	private int parTrimPrediction = 0; // influenced by parSmooth
	private int parTolerance = 20; // error acceptable in image, in absolute value; should be about parLossy/4+2

	private double activity = 0.0; // average absolute value of prediction error

	private static final boolean WRITE_LOSS_IMG_INFO = false; // only for debuggin, testing

	private int[] tablequant1; // 0-255
	private int[] tablequant2; // idem, more precise the first
	private int[] tablequant3; // idem, more precise the first

	private IErrorDifussion errordif;
	private final ImageInfo imginfo;
	private PngWriter pngw;
	private ImageLine imgline;
	private final static long[] statR0 = new long[256];
	private final static long[] statR1 = new long[256];

	
	public LossyHelper(ImageInfo imgInfo) {
		this.imginfo = imgInfo;
		if (imgInfo != null && WRITE_LOSS_IMG_INFO) {
			pngw = FileHelper.createPngWriter(new File("/temp/lossy.png"), new ImageInfo(imgInfo.cols, imgInfo.rows, 8,
					false), true);
			imgline = new ImageLine(pngw.imgInfo);
		}
	}

	public void setLossy(int lossyness) {
		this.parLossy = lossyness;
		parTableQuantA = lossyness * 0.1;
		double step = Math.pow(2, parTableQuantA);
		parGradient = 0.8;
		parDetail = 0.25;
		parTableQuantK = 1.98;
		parTolerance = (int)(step * 5);
		if (parTolerance > 50)
			parTolerance = 50;
		parMemory = 0.85;
		parActivityThreshold = Math.sqrt(8.0*step) * parGradient;
		parTrimPrediction = (int) (step *(1-parDetail) / (1 + parGradient * 3));
	}
	/**
	 * Argument is the original prediction error, signed, beforebyte folding (range -255 255) Response is ready to write
	 * prediction error, in range [0,255]
	 * */
	private int quantizeTable(int x, int offset, int usetable) {
		if (tablequant1 == null) {
			tablequant1 = buildTable2(parTableQuantA, parTableQuantK, 0);
			tablequant2 = buildTable2(parTableQuantA, parTableQuantK, 1);
			tablequant3 = buildTable2(parTableQuantA, parTableQuantK, 2);
		}
		if (offset != 0 && x != 0) {
			if (x > 0)
				x = x > offset ? x - offset : 0;
			else
				x = x < -offset ? x + offset : 0;
		}
		x = x & 0xFF;
		if (usetable == 1)
			return tablequant1[x];
		else if (usetable == 2)
			return tablequant2[x];
		else
			return tablequant3[x];
	}

	public int quantize(int x, int row, int col) {
		return quantizeTable(x, parTrimPrediction, activity >= parActivityThreshold ? 1
				: activity >= parActivityThreshold * 0.5 ? 2 : 3);
	}

	public void setUpFloydErrorDiffusion() {
		errordif = new ErrorDifussionFloydSteinberg(imginfo, 0);
	}

	public void setUpTrivialErrorDiffusion() {
		errordif = new ErrorDifussionTrivial(imginfo, 0);
	}

	/**
	 * 
	 * @param real
	 * @param approx
	 * @return
	 */
	public boolean isacceptable(int real, int approx, boolean signed) {
		int ma, mi;
		if (signed && real < 0)
			real += 256;
		if (signed && approx < 0)
			approx += 256;
		if (real > approx) {
			ma = real;
			mi = approx;
		} else {
			ma = approx;
			mi = real;
		}
		if (ma - mi > parTolerance)
			return false;
		else
			return true;
	}

	private static int[] buildTable2(double a, double k, int supersamplefactor) {
		// y = 2^a x^b donde b = k+1
		// a ~ cantidad de bits a recortar (0-4) k ~ alinealidad (0-4)
		// ej: (a=0,k=0) => lineal (exacta)
		// ej: (a=1,k=0) => corta un bit (0 2 4 6...)
		// ej: (a=2,k=0) => corta dos bits (0 4 8 12 ...)
		// (a=0 k=1) =>
		// (a=1 k=1) =>
		int[] tab = new int[256];
		// double b = Math.log(k + 1.0)/Math.log(2.0);
		double b = k + 1;
		int i, j, in, i0;
		double y1, y0 = 0.0, ym, x, dx;
		Arrays.fill(tab, (int) 0);
		dx = 1.0 / Math.pow(2.0, supersamplefactor); // arranca en 1 si supersamplefactor=0
		for (x = dx; x < 129; x += dx) {
			if (x >= 0.99)
				dx = 1.0;
			y1 = x >= 0.99 ? Math.pow(2.0, a) * Math.pow(x, b) : Math.pow(2.0, a) * x;
			ym = (y1 + y0) / 2.0;
			j = (int) (y1 + 0.49); // tab[i] = j
			if (j > 128)
				j = 128;
			i0 = (int) (ym + 1.1);
			for (i = i0; i <= 128; i++) {
				in = 256 - i;
				if (i < 128)
					tab[i] = j;
				if (in >= 128)
					tab[in] = (256 - j);
			}
			if (i0 >= 128)
				break;
			y0 = y1;
		}
		return tab;
	}

	public static void printTable(int[] t, boolean oneline) {
		if (oneline) {
			System.out.println(Arrays.toString(t));
			return;
		}
		for (int i = 0; i < 256; i++) {
			System.out.println(i + " " + t[i]);
		}
	}

	public void showStatR() {
		double ac0 = 0, ac1 = 0;
		for (int i = 0; i < 256; i++) {
			ac0 += statR0[i];
			ac1 += statR1[i];
		}
		ac0 = ac0 > 0.1 ? 1.0 / ac0 : 0;
		ac1 = ac1 > 0.1 ? 1.0 / ac1 : 0;
		double h0 = 0, h1 = 0;
		for (int i = 0; i < 256; i++) {
			if (statR0[i] > 0)
				h0 += (statR0[i] * ac0) * Math.log(statR0[i] * ac0);
			if (statR1[i] > 0)
				h1 += (statR1[i] * ac1) * Math.log(statR1[i] * ac1);
		}
		h0 /= -Math.log(2.0);
		h1 /= -Math.log(2.0);
		System.out.printf("H0=%.4f H1=%.4f\n", h0, h1);

		for (int i = -128; i < 128; i++) {
			System.out.printf("%d %.4f %.4f\n", i, statR0[(i + 256) % 256] * ac0, statR1[(i + 256) % 256] * ac1);
		}
	}

	public void reportOriginalR(int r0, int row, int col) {
		reportOriginalR(r0, r0,row, col); 
	}

	public void reportOriginalR(int r0, int r0orig,int row, int col) {
		activity = parMemory * activity + Math.abs(r0orig) * (1 - parMemory);
		if (WRITE_LOSS_IMG_INFO) {
			if (col < pngw.imgInfo.cols) {
				// write lossy info
				imgline.setRown(row);
				double r0x = activity;
				// double r0x = r0;
				int r = (int) (r0x * 32);
				if (r < 0)
					r = -r;
				if (r > 255)
					r = 255;
				int g = (int) (Math.pow(Math.abs(r0x), 1.5) * 4);
				if (g > 255)
					g = 255;
				ImageLineHelper.setPixelRGB8(imgline, col, r, g, r0x < 0 ? 64 : 0);
				if (col == pngw.imgInfo.cols - 1) {
					pngw.writeRow(imgline);
					if (row == pngw.imgInfo.rows - 1)
						pngw.end();
				}
			}
		}
		statR0[(r0 + 256) % 256]++;
	}

	public void reportFinalR(int r1, int row, int col) {
		statR1[(r1 + 256) % 256]++;
	}

	// /// error difussion no used ///////

	public void initErrorDif() {
		errordif = new ErrorDifussionFloydSteinberg(imginfo, 0);
	}

	public int getDiffusedErrorToAdd(int row, int col) {
		if (errordif == null)
			return 0;
		else
			return errordif.getTotalErr(row, col);
	}

	public void writeErrorToDiffuse(int row, int col, int err) {
		if (errordif != null)
			errordif.addErr(row, col, err);
	}

	public void resetErrorDiffussion() {
		if (errordif != null)
			errordif.reset();
	}

	// ////////

	public String toString() {
		return String.format("lossy=%d a=%.3f k=%.3f mem=%.4f threshold=%.4f tolerance=%d trim=%d detail=%s gradient=%s", parLossy,
				parTableQuantA, parTableQuantK, parMemory, parActivityThreshold, parTolerance, parTrimPrediction,cd(parDetail),cd(parGradient));
	}

	public String toStringCod() {
		return String.format("%03d_%s_%s_%s_%s_%d_%d_%s_%s", parLossy, cd(parTableQuantA), cd(parTableQuantK), cd(parMemory),
				cd(parActivityThreshold), parTolerance, parTrimPrediction,cd(parDetail),cd(parGradient));
	}

	private static String cd(double d) { // codifica double como dx100
		return String.valueOf((int) (d * 100 + 0.5));
	}

	public static void showTable(int[] t) {
		for (int i = 0; i < 256; i++) {
			System.out.printf("%d %d\n", i, t[i]);
		}
	}

	/** test */
	public static void encode(String orig, int lossy) {

		long t0 = System.currentTimeMillis();

		String suffix = "lossy" + lossy;
		String dest = orig.replaceAll("\\.png$", "") + "_" + suffix + ".png";

		PngReader pngr = FileHelper.createPngReader(new File(orig));
		System.out.println(pngr.imgInfo);
		File destf = new File(dest);
		PngWriterLossy pngw = new PngWriterLossy(FileHelper.openFileForWriting(destf, true), pngr.imgInfo, destf.getName());
		pngw.lossyHelper.setLossy(lossy);
		pngw.copyChunksFirst(pngr, ChunksToWrite.COPY_ALL_SAFE | ChunksToWrite.COPY_PALETTE);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			pngw.writeRow(l1);
		}
		String lossydesc = pngw.lossyHelper.toString();
		PngChunkTEXT txtChunk = (PngChunkTEXT) PngChunk.factoryFromId(ChunkHelper.tEXt_TEXT, pngw.imgInfo);
		txtChunk.setKeyVal("description", lossydesc);
		pngw.chunks.cloneAndAdd(txtChunk, true);
		pngr.end();
		pngw.copyChunksLast(pngr, ChunksToWrite.COPY_ALL_SAFE);
		pngw.end();
		suffix = "lossy" + pngw.lossyHelper.toStringCod();
		String destfinal = orig.replaceAll("\\.png$", "") + "_" + suffix + ".png";
		File fd = new File(destfinal);
		if (fd.exists())
			fd.delete();
		new File(dest).renameTo(fd);

		long t1 = System.currentTimeMillis();
		long size0 = (new File(orig)).length();
		long size1 = (new File(dest)).length();
		double sizerel = (size1 * 1000.0) / size0;
		System.out.printf("%s\t%d\t%.2f\n", dest, (t1 - t0), sizerel);
		pngw.lossyHelper.showStatR();
		System.out.println("tabla 1");
		printTable(pngw.lossyHelper.tablequant1, false);
		System.out.println("tabla 2");
		printTable(pngw.lossyHelper.tablequant2, false);
		System.out.println(pngw.lossyHelper);

	}



	public static void main(String[] args) throws Exception {
		encode("/temp/balcony.png", 10);
		// test0(10,2);
		// int[] tab = buildTable2(3.0, 1.0,2);
		// showTable(tab);
	}

}
