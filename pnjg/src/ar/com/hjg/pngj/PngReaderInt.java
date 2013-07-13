package ar.com.hjg.pngj;

import java.io.File;
import java.io.InputStream;

public class PngReaderInt extends PngReaderNg<ImageLine> {

	private IImageLineFactory<ImageLine> factory;

	public PngReaderInt(File file) {
		super(file);
	}

	public PngReaderInt(InputStream inputStream) {
		super(inputStream);
	}

	public PngReaderInt(String filename) {
		super(filename);
	}

	@Override
	IImageLineFactory<ImageLine> getImageLineFactory() {
		if (factory == null)
			factory = ImageLine.getFactory(imgInfo);
		return factory;
	}

}
