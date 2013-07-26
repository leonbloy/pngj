package ar.com.hjg.pngj;

import java.io.File;
import java.io.InputStream;

/**
 * This is esentially the same as PngReader, only that it provides some methods
 * that know the concrete ImageLine implementation
 */
public class PngReaderInt extends PngReader {

	public PngReaderInt(File file) {
		super(file);
	}

	public PngReaderInt(InputStream inputStream) {
		super(inputStream);
	}

	/**
	 * Utility method that casts the IImageLine to a ImageLineInt
	 * 
	 * This only make sense for this concrete class
	 * 
	 * @return
	 */
	public final ImageLineInt readRowInt() {
		IImageLine line = readRow();
		if (line instanceof ImageLineInt)
			return (ImageLineInt) line;
		else
			throw new PngjException("This is not a ImageLineInt : " + line.getClass());
	}
}
