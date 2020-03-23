package ar.com.hjg.pngj;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads bytes from an input stream, and feeds a IBytesConsumer.
 */
public class BufferedStreamFeeder implements Closeable {

	private InputStream stream;
	private byte[] buf;
	private int pendinglen; // bytes read+stored in buf, not yet still sent to IBytesConsumer
	private int offset;
	private boolean eof = false; // EOF on inputStream
	private boolean closeStream = true;
	private long bytesRead = 0;

	private static final int DEFAULTSIZE = 16384;

	/** By default, the stream will be closed on close() */
	public BufferedStreamFeeder(InputStream is) {
		this(is, DEFAULTSIZE);
	}

	public BufferedStreamFeeder(InputStream is, int bufsize) {
		this.stream = is;
		buf = new byte[bufsize < 1 ? DEFAULTSIZE : bufsize];
	}

	/**
	 * Returns inputstream
	 * 
	 * @return Input Stream from which bytes are read
	 */
	public InputStream getStream() {
		return stream;
	}

	/**
	 * @see BufferedStreamFeeder#feed(IBytesConsumer, int)
	 */
	public int feed(IBytesConsumer consumer) {
		return feed(consumer, Integer.MAX_VALUE);
	}

	/**
	 * Tries to feed the consumer with bytes read from the stream, at most
	 * maxbytes
	 * <p>
	 * It can return less than maxbytes (that doesn't mean that the consumer or
	 * the input stream is done)
	 * <p>
	 * Returns 0 if premature ending (no more to read, consumer not done) <br>
	 * Returns -1 if nothing fed, but consumer is done
	 */
	public int feed(IBytesConsumer consumer, int maxbytes) {
		refillBufferIfAppropiate();
		int consumed = 0;
		final int tofeed = maxbytes > 0 && maxbytes < pendinglen ? maxbytes : pendinglen;
		if (tofeed > 0) {
			consumed = consumer.consume(buf, offset, tofeed); // never returns 0
			if (consumed > 0) {
				offset += consumed;
				pendinglen -= consumed;
				assert pendinglen >= 0;
			}
		} else {
			// nothing to fed ? premature ending ?
			if (!eof)
				throw new PngjInputException("This should not happen");
			return consumer.isDone() ? -1 : 0 /* premature ending */;
		}
		if (consumed > 0) {
			return consumed;
		} else { // read bytes, but consumer refused to eat them ? (rare)
			if (!consumer.isDone())
				throw new PngjInputException("This should not happen!");
			return -1;
		}
	}

	/**
	 * Feeds as much bytes as it can to the consumer, in a loop. <br>
	 * Returns bytes actually consumed <br>
	 * This will stop when either the input stream is eof, or when the consumer
	 * refuses to eat more bytes. The caller can distinguish both cases by
	 * calling {@link #hasPendingBytes()}
	 */
	public long feedAll(IBytesConsumer consumer) {
		long n = 0;
		while (hasPendingBytes()) {
			int n1 = feed(consumer);
			if (n1 <= 0)
				break;
			n += n1;
		}
		return n;
	}

	/**
	 * Feeds exactly nbytes, retrying if necessary
	 * 
	 * @param consumer
	 *            Consumer
	 * @param nbytes
	 *            Number of bytes
	 * @return nbytes if success, 0 if premature input ending, -1 if consumer
	 *         done
	 */
	public int feedFixed(IBytesConsumer consumer, final int nbytes) {
		int remain = nbytes;
		while (remain > 0) {
			int n = feed(consumer, remain);
			if (n <= 0)
				return n;
			remain -= n;
		}
		assert remain == 0;
		return nbytes;
	}

	/**
	 * If there are not pending bytes to be consumed, tries to fill the buffer
	 * reading bytes from the stream.
	 * 
	 * If EOF is reached, sets eof=TRUE and calls close()
	 * 
	 * Find in <tt>pendinglen</tt> the amounts of bytes read.
	 * 
	 * If IOException, throws a PngjInputException
	 */
	protected void refillBufferIfAppropiate() {
		if (pendinglen > 0 || eof)
			return; // only if not pending data
		try {
			// try to read
			offset = 0;
			pendinglen = stream.read(buf);
			if (pendinglen == 0) // should never happen
				throw new PngjInputException("This should not happen: stream.read(buf) returned 0");
			else if (pendinglen < 0)
				close(); // this sets EOF and pendinglen=0
			else
				bytesRead += pendinglen;
		} catch (IOException e) {
			throw new PngjInputException(e);
		}
		// on return, either pendinglen > 0 or eof == true
	}

	/**
	 * Returuns true if we have more data to fed the consumer. This might try to
	 * grab more bytes from the stream if necessary
	 */
	public boolean hasPendingBytes() {
		refillBufferIfAppropiate();
		return pendinglen > 0;
	}

	/**
	 * @param closeStream
	 *            If true, the underlying stream will be closed on when close()
	 *            is called
	 */
	public void setCloseStream(boolean closeStream) {
		this.closeStream = closeStream;
	}

	/**
	 * Closes this object.
	 * 
	 * Sets EOF=true, and closes the stream if <tt>closeStream</tt> is true
	 * 
	 * This can be called internally, or from outside.
	 * 
	 * Idempotent, secure, never throws exception.
	 **/
	public void close() {
		eof = true;
		buf = null;
		pendinglen = 0;
		offset = 0;
		if (stream != null && closeStream) {
			try {
				stream.close();
			} catch (Exception e) {
				// PngHelperInternal.LOGGER.log(Level.WARNING, "Exception closing stream", e);
			}
		}
		stream = null;
	}

	/**
	 * Sets a new underlying inputstream. This allows to reuse this object. The
	 * old underlying is not closed and the state is not reset (you should call
	 * close() previously if you want that)
	 * 
	 * @param is
	 */
	public void setInputStream(InputStream is) { // to reuse this object
		this.stream = is;
		eof = false;
	}

	/**
	 * @return EOF on stream, or close() was called
	 */
	public boolean isEof() {
		return eof;
	}

	public long getBytesRead() {
		return bytesRead;
	}

}
