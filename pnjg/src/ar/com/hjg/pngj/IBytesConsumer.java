package ar.com.hjg.pngj;

public interface IBytesConsumer {
	/* returns bytes eaten  */
	int feed(byte[] buf,int offset,int len);
}
