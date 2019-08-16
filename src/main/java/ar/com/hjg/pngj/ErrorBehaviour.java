package ar.com.hjg.pngj;

public enum ErrorBehaviour {
	STRICT(0), // default mode: any error aborts reading with exception
	LENIENT1_CRC(1), // CRC errors only trigger warning (or nothing if not checking)
	LENIENT2_ANCILLARY(3), // also: content errors in ancillary chunks are ignored
	SUPER_LENIENT(5); // we try hard to read, even garbage, without throwing exceptions
	final int c;

	private ErrorBehaviour(int c) {
		this.c = c;
	}

}