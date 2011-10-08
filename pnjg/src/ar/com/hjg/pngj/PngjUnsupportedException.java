package ar.com.hjg.pngj;

public class PngjUnsupportedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PngjUnsupportedException() {
		super();
	}

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
