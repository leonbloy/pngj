package ar.com.hjg.pngj.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class TestZlib {

	protected Random r = new Random();
	protected final int size;
	byte prev;
	int contread = 0;

	public TestZlib(int size) {
		this.size = size;
	}

	protected byte getNextByte() {
		if ((contread % 3) == 0)
			prev = (byte) r.nextInt();
		return prev;
	}

	final public double oneTry() {
		contread = 0;
		InputStream is = new InputStream() {
			@Override
			public int read() throws IOException {
				return getNextByte();
			}
		};
		OutputStreamWithCounter os = new OutputStreamWithCounter() {
			@Override
			public void write(int b) throws IOException {
				increment();
			}
		};
		int complevel = 9;
		Deflater def = new Deflater(complevel);
		DeflaterOutputStream osz = new DeflaterOutputStream(os, def);
		try {
			while (contread < size) {
				contread++;
				osz.write(is.read());
			}
			osz.flush();
			osz.close();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return ((double) (os.getCount()) / contread);

	}

	public double nTries(int n) {
		double ac = 0;
		for (int i = 0; i < n; i++)
			ac += oneTry();
		return ac / n;
	}

	private abstract class OutputStreamWithCounter extends OutputStream {
		int count = 0;

		final void increment() {
			count++;
		}

		final int getCount() {
			return count;
		}
	}

	public static void test1(String[] args) {
		TestZlib t1 = new TestZlib(1000000);
		System.out.println(t1.getClass() + " : " + t1.nTries(2));
	}

	public static void main(String[] args) throws Exception // Just for simplicity
	{
		for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
			NetworkInterface iface = ifaces.nextElement();

			System.out.println(iface.getName() + ":" + iface.isLoopback());
			for (Enumeration<InetAddress> addresses = iface.getInetAddresses(); addresses.hasMoreElements();) {
				InetAddress address = addresses.nextElement();
				System.out.println("  " + address);
			}
		}
	}
}
