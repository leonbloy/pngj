package ar.com.hjg.pngj.chunks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelper;

/**
 * Chunks to be written - not including IDHR/ IDAT / END (buy yes PLTE)
 * http://www.w3.org/TR/PNG/#table53
 * 
 **/
public class ChunksToWrite {
	public static final int COPY_NONE = 0;
	public static final int COPY_PALETTE = 1; // only for indexed - use COPY_ALL
																						// for copy palette for RGB
	public static final int COPY_ALL_SAFE = 1 << 2;
	public static final int COPY_ALL = 1 << 3; // includes palette!
	public static final int COPY_PHYS = 1 << 4; // dpi
	public static final int COPY_TEXTUAL = 1 << 5; // all textual types
	public static final int COPY_TRANSPARENCY = 1 << 6; //

	public static boolean maskMatch(int v, int mask) {
		return (v & mask) != 0;
	}

	public final ImageInfo imgInfo;
	// ancillary chunks single
	private Map<String, PngChunk> chunks1 = new LinkedHashMap<String, PngChunk>();
	// other chunks (possibly multiple): text, splt, custom
	private List<PngChunk> chunks2 = new ArrayList<PngChunk>();
	private PngChunkPLTE palette = null;

	public ChunksToWrite(ImageInfo info) {
		this.imgInfo = info;
	}

	public List<PngChunk> getPending() { // can include palette
		List<PngChunk> li = new ArrayList<PngChunk>();
		for (PngChunk c : chunks1.values())
			if (c.getWriteStatus() <= 1)
				li.add(c);
		if (palette != null && palette.getWriteStatus() <= 1)
			li.add(palette);
		for (PngChunk c : chunks2)
			if (c.getWriteStatus() <= 1)
				li.add(c);
		Collections.sort(li, new Comparator<PngChunk>() {
			public int compare(PngChunk o1, PngChunk o2) {
				if (o1.getWriteStatus() != o2.getWriteStatus())
					return o1.getWriteStatus() < o2.getWriteStatus() ? 1 : -1;
				else
					return compareInt(o1.writeOrder(), o2.writeOrder());
			}
		});
		return li;
	}

	private static int compareInt(int a, int b) {
		return a < b ? -1 : 1;
	}

	public void setPHYS(long resx, long resy, int unit) {
		if (!chunks1.containsKey(ChunkHelper.pHYs_TEXT))
			chunks1.put(ChunkHelper.pHYs_TEXT, new PngChunkPHYS(imgInfo));
		PngChunkPHYS chunk = (PngChunkPHYS) chunks1.get(ChunkHelper.pHYs_TEXT);
		chunk.pixelsxUnitX = resx;
		chunk.pixelsxUnitY = resy;
		chunk.units = unit;
	}

	public void setPHYSdpi(double dpi) {
		long res = PngHelper.resDpiToMeters(dpi);
		setPHYS(res, res, 1);
	}

	public void copyPalette(PngChunkPLTE p) {
		palette = PngChunk.cloneChunk(p, imgInfo);
	}

	public void cloneAndAdd(PngChunk p, boolean overwrite) {
		if (p instanceof PngChunkPLTE) {
			if (palette != null && !overwrite)
				return;
			copyPalette((PngChunkPLTE) p);
		} else if (ChunkHelper.admitsMultiple(p.id)) {
			chunks2.add(PngChunk.cloneChunk(p, imgInfo));
		} else { // singletons: checks for existence
			if (chunks1.containsKey(p.id) && !overwrite)
				return;
			else
				chunks1.put(p.id, PngChunk.cloneChunk(p, imgInfo));
		}
	}
}
