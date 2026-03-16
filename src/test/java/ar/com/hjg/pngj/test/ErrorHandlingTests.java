package ar.com.hjg.pngj.test;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import ar.com.hjg.pngj.ChunkSeqReaderPng;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngjException;

public class ErrorHandlingTests {
	@Test
	public void testReadIncomplete() throws PngjException {
		InputStream is = TestSupport.istream("test/bad_truncated.png");
		PngReader r = new PngReader(is);
		//r.readRows();
		//r.end();
		//Assertions.assertFalse(r.getChunkseq().isDone());
	}
}
