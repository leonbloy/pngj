package ar.com.hjg.pngj;

/**
 * Bytes consumer.
 * 
 * An object implementing can be fed with bytes.
 * 
 * It can consume in steps, so each time it's fed with n bytes it can eat
 * between 1 and n bytes.
 */
public interface IBytesConsumer {
	/**
	 * Eats some bytes, at most len (perhaps less).
	 * <p>
	 * Returns bytes actually consumed.
	 * 
	 * It returns -1 if the object didn't consume bytes because it was done or
	 * closed
	 * 
	 * It should only returns 0 if len is 0
	 */
	int consume(byte[] buf, int offset, int len);

	/** 
	 * The consumer is DONE when it does not need more bytes, 
	 * either because it ended normally, or abnormally
	 * Typically this implies it will return -1 if consume() is called afterwards,
	 * but it might happen that it will consume more (unneeded) bytes anwyway 
	 *  */
	boolean isDone();
}
