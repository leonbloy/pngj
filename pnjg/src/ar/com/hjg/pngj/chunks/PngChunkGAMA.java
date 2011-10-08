package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;
import ar.com.hjg.pngj.PngjException;

/*
 */
public class PngChunkGAMA extends PngChunk {
	// http://www.w3.org/TR/PNG/#11gAMA
	private double gamma;

	public PngChunkGAMA(ImageInfo info) {
		super(ChunkHelper.gAMA_TEXT, info);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = createEmptyChunk(4, true);
		int g = (int) (gamma * 100000 + 0.5);
		PngHelper.writeInt4tobytes(g, c.data, 0);
		return c;
	}

	@Override
	public void parseFromChunk(ChunkRaw chunk) {
		if (chunk.len != 4)
			throw new PngjException("bad chunk " + chunk);
		int g = PngHelper.readInt4fromBytes(chunk.data, 0);
		gamma = ((double) g) / 100000.0;
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		gamma = ((PngChunkGAMA) other).gamma;
	}

	public double getGamma() {
		return gamma;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}
}
