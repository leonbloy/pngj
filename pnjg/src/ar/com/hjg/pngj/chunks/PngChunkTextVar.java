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
		/*
		 * public void writeChunks() { writeChunk("Title", title);
		 * writeChunk("Author", author); writeChunk("Description", description);
		 * writeChunk("Creation Time", creation_time); writeChunk("Software",
		 * software); writeChunk("Disclaimer", disclaimer); writeChunk("Software",
		 * software); writeChunk("Warning", warning); writeChunk("Source", source);
		 * writeChunk("Comment", comment); }
		 */
		/*
		 * private void writeChunk(String name, String val) { if (val == null)
		 * return; PngChunk p = PngChunk.createTextChunk(name, val, crcEngine);
		 * p.writeChunk(os); }
		 */
	}

	public String getKey() {
		return key;
	}

	public String getVal() {
		return val;
	}
}
