package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.PngjException;

public class PngBadCharsetException extends PngjException {

	public PngBadCharsetException(String message, Throwable cause) {
		super(message, cause);
	}

	public PngBadCharsetException(String message) {
		super(message);
	}

	public PngBadCharsetException(Throwable cause) {
		super(cause);
	}

}
