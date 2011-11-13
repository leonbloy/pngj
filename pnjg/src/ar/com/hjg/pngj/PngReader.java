package ar.com.hjg.pngj;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.InflaterInputStream;

import ar.com.hjg.pngj.PngIDatChunkInputStream.IdatChunkInfo;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkList;
import ar.com.hjg.pngj.chunks.ChunkLoadBehaviour;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkIHDR;

/**
 * Reads a PNG image, line by line
 */
public class PngReader {
	/**
	 * Basic image info - final and inmutable.
	 */
	public final ImageInfo imgInfo;
	protected final String filename; // optional
	private final InputStream is;
	private final InflaterInputStream idatIstream;
	private final PngIDatChunkInputStream iIdatCstream;
	private int offset = 0;
	// chunks: add exclusively with addChunkToList()
	private static int MAX_BYTES_CHUNKS_TO_LOAD = 640000;
	private int bytesChunksLoaded;
	private ChunkLoadBehaviour chunkLoadBehaviour = ChunkLoadBehaviour.LOAD_CHUNK_ALWAYS;
	// private final int valsPerRow; // samples per row= cols x channels
	protected int rowNum = -1; // current row number
	protected ImageLine imgLine;
	// line as bytes, counting from 1 (index 0 is reserved for filter type)
	protected byte[] rowb = null; 
	protected byte[] rowbprev = null; // rowb previous
	protected byte[] rowbfilter = null; // current line 'filtered': exactly as in uncompressed stream
	/** 
	 * All chunks loaded.
	 * <p>
	 * Criticals are included, except that all IDAT chunks appearance are replaced
	 * by a single dummy-marker IDAT chunk.
	 * <p>
	 * These might be copied to the PngWriter
	 */
	public ChunkList chunks = new ChunkList();
	// FoundChunkInfo/foundChunksInfo : all chunks signatures - merely informative
	protected List<FoundChunkInfo> foundChunksInfo = new ArrayList<FoundChunkInfo>();

	private static class FoundChunkInfo {
		public final String id;
		public final int len;
		public final int offset;
		public final boolean loaded;

		private FoundChunkInfo(String id, int len, int offset, boolean loaded) {
			this.id = id;
			this.len = len;
			this.offset = offset;
			this.loaded = loaded;
		}

		public String toString() {
			return "chunk " + id + " len=" + len + " offset=" + offset
					+ (this.loaded ? " " : " X ");
		}
	}

	/**
	 * Constructs a PngReader from an InputStream.
	 * <p>
	 * It loads the header and first chunks, pausing at the
	 * beginning of the image data (first IDAT chunk).
	 * <p>
	 * See also <code>FileHelper.createPngReader(File f)</code> if available.
	 *     
	 * @param filenameOrDescription : 
	 *    Optional, can be a description. Just for error/debug messages
	 *    
	 */
	public PngReader(InputStream inputStream, String filenameOrDescription) {
		this.filename = filenameOrDescription == null ? "" : filenameOrDescription;
		this.is = inputStream;
		// reads header (magic bytes)
		byte[] pngid = new byte[PngHelper.pngIdBytes.length];
		PngHelper.readBytes(is, pngid, 0, pngid.length);
		offset += pngid.length;
		if (!Arrays.equals(pngid, PngHelper.pngIdBytes))
			throw new PngjInputException("Bad PNG signature");
		// reads first chunks
		int clen = PngHelper.readInt4(is);
		offset += 4;
		if (clen != 13)
			throw new RuntimeException("IDHR chunk len != 13 ?? " + clen);
		byte[] chunkid = new byte[4];
		PngHelper.readBytes(is, chunkid, 0, 4);
		if (!Arrays.equals(chunkid, ChunkHelper.IHDR))
			throw new PngjInputException("IHDR not found as first chunk??? ["
					+ ChunkHelper.toString(chunkid) + "]");
		offset += 4;
		ChunkRaw chunk = new ChunkRaw(clen, chunkid, true);
		String chunkids = ChunkHelper.toString(chunkid);
		foundChunksInfo.add(new FoundChunkInfo(chunkids, clen, offset - 8, true));
		offset += chunk.readChunkData(is);
		PngChunkIHDR ihdr = (PngChunkIHDR) addChunkToList(chunk);
		if (ihdr.interlaced != 0)
			throw new PngjUnsupportedException("PNG interlaced not supported by this library");
		if (ihdr.filmeth != 0 || ihdr.compmeth != 0)
			throw new PngjInputException("compmethod o filtermethod unrecognized");
		boolean alpha = (ihdr.colormodel & 0x04) != 0;
		boolean palette = (ihdr.colormodel & 0x01) != 0;
		boolean grayscale = (ihdr.colormodel == 0 || ihdr.colormodel == 4);
		if (ihdr.colormodel < 0 || ihdr.colormodel > 6 || ihdr.colormodel == 1
				|| ihdr.colormodel == 5)
			throw new PngjInputException("Invalid colormodel " + ihdr.colormodel);
		if (ihdr.bitspc != 1 && ihdr.bitspc != 2 && ihdr.bitspc != 4 && ihdr.bitspc != 8
				&& ihdr.bitspc != 16)
			throw new PngjInputException("Invalid bit depth " + ihdr.bitspc);
		imgInfo = new ImageInfo(ihdr.cols, ihdr.rows, ihdr.bitspc, alpha, grayscale, palette);
		imgLine = new ImageLine(imgInfo);
		// allocation: one extra byte for filter type one pixel
		rowbfilter = new byte[imgInfo.bytesPerRow + 1];
		rowb = new byte[imgInfo.bytesPerRow + 1];
		rowbprev = new byte[rowb.length];
		int idatLen = readFirstChunks();
		if (idatLen < 0)
			throw new PngjInputException("first idat chunk not found!");
		iIdatCstream = new PngIDatChunkInputStream(is, idatLen, offset);
		idatIstream = new InflaterInputStream(iIdatCstream);
	}

	private PngChunk addChunkToList(ChunkRaw chunk) {
		PngChunk chunkType = PngChunk.factory(chunk, imgInfo);
		if (!chunkType.crit) {
			bytesChunksLoaded += chunk.len;
		}
		if (bytesChunksLoaded > MAX_BYTES_CHUNKS_TO_LOAD) {
			throw new PngjInputException("Chunk exceeded available space ("
					+ MAX_BYTES_CHUNKS_TO_LOAD + ") chunk: " + chunk
					+ " See PngReader.MAX_BYTES_CHUNKS_TO_LOAD\n");
		}
		chunks.appendChunk(chunkType);
		return chunkType;
	}

	/**
	 * Reads chunks before first IDAT. Position before: after IDHR (crc included)
	 * Position after: just after the first IDAT chunk id Returns length of first
	 * IDAT chunk , -1 if not found
	 **/
	private int readFirstChunks() {
		int clen = 0;
		boolean found = false;
		byte[] chunkid = new byte[4]; // it's important to reallocate in each
																	// iteration
		while (!found) {
			clen = PngHelper.readInt4(is);
			offset += 4;
			if (clen < 0)
				break;
			PngHelper.readBytes(is, chunkid, 0, 4);
			offset += 4;
			if (Arrays.equals(chunkid, ChunkHelper.IDAT)) {
				found = true;
				// add dummy idat chunk to list
				ChunkRaw chunk = new ChunkRaw(0, chunkid, false);
				addChunkToList(chunk);
				break;
			} else if (Arrays.equals(chunkid, ChunkHelper.IEND)) {
				throw new PngjInputException(
						"END chunk found before image data (IDAT) at offset=" + offset);
			}
			ChunkRaw chunk = new ChunkRaw(clen, chunkid, true);
			String chunkids = ChunkHelper.toString(chunkid);
			boolean loadchunk = ChunkHelper.shouldLoad(chunkids, chunkLoadBehaviour);
			foundChunksInfo.add(new FoundChunkInfo(chunkids, clen, offset - 8, loadchunk));
			offset += chunk.readChunkData(is);
			if (loadchunk)
				addChunkToList(chunk);
		}
		return found ? clen : -1;
	}

	/**
	 * Reads (and processes ... up to a point) chunks after last IDAT.
	 **/
	private void readLastChunks() {
		// PngHelper.logdebug("idat ended? " + iIdatCstream.isEnded());
		if (!iIdatCstream.isEnded())
			iIdatCstream.forceChunkEnd();
		// add chunks to list (just informational)
		for (IdatChunkInfo idat : iIdatCstream.foundChunksInfo)
			foundChunksInfo.add(new FoundChunkInfo(ChunkHelper.IDAT_TEXT, idat.len,
					idat.offset, true));
		int clen = iIdatCstream.getLenLastChunk();
		byte[] chunkid = iIdatCstream.getIdLastChunk();
		boolean endfound = false;
		boolean first = true;
		boolean ignore = false;
		while (!endfound) {
			ignore = false;
			if (!first) {
				clen = PngHelper.readInt4(is);
				offset += 4;
				if (clen < 0)
					throw new PngjInputException("bad len " + clen);
				PngHelper.readBytes(is, chunkid, 0, 4);
				offset += 4;
			}
			first = false;
			if (Arrays.equals(chunkid, ChunkHelper.IDAT)) {
				// PngHelper.logdebug("extra IDAT chunk len - ignoring : ");
				ignore = true;
			} else if (Arrays.equals(chunkid, ChunkHelper.IEND)) {
				endfound = true;
			}
			ChunkRaw chunk = new ChunkRaw(clen, chunkid, true);
			String chunkids = ChunkHelper.toString(chunkid);
			boolean loadchunk = ChunkHelper.shouldLoad(chunkids, chunkLoadBehaviour);
			foundChunksInfo.add(new FoundChunkInfo(chunkids, clen, offset - 8, loadchunk));
			offset += chunk.readChunkData(is);
			if (loadchunk && !ignore) {
				addChunkToList(chunk);
			}
		}
		if (!endfound)
			throw new PngjInputException("end chunk not found - offset=" + offset);
		// PngHelper.logdebug("end chunk found ok offset=" + offset);
	}

	/**
	 * Calls <code>readRow(int[] buffer, int nrow)</code>
	 * using internal ImageLine as buffer. This doesn't allocate
	 * or copy anything.
	 * 
	 * @return The ImageLine that also is available inside this object.
	 */
	public ImageLine readRow(int nrow) {
		readRow(imgLine.scanline, nrow);
		imgLine.filterUsed = PngFilterType.getByVal(rowbfilter[0]);
		imgLine.incRown();
		return imgLine;
	}

	/**
	 * Reads a line and returns it as a int[] array.
	 * 
	 * You can pass (optionally) a prealocatted buffer.
	 * 
	 * @param buffer Prealocated buffer, or null.
	 * @param nrow Row number (0 is top). This is mostly for checking, because
	 *  this library reads rows in sequence.
	 *  
	 * @return The scanline in the same passwd buffer if it was allocated, a newly allocated one
	 *         otherwise
	 */
	public int[] readRow(int[] buffer, int nrow) {
		if (nrow < 0 || nrow >= imgInfo.rows)
			throw new PngjInputException("invalid line");
		if (nrow != rowNum + 1)
			throw new PngjInputException("invalid line (expected: " + (rowNum + 1));
		rowNum++;
		if (buffer == null || buffer.length<imgInfo.samplesPerRowP)
			buffer = new int[imgInfo.samplesPerRowP];
		// swap
		byte[] tmp = rowb;
		rowb = rowbprev;
		rowbprev = tmp;
		// loads in rowbfilter "raw" bytes, with filter
		PngHelper.readBytes(idatIstream, rowbfilter, 0, rowbfilter.length);
		rowb[0] = 0;
		unfilterRow();
		rowb[0] = rowbfilter[0];
		convertRowFromBytes(buffer);
		return buffer;
	}

	private void convertRowFromBytes(int[] buffer) {
		// http://www.libpng.org/pub/png/spec/1.2/PNG-DataRep.html
		int i, j;
		if (imgInfo.bitDepth <= 8) {
			for (i = 0, j = 1; i < imgInfo.samplesPerRowP; i++) {
				buffer[i] = (rowb[j++]&0xFF);
			}
		} else { // 16 bitspc
			for (i = 0, j = 1; i < imgInfo.samplesPerRowP; i++) {
				buffer[i] = ((rowb[j++]&0xFF) << 8) + (rowb[j++]&0xFF);
			}
		}
	}

	private void unfilterRow() {
		int ftn = rowbfilter[0];
		PngFilterType ft = PngFilterType.getByVal(ftn);
		if (ft == null)
			throw new PngjInputException("Filter type " + ftn + " invalid");
		switch (ft) {
		case FILTER_NONE:
			unfilterRowNone();
			break;
		case FILTER_SUB:
			unfilterRowSub();
			break;
		case FILTER_UP:
			unfilterRowUp();
			break;
		case FILTER_AVERAGE:
			unfilterRowAverage();
			break;
		case FILTER_PAETH:
			unfilterRowPaeth();
			break;
		default:
			throw new PngjInputException("Filter type " + ftn + " not implemented");
		}
	}

	private void unfilterRowNone() {
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			rowb[i] = (byte) (rowbfilter[i] );
		}
	}

	private void unfilterRowSub() {
		int i, j;
		for (i = 1; i <= imgInfo.bytesPixel; i++) {
			rowb[i] = (byte) (rowbfilter[i]);
		}
		for (j = 1, i = imgInfo.bytesPixel + 1; i <= imgInfo.bytesPerRow; i++, j++) {
			rowb[i] = (byte) (rowbfilter[i] + rowb[j]);
		}
	}

	private void unfilterRowUp() {
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			rowb[i] = (byte) (rowbfilter[i] + rowbprev[i]) ;
		}
	}

	private void unfilterRowAverage() {
		int i, j, x;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imgInfo.bytesPerRow; i++, j++) {
			x = j > 0 ? (rowb[j]&0xff) : 0;
			rowb[i] = (byte) (rowbfilter[i]  + (x + (rowbprev[i]&0xFF)) / 2);
		}
	}

	private void unfilterRowPaeth() {
		int i, j, x, y;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imgInfo.bytesPerRow; i++, j++) {
			x = j > 0 ? (rowb[j]&0xFF) : 0;
			y = j > 0 ? (rowbprev[j]&0xFF) : 0;
			rowb[i] = (byte) (rowbfilter[i] + PngFilterType.filterPaethPredictor(x,	rowbprev[i]&0xFF, y));
		}
	}

	/**
	 * This should be called after having read the last line. It reads extra
	 * chunks after IDAT, if present.
	 */
	public void end() {
		offset = (int) iIdatCstream.getOffset();
		try {
			idatIstream.close();
		} catch (Exception e) {
		}
		readLastChunks();
		try {
			is.close();
		} catch (Exception e) {
			throw new PngjInputException("error closing input stream!", e);
		}
	}

	public String toString() { // basic info
		return "filename=" + filename + " " + imgInfo.toString();
	}

	/** negative if not set */
	public double getDpi() { 
		return chunks.getPHYSdpi();
	}

	
	/** Prints chunks list to sdtio, for debugging. */
	public void printFoundChunks() { 
		for (FoundChunkInfo c : foundChunksInfo) {
			System.out.println(c);
		}
	}

	
}
