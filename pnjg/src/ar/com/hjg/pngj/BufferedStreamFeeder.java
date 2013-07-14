package ar.com.hjg.pngj;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads bytes from an input stream and feeds a IBytesConsumer
 */
public class BufferedStreamFeeder {
	private InputStream is;
	private byte[] buf;
	private int pendinglen; // bytes read and stored in buf that have not yet still been fed to IBytesConsumer
	private int offset;
	private boolean eof = false;
	private boolean closeOnEof = true;
	private boolean failIfNoFeed = false;

	public BufferedStreamFeeder(InputStream is) {
		this(is, 8192);
	}

	public BufferedStreamFeeder(InputStream is, int bufsize) {
		this.is = is;
		buf = new byte[bufsize];
	}

	public InputStream getIs() {
		return is;
	}

	public int feed(IBytesConsumer c) {
		return feed(c, -1);
	}

	/**
	 * Returns bytes feeded
	 * 
	 * This should return 0 only if the stream is EOF or the consumer is done
	 */
	public int feed(IBytesConsumer c, int maxbytes) {
		int n = 0;
		if (pendinglen == 0) {
			refillBuffer();
		}
		int tofeed = maxbytes > 1 && maxbytes < pendinglen ? maxbytes : pendinglen;
		if (tofeed > 0) {
			n = c.feed(buf, offset, tofeed);
			if (n > 0) {
				offset += n;
				pendinglen -= n;
			}
		}
		if (n < 1 && failIfNoFeed)
			throw new PngjInputException("failed feed bytes");
		return n;
	}

	public boolean feedFixed(IBytesConsumer c, int bytes) {
		int remain = bytes;
		while (remain > 0) {
			int n = feed(c, remain);
			if (n < 1)
				return false;
			remain -= n;
		}
		return true;
	}

	protected void refillBuffer() {
		if (pendinglen > 0 || eof)
			return; // only if not pending data
		try {
			// try to read
			offset = 0;
			pendinglen = is.read(buf);
			if (pendinglen < 0) {
				close();
				return;
			} else
				return;
		} catch (IOException e) {
			throw new PngjInputException(e);
		}
	}

	public boolean hasMoreToFeed() {
		if (eof)
			return pendinglen > 0;
		else
			refillBuffer();
		return pendinglen > 0;
	}

	public void setCloseOnEof(boolean closeOnEof) {
		this.closeOnEof = closeOnEof;
	}

	/**
	 * sets EOF=true, and closes the stream if closeOnEof Idempotent, secure,
	 * never throws exception
	 * */
	public void close() {
		eof = true;
		buf = null;
		try {
			if (is != null && closeOnEof)
				is.close();
		} catch (Exception e) {
		}
		is = null;
	}

	public void setInputStream(InputStream is) { // to reuse this object
		this.is = is;
		pendinglen = 0;
		offset = 0;
		eof = false;
	}

	public boolean isEof() {
		return eof;
	}

	public void setEof(boolean eof) {
		this.eof = eof;
	}

	public void setFailIfNoFeed(boolean failIfNoFeed) {
		this.failIfNoFeed = failIfNoFeed;
	}
}
