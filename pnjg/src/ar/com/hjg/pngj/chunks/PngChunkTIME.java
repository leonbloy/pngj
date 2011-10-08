package ar.com.hjg.pngj.chunks;

import java.util.Calendar;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;
import ar.com.hjg.pngj.PngjException;

public class PngChunkTIME extends PngChunk {
	// http://www.w3.org/TR/PNG/#11tIME
	public int year, mon, day, h, m, s;

	public PngChunkTIME(ImageInfo info) {
		super(ChunkHelper.tIME_TEXT, info);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = createEmptyChunk(7, true);
		PngHelper.writeInt2tobytes(year, c.data, 0);
		c.data[2] = (byte) mon;
		c.data[3] = (byte) day;
		c.data[4] = (byte) h;
		c.data[5] = (byte) m;
		c.data[6] = (byte) s;
		return c;
	}

	@Override
	public void parseFromChunk(ChunkRaw chunk) {
		if (chunk.len != 7)
			throw new PngjException("bad chunk " + chunk);
		year = PngHelper.readInt2fromBytes(chunk.data, 0);
		mon = PngHelper.readInt1fromByte(chunk.data, 2);
		day = PngHelper.readInt1fromByte(chunk.data, 3);
		h = PngHelper.readInt1fromByte(chunk.data, 4);
		m = PngHelper.readInt1fromByte(chunk.data, 5);
		s = PngHelper.readInt1fromByte(chunk.data, 6);
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkTIME x = (PngChunkTIME) other;
		year = x.year;
		mon = x.mon;
		day = x.day;
		h = x.h;
		m = x.m;
		s = x.s;
	}

	public void setNow(int secsAgo) {
		Calendar d = Calendar.getInstance();
		d.setTimeInMillis(System.currentTimeMillis() - 1000 * (long) secsAgo);
		year = d.get(Calendar.YEAR);
		mon = d.get(Calendar.MONTH) + 1;
		day = d.get(Calendar.DAY_OF_MONTH);
		h = d.get(Calendar.HOUR_OF_DAY);
		m = d.get(Calendar.MINUTE);
		s = d.get(Calendar.SECOND);
	}
}
