package ar.com.hjg.pngj.chunks;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngjException;

/**
 * tIME chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11tIME
 */
public class PngChunkTIME extends PngChunkSingle {
	public final static String ID = ChunkHelper.tIME;

	// http://www.w3.org/TR/PNG/#11tIME
	private int year, mon, day, hour, min, sec;

	public PngChunkTIME(ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.NONE;
	}

	@Override
	public ChunkRaw createRawChunk() {
		ChunkRaw c = createEmptyChunk(7, true);
		PngHelperInternal.writeInt2tobytes(year, c.data, 0);
		c.data[2] = (byte) mon;
		c.data[3] = (byte) day;
		c.data[4] = (byte) hour;
		c.data[5] = (byte) min;
		c.data[6] = (byte) sec;
		return c;
	}

	@Override
	public void parseFromRaw(ChunkRaw chunk) {
		if (chunk.len != 7)
			throw new PngjException("bad chunk " + chunk);
		year = PngHelperInternal.readInt2fromBytes(chunk.data, 0);
		mon = PngHelperInternal.readInt1fromByte(chunk.data, 2);
		day = PngHelperInternal.readInt1fromByte(chunk.data, 3);
		hour = PngHelperInternal.readInt1fromByte(chunk.data, 4);
		min = PngHelperInternal.readInt1fromByte(chunk.data, 5);
		sec = PngHelperInternal.readInt1fromByte(chunk.data, 6);
	}

	public void setNow(int secsAgo) {
		LocalDateTime d = LocalDateTime.ofEpochSecond(
				System.currentTimeMillis() / 1000 - secsAgo, 0, ZoneOffset.UTC);
		year = d.getYear();
		mon = d.getMonthValue();
		day = d.getDayOfMonth();
		hour = d.getHour();
		min = d.getMinute();
		sec = d.getSecond();
	}

	public void setYMDHMS(int yearx, int monx, int dayx, int hourx, int minx, int secx) {
		year = yearx;
		mon = monx;
		day = dayx;
		hour = hourx;
		min = minx;
		sec = secx;
	}

	public int[] getYMDHMS() {
		return new int[] { year, mon, day, hour, min, sec };
	}

	/** format YYYY/MM/DD HH:mm:SS */
	public String getAsString() {
		return String.format("%04d/%02d/%02d %02d:%02d:%02d", year, mon, day, hour, min, sec);
	}

}
