package ar.com.hjg.pngj.samples;

import java.io.File;

import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkPredicate;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkGAMA;

/**
 * Remove GAMA chunk, if present
 * 
 * These could be done more efficienty, by treating IDAT chunks as ancillary chunks
 * See ChunkSeqBasicTest for an example
 * 
 */
public class SampleRemoveGama {

	public static void convert(String origFilename, String destFilename) {
		PngReader pngr = new PngReader(new File(origFilename));
		PngWriter pngw = new PngWriter(new File(destFilename), pngr.imgInfo, false);
		pngw.setFilterPreserve(true);
		pngw.copyChunksFrom(pngr.getChunksList(), new ChunkPredicate() {
			public boolean match(PngChunk chunk) {
				return chunk.id.equals(PngChunkGAMA.ID);
			}
		}); 
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			pngw.writeRow(pngr.readRow(row));
		}
		pngw.end();
		pngr.end();
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2 || args[0].equals(args[1])) {
			System.err.println("Copies image removing gamma chunk (no color correction)");
			System.err.println("Arguments: [pngsrc] [pngdest]");
			System.exit(1);
		}
		convert(args[0], args[1]);
		System.out.println("Done. Result in " + args[1]);
	}
}
