package ar.com.hjg.pngj.test;

import java.io.File;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.chunks.ChunkLoadBehaviour;

/**
 * prints all chunks (remember that IDAT is shown as only one pseudo zero-length chunk)
 */
public class ShowChunks {

	public static void showChunks(String file) {
		PngReader pngr = FileHelper.createPngReader(new File(file));
		pngr.setChunkLoadBehaviour(ChunkLoadBehaviour.LOAD_CHUNK_ALWAYS);
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
		}
		pngr.end();
		System.out.println(pngr.toString());
		System.out.println(pngr.getChunksList().toStringFull());
	}

	public static void main(String[] args) throws Exception {
		showChunks(args.length > 0 ? args[0] : "/temp/testpal.png");
	}
}
