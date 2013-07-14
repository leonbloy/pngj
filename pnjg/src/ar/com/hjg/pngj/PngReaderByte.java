package ar.com.hjg.pngj;

import java.io.File;
import java.io.InputStream;

public class PngReaderByte extends PngReader {

	public PngReaderByte(File file) {
		super(file);
		setImageLineFactory(ImageLineByte.getFactory(imgInfo));
	}

	public PngReaderByte(InputStream inputStream) {
		super(inputStream);
		setImageLineFactory(ImageLineByte.getFactory(imgInfo));
	}


}
