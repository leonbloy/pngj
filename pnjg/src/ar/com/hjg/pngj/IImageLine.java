package ar.com.hjg.pngj;

public interface IImageLine {
	/**
	 * Extract pixels from a raw PNG row. Len is the total amount of bytes in the array,
	 * including the first (filter)
	 * 
	 * offset and step (0 and 1 for non interlaced) are in PIXELS
	 * 
	 * Notice that when step!=1 the data is partial, this method will be called several
	 * times
	 */
	void fromPngRaw(byte[] raw, int len, int offset, int step);
	

	void end();
	
	/**
	 * raw[0] will be ignored
	 */
	void toPngRaw(byte[] raw);
	
}
