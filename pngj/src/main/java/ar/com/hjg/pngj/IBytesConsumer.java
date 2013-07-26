package ar.com.hjg.pngj;

public interface IBytesConsumer {
	/**
	 * Returns bytes consumed, -1 if we are done Should return some value
	 * between 1 and len should never return 0 (unless len=0)
	 * */
	int feed(byte[] buf, int offset, int len);
}
