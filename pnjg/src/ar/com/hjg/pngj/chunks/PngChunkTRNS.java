package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;

/*
 */
public class PngChunkTRNS extends PngChunk {
	// http://www.w3.org/TR/PNG/#11tRNS
	// this chunk structure depends on the image type
	// only one of these is meaningful
	private int gray;
	private int red, green, blue;
	private int[] paletteAlpha;

	public PngChunkTRNS(ImageInfo info) {
		super(ChunkHelper.tRNS_TEXT, info);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = null;
		if (imgInfo.greyscale) {
			c = createEmptyChunk(2, true);
			PngHelper.writeInt2tobytes(gray, c.data, 0);
		} else if (imgInfo.indexed) {
			c = createEmptyChunk(paletteAlpha.length, true);
			for (int n = 0; n < c.len; n++) {
				c.data[n] = (byte) paletteAlpha[n];
			}
		} else {
			c = createEmptyChunk(6, true);
			PngHelper.writeInt2tobytes(red, c.data, 0);
			PngHelper.writeInt2tobytes(green, c.data, 0);
			PngHelper.writeInt2tobytes(blue, c.data, 0);
		}
		return c;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		if (imgInfo.greyscale) {
			gray = PngHelper.readInt2fromBytes(c.data, 0);
		} else if (imgInfo.indexed) {
			int nentries = c.data.length;
			paletteAlpha = new int[nentries];
			for (int n = 0; n < nentries; n++) {
				paletteAlpha[n] = (int) (c.data[n] & 0xff);
			}
		} else {
			red = PngHelper.readInt2fromBytes(c.data, 0);
			green = PngHelper.readInt2fromBytes(c.data, 2);
			blue = PngHelper.readInt2fromBytes(c.data, 4);
		}
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkTRNS otherx = (PngChunkTRNS) other;
		gray = otherx.gray;
		red = otherx.red;
		green = otherx.red;
		blue = otherx.red;
		paletteAlpha = otherx.paletteAlpha; // not deep!
	}
}
