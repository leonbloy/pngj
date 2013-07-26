package ar.com.hjg.pngj;

/**
 * A "image line" (or row) providing methods for reading and writing to PNG raw
 * image data. Notice that this is does not make any assumptions of an
 * underlying storage. The user of this library will normally cast to a more
 * concrete implementation, as {@link ImageLineInt} or {@link ImageLineByte}
 * 
 */
public interface IImageLine {

	/**
	 * Extract pixels from a raw unlfilterd PNG row. Len is the total amount of
	 * bytes in the array, including the first byte (filter type)
	 * 
	 * offset and step (0 and 1 for non interlaced) are in PIXELS
	 * 
	 * Notice that when step!=1 the data is partial, this method will be called
	 * several times
	 */
	void readFromPngRaw(byte[] raw, int len, int offset, int step);

	/**
	 * This is called when the read for the line has been completed (eg for
	 * interlaced). It's called exactly once for each line. This is provided in
	 * case the class needs to to some postprocessing.
	 */
	void endReadFromPngRaw();

	/**
	 * Writes the line to a PNG raw byte array, in the unfiltered PNG format
	 * Notice that the first byte is the filter type, you should write it only
	 * if you know it.
	 * 
	 */
	void writeToPngRaw(byte[] raw);

}
