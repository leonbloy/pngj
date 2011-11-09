package ar.com.hjg.pngj;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.print.attribute.standard.Sides;

import ar.com.hjg.pngj.chunks.ChunksToWrite;
import ar.com.hjg.pngj.nosandbox.FileHelper;

/**
 */
public class LossyHelper {



	/** first index: quantization step ; second: offset: how many sample before the quantization center this step starts (offset shoud be between 0 and step/2 
	 * eg
	 * tx = {{4,1},{6,3},{8,4},{9,5},{12,6}};
	 *      [0, 0, 0, 4, 4, 4, 4, 10, 10, 10, 10, 10, 10, 10, 18, 18, 18, 18, 18, 18, 18, 18, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 51, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 63, 75, 75, 75, 75, 75, 75, 75, 75, 75, 75, 75, 75, 87, 87, 87, 87, 87, 87, 87, 87, 87, 87, 87, 87, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 123, 123, 123, 123, 123, 123, 123, 123, 123, 123, 123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
     *                   *                     *                
	 * */

	static int[][] tx8 =  {{4,2},{8,3},{12,5}};
	static int[][] tx7 =  {{4,1},{6,3},{8,4},{9,5},{12,6}};
	static int[][] tx6 =  {{3,1},{3,1},{5,2},{5,2},{10,5}}; 
	static int[][] tx5 =  {{2,0},{2,0},{4,1},{4,1},{4,1},{8,2}};
	static int[][] tx4 =  {{2,0},{2,0},{4,0}}; 
	static int[][] tx3 =  {{1,0},{2,0},{3,1},{4,1},{4,1},{4,1},{4,1},{8,2}}; 
	static int[][] tx2 =  {{1,0},{2,0},{2,0},{2,0},{3,1},{4,1}}; 
	static int[][] tx1 =  {{1,0},{2,0}};
	static int[][] tx0 =  {{1,0}};
	
	public final int lossy;
	private final boolean signed;
	private Map<PngFilterType,int[]> tablequant = new HashMap<PngFilterType,int[]>(); 
	private int tolerance = 10;
	
	public LossyHelper(int lossy,boolean signed) {
		super();
		this.lossy = lossy;
		this.signed = signed;
		
	}

	public int quantize(int x,PngFilterType filterType) {
		if(! tablequant.containsKey(filterType)) {
			tablequant.put(filterType,buildTable(filterType));
		}
		int y  =tablequant.get(filterType)[signed && x<0 ? x+256 : x];
		return signed && y>127 ? y-256 : y; 
	}
	

	public boolean isacceptable(int real, int approx) {
		int ma, mi;
		if(signed && real<0) real +=256;
		if(signed && approx<0) approx +=256;
		if (real > approx) {
			ma = real;
			mi = approx;
		} else {
			ma = approx;
			mi = real;
		}
		if (ma - mi > tolerance)
			return false;
		else
			return true;
	}
	
	/**
	 * x is in (0:255) , the posterize tries to round its value, interpreted as as signed int (towards 0 or 255) nbits:
	 * 678
	 * @param filterType 
	 * @return 
	 */
	private int[] buildTable(PngFilterType filterType) {
			int[] tablequantx; 
			int corr = 0;
			/*if(filterType == PngFilterType.FILTER_AVERAGE) {
				corr = lossy <4? 1 : 2;
				if(lossy>7) corr++;
			}*/
			if (lossy == 0)
				tablequantx = regenerateTable(tx0, tx0,corr); 
			else if (lossy == 1)
				tablequantx= regenerateTable(tx1,tx1,corr);
			else if (lossy == 2)
				tablequantx= regenerateTable(tx2,tx2,corr);
			else if (lossy == 3)
				tablequantx= regenerateTable(tx3,tx3,corr);
			else if (lossy == 4)
				tablequantx = regenerateTable(tx4, tx4,corr); 
			else if (lossy == 5)
				tablequantx = regenerateTable(tx5, tx5,corr); 
			else if (lossy == 6)
				tablequantx = regenerateTable(tx6, tx6,corr); 
			else if (lossy == 7)
				tablequantx = regenerateTable(tx7, tx7,corr); 
			else
				tablequantx = regenerateTable(tx8, tx8,corr); 
			return tablequantx ;
	
	}

	
	private static int[] regenerateTable(int[][] tp,int[][] tn,int asymcorr) {
		int tab[]=new int[256];
		int k=0,j,a,off,center;
		j=-1;
		center=0;
		while(center<128) {
			j++; if(j>=tp.length) j--;
			a=tp[j][0];
			off=tp[j][1];
			center += a;
			for(k=center-off;k<=128;k++) {
				if(k<=0) throw new RuntimeException("?");
				tab[k]=center;//not very efficient, but nice nvertheles
			}
		}
		j=-1;  // samefor negative indexes
		center=0;
		while(center<128) {
			j++; if(j>=tp.length) j--;
			a=tn[j][0];
			if(j==0) a+=asymcorr;
			off=tn[j][1];
			center += a;
			for(k=center-off;k<=128;k++) {
				if(k<=0) throw new RuntimeException("?");
				tab[(256-k)]=(256-center)%256;//not very efficient, but nice nvertheles
			}
		}
		return tab;
	}
	

	
	public static void encode(String orig, int lossy, PngFilterType filterType) {
		long t0 = System.currentTimeMillis();
		String suffix = "lossy" + lossy;
		String dest = orig.replaceAll("\\.png$", "") + "_" + suffix + ".png";
		PngReader pngr = FileHelper.createPngReader(new File(orig));
		System.out.println(pngr.imgInfo);
		File destf = new File(dest);
		PngWriter2 pngw = new PngWriter2(FileHelper.openFileForWriting(destf, true), pngr.imgInfo, destf.getName());

		pngw.setFilterType(filterType);
		pngw.setCompLevel(9);
		pngw.setLossyness(lossy);
		pngw.copyChunksFirst(pngr, ChunksToWrite.COPY_ALL_SAFE | ChunksToWrite.COPY_PALETTE);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			pngw.writeRow(l1);
		}
		pngr.end();
		pngw.copyChunksLast(pngr, ChunksToWrite.COPY_ALL_SAFE);
		pngw.end();
		long t1 = System.currentTimeMillis();
		long size0 = (new File(orig)).length();
		long size1 = (new File(dest)).length();
		double sizerel = (size1 * 1000.0) / size0;
		System.out.printf("%s\t%d\t%.2f\n", dest, (t1 - t0), sizerel);
		//pngw.showStatR();
	}


	public static void printTable(int[] t,boolean oneline) {
		if(oneline) { System.out.println(Arrays.toString(t)); return; }
		for(int i=0;i<256;i++) {
			System.out.println(i + " " + t[i]);
		}
	}

	
   public static void main(String[] args) throws Exception {
	   int[] t = regenerateTable(tx5, tx5,1);
	   printTable(t, false);
	}
}
