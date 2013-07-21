package ar.com.hjg.pngj;

import java.io.File;
import java.io.InputStream;

public class PngReaderByte extends PngReader {

	public PngReaderByte(File file) {
		super(file);
		setLineSetFactory(ImageLineSetDefault.getFactoryByte());
	}

	public PngReaderByte(InputStream inputStream) {
		super(inputStream);
		setLineSetFactory(ImageLineSetDefault.getFactoryByte());
	}

}
