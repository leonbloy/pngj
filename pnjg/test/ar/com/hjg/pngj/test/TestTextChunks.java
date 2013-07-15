package ar.com.hjg.pngj.test;

import java.io.File;
import java.util.HashMap;

import junit.framework.TestCase;

import org.junit.Test;

import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;

public class TestTextChunks {

	@Test
	public void testTestChunks() {
		HashMap<String, String> texts = new HashMap<String, String>();
		texts.put("key1", "val");
		texts.put("empty1", "");
		texts.put("unicode1", "Hernán");
		texts.put("zero1", "Hola\0chau");
		texts.put("key2", "val");
		texts.put("empty2", "");
		texts.put("unicode2", "Hernán");
		texts.put("zero2", "Hello\0bye");
		texts.put("key3", "val");
		texts.put("empty3", "");
		texts.put("unicode3", "Hernán");
		texts.put("zero3", "Hello\0bye");
		texts.put("nolatin1", "Hernán\u1230");

		File file1 = TestSupport.getTmpFile("testtext");
		PngWriter png = TestSupport.prepareFileTmp(file1);

		png.getMetadata().setText("key1", texts.get("key1"), false, false);
		png.getMetadata().setText("key2", texts.get("key2"), true, false);
		png.getMetadata().setText("key3", texts.get("key3"), true, true);

		png.getMetadata().setText("empty1", texts.get("empty1"), false, false);
		png.getMetadata().setText("empty2", texts.get("empty2"), true, false);
		png.getMetadata().setText("empty3", texts.get("empty3"), true, true);

		png.getMetadata().setText("unicode1", texts.get("unicode1"), false, false);
		png.getMetadata().setText("unicode2", texts.get("unicode2"), true, false);
		png.getMetadata().setText("unicode3", texts.get("unicode3"), true, true);

		png.getMetadata().setText("nolatin1", texts.get("nolatin1"), false, false);

		png.getMetadata().setText("zero1", texts.get("zero1"), false, false);
		png.getMetadata().setText("zero2", texts.get("zero2"), true, false);
		png.getMetadata().setText("zero3", texts.get("zero3"), true, true);

		TestSupport.endFileTmp(png);

		PngReader pngr = new PngReader(file1);
		pngr.readSkippingAllRows();
		int ok = 0;
		for (PngChunk c : pngr.getChunksList().getChunks()) {
			if (!ChunkHelper.isText(c))
				continue;
			ok++;
			PngChunkTextVar ct = (PngChunkTextVar) c;
			String key = ct.getKey();
			String val = ct.getVal();
			//System.out.println(c.id + " chunk. Key:" + key + " val='" + val + "'");
			TestCase.assertEquals(texts.get(key), val);
		}
		TestCase.assertEquals("number of text chunks does not coincide",texts.keySet().size(), ok);

	}

}
