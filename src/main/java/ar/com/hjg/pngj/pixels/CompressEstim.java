package ar.com.hjg.pngj.pixels;

import java.util.Random;
import java.util.zip.Deflater;

public class CompressEstim  {


	enum CompressEstimMethod {
		DEFLATER, LZ4DUMMY
	};

	private final CompressEstimMethod method;
	/**
	 * we refuse to do estimations for very small lengths, we return 1.0 in those cases
	 */
	public final static int MINLEN = 32;
	private long statmsecs = 1;
	private long statbytesin = 1;
	private long statbytesout = 1;
	private byte[]  buffer;

	public CompressEstim(CompressEstimMethod method) {
		this.method = method;
	}

	public double estim(byte[] buf) {
		return estim(buf, 0, buf.length);
	}

	/**
	 * Returns an estimation of compression ratio (out/in).
	 * 
	 * This is one-shot (not dependent on previous history). For small values (less than MINLEN) returns 1.0
	 * For large values (more than 64K) it might consider only the first 64K
	 * 
	 * @param buf
	 * @param offset
	 * @param len
	 * @return
	 */
	public double estim(byte[] buf, int offset, int len) {
		double r = 1.0;
		long t0 = System.currentTimeMillis();
		if (len >= MINLEN) {
			if (method == CompressEstimMethod.LZ4DUMMY) {
				r = estimWithLz4(buf, offset, len);
			} else if (method == CompressEstimMethod.DEFLATER) {
				r = estimWithDeflate(buf, offset, len);
			} else {
				throw new RuntimeException("??");
			}
		}
		t0 = System.currentTimeMillis() - t0;
		statbytesin += len;
		statbytesout += (long) (r * len);
		statmsecs += t0;
		return r;
	}

	private double estimWithLz4(byte[] buf, int offset, int len) {
		DeflaterEstimatorLz4 dc = new DeflaterEstimatorLz4();
		return dc.compressEstim(buf, offset, len);
	}

	private double estimWithDeflate(byte[] buf, int offset, int len) {
		Deflater def = new Deflater(); // could be a field?little improvement
		def.setInput(buf, offset, len);
		def.finish();
		int maxsize = len * 2; // !!
		if (buffer == null || buffer.length < maxsize)
			buffer = new byte[maxsize];
		int compressed = def.deflate(buffer);
		def.end();
		return compressed / (double) len;
	}

	public void close() {
		
	}
	
	public String toStringStats() {
		return String.format("Method=%s bytesin(kb)=%.1f r=%.3f MB/sec=%.1f", method.toString(), (statbytesin / 1024.0),
				globalCompression(), getSpeed());
	}

	/** returns global compression ratio for all history - you'll rarely use this */
	public double globalCompression() { // all history!
		return (statbytesout / (double) statbytesin);
	}

	/** returns speed estimator (in MB/sec) for all history - only for profile */
	public double getSpeed() { // MB/sec in
		return statbytesin / (statmsecs * 1024.0);
	}

	private static byte[] getSample1(int d) {
		int len = 25000;
		Random r = new Random();
		byte[] b = new byte[len];
		for (int i = 1; i < len; i++)
			b[i] = r.nextBoolean() ? b[i - 1] : (byte) (b[i - 1] + r.nextInt(d * 2 + 1) / 2);
		return b;
	}

	public static void main(String[] args) {
		CompressEstim estdef = new CompressEstim(CompressEstimMethod.DEFLATER);
		CompressEstim estlz4 = new CompressEstim(CompressEstimMethod.LZ4DUMMY);
		for (int d = 0; d < 20; d++) {
			byte[] b = getSample1(d);
			double edef = 0;
			double elz42 = 0;
			int times = 100;
			for (int t = 0; t < times; t++) {
				b[0]=(byte) t;
				elz42 = estlz4.estim(b);
				edef = estdef.estim(b);
			}
			System.out.printf("%.4f %.4f\n", edef, elz42);
		}
		System.out.println(estdef.toStringStats());
		System.out.println(estlz4.toStringStats());

		System.out.println(estlz4.getSpeed() / estdef.getSpeed());
	}

}
