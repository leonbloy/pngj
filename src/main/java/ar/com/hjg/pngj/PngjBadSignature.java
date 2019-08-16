package ar.com.hjg.pngj;

/**
 * Exception thrown by bad signature (not a PNG file)
 */
public class PngjBadSignature extends PngjInputException {
	private static final long serialVersionUID = 1L;

	public PngjBadSignature(String message, Throwable cause) {
		super(message, cause);
	}

	public PngjBadSignature(String message) {
		super(message);
	}

	public PngjBadSignature(Throwable cause) {
		super(cause);
	}
}
