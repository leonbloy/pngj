package ar.com.hjg.pngj.test;

import java.util.HashMap;

import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;

public class TestTextChunks {
	
	public static void test1() {
		HashMap<String,String> texts = new HashMap<String, String>();
		texts.put("key1","val");
		texts.put("empty1","");
		texts.put("unicode1","Hernán");
		texts.put("zero1","Hola\0chau");
		texts.put("key2","val");
		texts.put("empty2","");
		texts.put("unicode2","Hernán");
		texts.put("zero2","Hola\0chau");
		texts.put("key3","val");
		texts.put("empty3","");
		texts.put("unicode3","Hernán");
		texts.put("zero3","Hola\0chau");
		texts.put("nolatin1","Hernán\u1230");

		String suffix = "text";
		PngWriter png = TestHelper.prepareFileTmp(suffix);
			
		png.getMetadata().setText("key1", texts.get("key1"),false,false);
		png.getMetadata().setText("key2", texts.get("key2"),true,false);
		png.getMetadata().setText("key3", texts.get("key3"),true,true);
		
		png.getMetadata().setText("empty1", texts.get("empty1"),false,false);
		png.getMetadata().setText("empty2", texts.get("empty2"),true,false);
		png.getMetadata().setText("empty3", texts.get("empty3"),true,true);

		png.getMetadata().setText("unicode1", texts.get("unicode1"),false,false);
		png.getMetadata().setText("unicode2", texts.get("unicode2"),true,false);
		png.getMetadata().setText("unicode3", texts.get("unicode3"),true,true);

		png.getMetadata().setText("nolatin1", texts.get("nolatin1"),false,false);

		png.getMetadata().setText("zero1", texts.get("zero1"),false,false);
		png.getMetadata().setText("zero2", texts.get("zero2"),true,false);
		png.getMetadata().setText("zero3", texts.get("zero3"),true,true);
		
		TestHelper.endFileTmp(png);
		
		PngReader pngr = TestHelper.getFileTmp(suffix,true);
		int ok=0;
		for(PngChunk c:pngr.getChunksList().getChunks()) {
			if(! ChunkHelper.isText(c)) continue;
			ok++;
			PngChunkTextVar ct = (PngChunkTextVar)c;
			String key = ct.getKey();
			String val = ct.getVal();
			System.out.println(c.id + " chunk. Key:" + key+ " val='" + val +"'");
			if(!val.equals(texts.get(key)) ){
				System.err.println("Error: expected '" + texts.get(key) +"' got '" + val + "' key="+key + " id=" + c.id);
			}
		}
		if(ok!= texts.keySet().size())
			System.err.println("number of text chunks does not coincide");
		
		System.out.println("done");
		
	}

	public static void main(String[] args) {
		test1();
	}
}
