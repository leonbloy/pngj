package ar.com.hjg.pngj;

public class PngjOutputException extends PngjException {
	private static final long serialVersionUID = 1L;

	public PngjOutputException(String message, Throwable cause) {
		super(message, cause);
	}

	public PngjOutputException(String message) {
		super(message);
	}

	public PngjOutputException(Throwable cause) {
		super(cause);
	}
}
