package ar.com.hjg.pngj;

import java.io.IOException;
import java.io.InputStream;

public class BufferedStreamFeeder {
	private InputStream is;
	private byte[] buf;
	private int pendinglen;
	private int offset;
	private boolean eof = false;
	private boolean closeOnEof = true;

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

	public int feed(IBytesConsumer c, int maxbytes) {
		if (pendinglen == 0) {
			refillBuffer();
		}
		int toread = maxbytes > 1 && maxbytes < pendinglen ? maxbytes : pendinglen;
		if (toread > 0) {
			int n = c.feed(buf, offset, toread);
			if (n > 0) {
				offset += n;
				pendinglen -= n;
			}
			return n;
		} else
			return 0;
	}

	public void feedAll(IBytesConsumer c) {
		while (hasMoreToFeed())
			feed(c);
	}

	private int refillBuffer() {
		if (pendinglen > 0 && eof)
			return 0; // only if not pending data
		try {
			// try to read
			offset = 0;
			pendinglen = is.read(buf);
			if (pendinglen < 0) {
				end();
				return -1;
			} else
				return pendinglen;
		} catch (IOException e) {
			throw new PngjInputException(e);
		}

	}

	public boolean hasMoreToFeed() {
		if (eof)
			return true;
		else
			refillBuffer();
		return pendinglen > 0;
	}

	public void setCloseOnEof(boolean closeOnEof) {
		this.closeOnEof = closeOnEof;
	}

	/** sets EOF=true, and closes the stream if closeOnEof */
	public void end() {
		try {
			eof=true;
			buf=null;
			is=null;
			if(closeOnEof)
				is.close();
		} catch (Exception e) {
		}
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
}
