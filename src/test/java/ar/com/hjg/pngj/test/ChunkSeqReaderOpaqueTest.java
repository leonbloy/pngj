package ar.com.hjg.pngj.test;

import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.samples.ChunkSeqReaderOpaque;

public class ChunkSeqReaderOpaqueTest {

	public class ChunkSeqReaderOpaque1 extends ChunkSeqReaderOpaque {

		StringBuilder sbdeb1 = new StringBuilder(); // For tests (exact comparison)
		protected long idatInflated = 0;

		public ChunkSeqReaderOpaque1() {
			super();
		}

		@Override
		protected void startNewChunk(int len, String id, long offset) {
			super.startNewChunk(len, id, offset);
			sbdeb1.append(" s" + id);
			// PngHelperInternal.debug("startNewChunk " + id);
		}

		@Override
		protected void processChunkContent(ChunkRaw chunkRaw, int offInChunk, byte[] buf, int off, int len) {
			super.processChunkContent(chunkRaw, offInChunk, buf, off, len);
			// PngHelperInternal.debug("process chunk contents " + chunkRaw.id + " " + len +
			// "/" +
			// chunkRaw.len);
			sbdeb1.append(" p" + chunkRaw.id);
		}

		@Override
		protected void postProcessChunk(ChunkReader chunkR) {
			super.postProcessChunk(chunkR);
			// PngHelperInternal.debug("post process chunk " + chunkR.getChunkRaw().id);
			sbdeb1.append(" e" + chunkR.getChunkRaw().id + "" + (chunkR.getChunkRaw().data == null ? "s" : "b"));
		}

		protected void processIdatInflatedData(byte[] inflatedRow, int offset, int len) {
			super.processIdatInflatedData(inflatedRow, offset, len);
			// PngHelperInternal.debug("process idat inflated data: " + len);
			idatInflated += len;
		}

		@Override
		protected void checkSignature(byte[] buf) {
			super.checkSignature(buf);
			// PngHelperInternal.debug("signature: ");
			sbdeb1.append("sig" + buf.length);
		}

	}

	public class ChunkSeqReaderOpaque2 extends ChunkSeqReaderOpaque1 {
		// no idat
		@Override
		protected boolean isIdatKind(String id) {
			return false;
		}

		@Override
		// skips pHYs
		protected boolean shouldSkipContent(int len, String id) {
			return super.shouldSkipContent(len, id) || id.equals("pHYs");
		}

	}

	@Test
	public void testOpaque1a() {
		testOpaqueWith(false, TestSupport.PNG_TEST_TESTG2, 7,
				"sig8 sIHDR pIHDR eIHDRs spHYs ppHYs epHYss stEXt ptEXt etEXts sIDAT eIDATs sIDAT eIDATs sIEND pIEND eIENDs");
	}

	@Test
	public void testOpaque1b() {
		testOpaqueWith(false, TestSupport.PNG_TEST_TESTG2I, 5,
				"sig8 sIHDR pIHDR eIHDRs spHYs ppHYs epHYss stEXt ptEXt etEXts sIDAT eIDATs sIEND pIEND eIENDs");
	}

	@Test
	public void testOpaque1c() {
		testOpaqueWith(false, TestSupport.PNG_TEST_IDATTRICKY, 777,
				"sig8 sIHDR pIHDR eIHDRs spHYs ppHYs epHYss sIDAT eIDATs sIDAT eIDATs sIDAT eIDATs sIDAT eIDATs sIDAT eIDATs sIDAT eIDATs stIME ptIME etIMEs siTXt piTXt eiTXts sIEND pIEND eIENDs");

	}

	@Test
	public void testOpaque1d() { // same as testOpaque1 with buffering
		testOpaqueWith(true, TestSupport.PNG_TEST_TESTG2, 1,
				"sig8 sIHDR eIHDRb spHYs epHYsb stEXt etEXtb sIDAT eIDATs sIDAT eIDATs sIEND eIENDb");
	}

	@Test
	public void testOpaque1e() { // same as testOpaque1 with buffering
		testOpaqueWith(true, TestSupport.PNG_TEST_TESTG2I, 1,
				"sig8 sIHDR eIHDRb spHYs epHYsb stEXt etEXtb sIDAT eIDATs sIEND eIENDb");
	}

	@Test
	public void testOpaque1f() { // same as testOpaque1 with buffering
		testOpaqueWith(true, TestSupport.PNG_TEST_IDATTRICKY, 7000077,
				"sig8 sIHDR eIHDRb spHYs epHYsb sIDAT eIDATs sIDAT eIDATs sIDAT eIDATs sIDAT eIDATs sIDAT eIDATs sIDAT eIDATs stIME etIMEb siTXt eiTXtb sIEND eIENDb");
	}

	@Test
	public void testOpaque2a() { // IDAT not special, pHYs skipped
		testOpaque2With(true, TestSupport.PNG_TEST_TESTG2, 3000,
				"sig8 sIHDR eIHDRb spHYs epHYss stEXt etEXtb sIDAT eIDATb sIDAT eIDATb sIEND eIENDb");
	}

	@Test
	public void testOpaque2b() { // IDAT not special, pHYs skipped
		testOpaque2With(false, TestSupport.PNG_TEST_TESTG2I, 5,
				"sig8 sIHDR pIHDR eIHDRs spHYs epHYss stEXt ptEXt etEXts sIDAT pIDAT eIDATs sIEND pIEND eIENDs");
	}

	@Test
	public void testOpaque2c() { // IDAT not special, pHYs skipped
		testOpaque2With(false, TestSupport.PNG_TEST_IDATTRICKY, 777,
				"sig8 sIHDR pIHDR eIHDRs spHYs epHYss sIDAT pIDAT eIDATs sIDAT pIDAT eIDATs sIDAT pIDAT eIDATs sIDAT pIDAT eIDATs sIDAT pIDAT eIDATs sIDAT pIDAT eIDATs stIME ptIME etIMEs siTXt piTXt eiTXts sIEND pIEND eIENDs");

	}

	private void testOpaqueWith(boolean buffer, String png, int bsize, String expected) {
		ChunkSeqReaderOpaque1 cr = new ChunkSeqReaderOpaque1();
		cr.setBlockSize(bsize);
		cr.setBufferChunks(buffer);
		cr.feedFromInputStream(TestSupport.istream(png));
		PngReader pngr = new PngReader(TestSupport.istream(png));
		pngr.readSkippingAllRows();
		long rawbytes = PngHelperInternal.getRawIdatBytes(pngr);
		TestCase.assertEquals(rawbytes, cr.idatInflated);
		TestCase.assertEquals(cr.getCurDeflatedSet().getBytesOut(), cr.idatInflated);
		TestCase.assertEquals(expected, cr.sbdeb1.toString());
	}

	private void testOpaque2With(boolean buffer, String png, int bsize, String expected) {
		ChunkSeqReaderOpaque2 cr = new ChunkSeqReaderOpaque2();
		cr.setBlockSize(bsize);
		cr.setBufferChunks(buffer);
		cr.feedFromInputStream(TestSupport.istream(png));
		TestCase.assertEquals(0, cr.idatInflated);
		TestCase.assertNull(cr.getCurDeflatedSet());
		TestCase.assertEquals(expected, cr.sbdeb1.toString());
	}

}
