package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngFilterType;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.nosandbox.FileHelper;

/**
 * 
 */
public class ShowFilterInfo {
	
	public static void show2(File file) {
		PngReader pngr = FileHelper.createPngReader(file);
		PngFilterType[] types= new PngFilterType[pngr.imgInfo.rows];
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine imline = pngr.readRow(row);
			types[row]=imline.filterUsed;
		}
		pngr.end();
		System.out.println(pngr.toString());
		for(int r=0;r<pngr.imgInfo.rows; r++) {
			if(r==0  || types[r] != types[r-1])
				System.out.println("row="+r +" t="+types[r]);
		}
	}

	public static void main(String[] args) throws Exception {
		show2(new File("/temp/treetext-FULL-lossy-30.png"));
		
	}

}
