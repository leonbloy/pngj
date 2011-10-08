package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;
import ar.com.hjg.pngj.PngjException;

/*
 */
public class PngChunkSBIT extends PngChunk {
	// http://www.w3.org/TR/PNG/#11sBIT
	// this chunk structure depends on the image type

	//	significant bits
	private int graysb,alphasb; 
	private int redsb, greensb, bluesb;
	
	public PngChunkSBIT(ImageInfo info) {
		super(ChunkHelper.sBIT_TEXT, info);
	}

	private int getLen() {
		int len = imgInfo.greyscale ? 1 : 3;
		if(imgInfo.alpha) len +=1;
		return len;
	}
	
	@Override
	public void parseFromChunk(ChunkRaw c) {
		if (c.len != getLen())
			throw new PngjException("bad chunk length " + c);
		if (imgInfo.greyscale) {
			graysb = PngHelper.readInt1fromByte(c.data, 0);
			if(imgInfo.alpha)
				alphasb = PngHelper.readInt1fromByte(c.data, 1);
		} else {
			redsb = PngHelper.readInt1fromByte(c.data, 0);
			greensb = PngHelper.readInt1fromByte(c.data, 1);
			bluesb = PngHelper.readInt1fromByte(c.data, 2);
			if(imgInfo.alpha)
				alphasb = PngHelper.readInt1fromByte(c.data, 3);
		}
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = null;
		c = createEmptyChunk(getLen(), true);
		if (imgInfo.greyscale) {
			c.data[0] = (byte) graysb;
			if(imgInfo.alpha)
				c.data[1] = (byte) alphasb;
		} else {
			c.data[0] = (byte) redsb;
			c.data[1] = (byte) greensb;
			c.data[2] = (byte) bluesb;
			if(imgInfo.alpha)
				c.data[3] = (byte) alphasb;
		}
		return c;
	}


	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkSBIT otherx = (PngChunkSBIT) other;
		graysb = otherx.graysb;
		redsb = otherx.redsb;
		greensb = otherx.greensb;
		bluesb = otherx.bluesb;
		alphasb = otherx.alphasb;
	}
}
