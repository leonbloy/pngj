package ar.com.hjg.pngj;

/**
 * Exception thrown because of some valid feature of PNG standard that this
 * library does not support.
 */
public class PngjUnsupportedException extends PngjException {
	private static final long serialVersionUID = 1L;

	public PngjUnsupportedException(String message, Throwable cause) {
		super(message, cause);
	}

	public PngjUnsupportedException(String message) {
		super(message);
	}

	public PngjUnsupportedException(Throwable cause) {
		super(cause);
	}
}
