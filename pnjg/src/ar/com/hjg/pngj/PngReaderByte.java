package ar.com.hjg.pngj;

import java.io.File;
import java.io.InputStream;

public class PngReaderByte extends PngReaderNg<ImageLineByte> {

	private IImageLineFactory<ImageLineByte> factory;

	public PngReaderByte(File file) {
		super(file);
	}

	public PngReaderByte(InputStream inputStream) {
		super(inputStream);
	}

	public PngReaderByte(String filename) {
		super(filename);
	}

	@Override
	IImageLineFactory<ImageLineByte> getImageLineFactory() {
		if (factory == null)
			factory = ImageLineByte.getFactory(imgInfo);
		return factory;
	}

}
