package ar.com.hjg.pngj.chunks;

import ar.com.hjg.pngj.ImageInfo;

public abstract class PngChunkTextVar extends PngChunk {
	protected String key; // key/val: only for tEXt. lazy computed
	protected String val;

	protected PngChunkTextVar(String id, ImageInfo info) {
		super(id, info);
	}

	public static class PngTxtInfo {
		public String title;
		public String author;
		public String description;
		public String creation_time;// = (new Date()).toString();
		public String software;
		public String disclaimer;
		public String warning;
		public String source;
		public String comment;
		
	}

	public String getKey() {
		return key;
	}

	public String getVal() {
		return val;
	}

	public void setKeyVal(String key,String val) {
		this.key = key;
		this.val = val;
	}
	
	
}
