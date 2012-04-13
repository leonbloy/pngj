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
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkGAMA;

/**
 * Remove GAMA chunk, if present
 */
public class RemoveGama {
	public static void removeGama(String origFilename, String destFilename) {
		PngReader pngr = FileHelper.createPngReader(new File(origFilename));
		PngWriter pngw = FileHelper.createPngWriter(new File(destFilename), pngr.imgInfo, true);
		pngw.setFilterType(FilterType.FILTER_AGGRESSIVE);
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL);
		PngChunkGAMA gama = (PngChunkGAMA) pngw.getChunkList().getQueuedById1(ChunkHelper.gAMA);
		if(gama != null) {
			System.out.println("removing gama chunk gamma=" + gama.getGamma());
			pngw.getChunkList().removeChunk(gama);
		}
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			pngw.writeRow(l1,row);
		}
		pngr.end();
		// just in case some new metadata has been read
		pngw.copyChunksLast(pngr, ChunkCopyBehaviour.COPY_ALL);
		pngw.end();
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Arguments: [pngsrc] [pngdest]");
			System.exit(1);
		}
		removeGama(args[0], args[1]);
		System.out.println("Done");
	}
}
