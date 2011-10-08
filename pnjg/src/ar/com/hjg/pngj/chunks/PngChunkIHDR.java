package ar.com.hjg.pngj.chunks;

import java.io.ByteArrayInputStream;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;
import ar.com.hjg.pngj.PngjException;

public class PngChunkIHDR extends PngChunk {
	public int cols;
	public int rows;
	public int bitspc;
	public int colormodel;
	public int compmeth;
	public int filmeth;
	public int interlaced;

	// http://www.w3.org/TR/PNG/#11IHDR
	//
	public PngChunkIHDR(ImageInfo info) {
		super(ChunkHelper.IHDR_TEXT, info);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = new ChunkRaw(13, ChunkHelper.IHDR, true);
		int offset = 0;
		PngHelper.writeInt4tobytes(cols, c.data, offset);
		offset += 4;
		PngHelper.writeInt4tobytes(rows, c.data, offset);
		offset += 4;
		c.data[offset++] = (byte) bitspc;
		c.data[offset++] = (byte) colormodel;
		c.data[offset++] = (byte) compmeth;
		c.data[offset++] = (byte) filmeth;
		c.data[offset++] = (byte) interlaced;
		return c;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		if (c.len != 13)
			throw new PngjException("Bad IDHR len " + c.len);
		ByteArrayInputStream st = c.getAsByteStream();
		cols = PngHelper.readInt4(st);
		rows = PngHelper.readInt4(st);
		// bit depth: number of bits per channel
		bitspc = PngHelper.readByte(st);
		colormodel = PngHelper.readByte(st);
		compmeth = PngHelper.readByte(st);
		filmeth = PngHelper.readByte(st);
		interlaced = PngHelper.readByte(st);
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkIHDR otherx = (PngChunkIHDR) other;
		cols = otherx.cols;
		rows = otherx.rows;
		bitspc = otherx.bitspc;
		colormodel = otherx.colormodel;
		compmeth = otherx.compmeth;
		filmeth = otherx.filmeth;
		interlaced = otherx.interlaced;
	}
}
