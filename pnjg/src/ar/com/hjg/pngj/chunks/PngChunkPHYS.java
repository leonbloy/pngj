package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;
import ar.com.hjg.pngj.PngjException;

public class PngChunkPHYS extends PngChunk {
	// http://www.w3.org/TR/PNG/#11pHYs
	public long pixelsxUnitX;
	public long pixelsxUnitY;
	public int units; // 0: unknown 1:metre

	public PngChunkPHYS(ImageInfo info) {
		super(ChunkHelper.pHYs_TEXT, info);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = createEmptyChunk(9, true);
		PngHelper.writeInt4tobytes((int) pixelsxUnitX, c.data, 0);
		PngHelper.writeInt4tobytes((int) pixelsxUnitY, c.data, 4);
		c.data[8] = (byte) units;
		return c;
	}

	@Override
	public void parseFromChunk(ChunkRaw chunk) {
		if (chunk.len != 9)
			throw new PngjException("bad chunk length " + chunk);
		pixelsxUnitX = PngHelper.readInt4fromBytes(chunk.data, 0);
		if (pixelsxUnitX < 0)
			pixelsxUnitX += 0x100000000L;
		pixelsxUnitY = PngHelper.readInt4fromBytes(chunk.data, 4);
		if (pixelsxUnitY < 0)
			pixelsxUnitY += 0x100000000L;
		units = PngHelper.readInt1fromByte(chunk.data, 8);
	}

	// returns -1 if not in meters, or not equal
	public double getAsDpi() {
		if (units != 1 || pixelsxUnitX != pixelsxUnitY)
			return -1;
		return ((double) pixelsxUnitX) * 0.0254;
	}

	public void setAsDpi(double dpi) {
		units = 1;
		pixelsxUnitX = (long) (dpi / 0.0254 + 0.5);
		pixelsxUnitY = pixelsxUnitX;
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkPHYS otherx = (PngChunkPHYS) other;
		this.pixelsxUnitX = otherx.pixelsxUnitX;
		this.pixelsxUnitY = otherx.pixelsxUnitY;
		this.units = otherx.units;
	}
}
