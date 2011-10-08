package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;

/*
 */
public class PngChunkBKGD extends PngChunk {
	// http://www.w3.org/TR/PNG/#11bKGD
	// this chunk structure depends on the image type
	// only one of these is meaningful
	private int gray;
	private int red, green, blue;
	private int paletteIndex;

	public PngChunkBKGD(ImageInfo info) {
		super(ChunkHelper.bKGD_TEXT, info);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = null;
		if (imgInfo.greyscale) {
			c = createEmptyChunk(2, true);
			PngHelper.writeInt2tobytes(gray, c.data, 0);
		} else if (imgInfo.indexed) {
			c = createEmptyChunk(1, true);
			c.data[0] = (byte) paletteIndex;
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
			paletteIndex = (int) (c.data[0] & 0xff);
		} else {
			red = PngHelper.readInt2fromBytes(c.data, 0);
			green = PngHelper.readInt2fromBytes(c.data, 2);
			blue = PngHelper.readInt2fromBytes(c.data, 4);
		}
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkBKGD otherx = (PngChunkBKGD) other;
		gray = otherx.gray;
		red = otherx.red;
		green = otherx.red;
		blue = otherx.red;
		paletteIndex = otherx.paletteIndex;
	}
}
