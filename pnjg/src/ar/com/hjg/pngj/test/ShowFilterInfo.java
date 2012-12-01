package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngReader;

/**
 * 
 */
public class ShowFilterInfo {

	public static void show2(File file) {
		PngReader pngr = FileHelper.createPngReader(file);
		FilterType[] types = new FilterType[pngr.imgInfo.rows];
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine imline = pngr.readRowByte(row);
			types[row] = imline.getFilterUsed();
		}
		pngr.end();
		System.out.println(pngr.toString());
		for (int r = 0; r < pngr.imgInfo.rows; r++) {
			if (r == 0 || types[r] != types[r - 1])
				System.out.println("row=" + r + " t=" + types[r]);
		}
	}

	public static void main(String[] args) throws Exception {
		show2(new File(args[0]));
	}

}
