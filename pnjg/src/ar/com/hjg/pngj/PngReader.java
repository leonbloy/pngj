package ar.com.hjg.pngj;

import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkLoadBehaviour;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkIHDR;
import ar.com.hjg.pngj.chunks.PngMetadata;

/**
 * Reads a PNG image, line by line
 */
public class PngReader {
	/**
	 * Basic image info - final and inmutable.
	 */
	public final ImageInfo imgInfo;

	/**
	 * not necesarily a filename, can be a description - merely informative
	 */
	protected final String filename;

	private int maxBytesChunksToLoad = 1024 * 1024; // for ancillary chunks

	private ChunkLoadBehaviour chunkLoadBehaviour = ChunkLoadBehaviour.LOAD_CHUNK_ALWAYS;

	private final InputStream is;
	private InflaterInputStream idatIstream;
	private PngIDatChunkInputStream iIdatCstream;

	/**
	 * Current chunk grounp, (0-6) already read or reading
	 * <p>
	 * see {@link ChunksList}
	 */
	protected int currentChunkGroup = -1;
	/**
	 * last read row number, starting from 0
	 */
	protected int rowNum = -1; // current row number
	private int offset = 0;
	private int bytesChunksLoaded; // bytes loaded from anciallary chunks

	private final ChunksList chunksList;
	private final PngMetadata metadata; // this a wrapper over chunks

	protected ImageLine imgLine;

	// line as bytes, counting from 1 (index 0 is reserved for filter type)
	protected byte[] rowb = null;
	protected byte[] rowbprev = null; // rowb previous
	protected byte[] rowbfilter = null; // current line 'filtered': exactly as in uncompressed stream

	private boolean shouldCloseStream = true; // true: closes stream after ending read

	/**
	 * Constructs a PngReader from an InputStream.
	 * <p>
	 * See also <code>FileHelper.createPngReader(File f)</code> if available.
	 * 
	 * Reads only the signature and first chunk (IDHR)
	 * 
	 * @param filenameOrDescription
	 *            : Optional, can be a filename or a description. Just for error/debug messages
	 * 
	 */
	public PngReader(InputStream inputStream, String filenameOrDescription) {
		this.filename = filenameOrDescription == null ? "" : filenameOrDescription;
		this.is = inputStream;
		this.chunksList = new ChunksList(null);
		this.metadata = new PngMetadata(chunksList);
		// reads header (magic bytes)
		byte[] pngid = new byte[PngHelperInternal.pngIdBytes.length];
		PngHelperInternal.readBytes(is, pngid, 0, pngid.length);
		offset += pngid.length;
		if (!Arrays.equals(pngid, PngHelperInternal.pngIdBytes))
			throw new PngjInputException("Bad PNG signature");
		// reads first chunk
		currentChunkGroup = ChunksList.CHUNK_GROUP_0_IDHR;
		int clen = PngHelperInternal.readInt4(is);
		offset += 4;
		if (clen != 13)
			throw new RuntimeException("IDHR chunk len != 13 ?? " + clen);
		byte[] chunkid = new byte[4];
		PngHelperInternal.readBytes(is, chunkid, 0, 4);
		if (!Arrays.equals(chunkid, ChunkHelper.b_IHDR))
			throw new PngjInputException("IHDR not found as first chunk??? [" + ChunkHelper.toString(chunkid) + "]");
		offset += 4;
		ChunkRaw chunk = new ChunkRaw(clen, chunkid, true);
		offset += chunk.readChunkData(is);
		PngChunkIHDR ihdr = (PngChunkIHDR) addChunkToList(chunk);
		boolean alpha = (ihdr.getColormodel() & 0x04) != 0;
		boolean palette = (ihdr.getColormodel() & 0x01) != 0;
		boolean grayscale = (ihdr.getColormodel() == 0 || ihdr.getColormodel() == 4);
		imgInfo = new ImageInfo(ihdr.getCols(), ihdr.getRows(), ihdr.getBitspc(), alpha, grayscale, palette);
		imgLine = new ImageLine(imgInfo);
		if (ihdr.getInterlaced() != 0)
			throw new PngjUnsupportedException("PNG interlaced not supported by this library");
		if (ihdr.getFilmeth() != 0 || ihdr.getCompmeth() != 0)
			throw new PngjInputException("compmethod o filtermethod unrecognized");
		if (ihdr.getColormodel() < 0 || ihdr.getColormodel() > 6 || ihdr.getColormodel() == 1
				|| ihdr.getColormodel() == 5)
			throw new PngjInputException("Invalid colormodel " + ihdr.getColormodel());
		if (ihdr.getBitspc() != 1 && ihdr.getBitspc() != 2 && ihdr.getBitspc() != 4 && ihdr.getBitspc() != 8
				&& ihdr.getBitspc() != 16)
			throw new PngjInputException("Invalid bit depth " + ihdr.getBitspc());
		// allocation: one extra byte for filter type one pixel
		rowbfilter = new byte[imgInfo.bytesPerRow + 1];
		rowb = new byte[imgInfo.bytesPerRow + 1];
		rowbprev = new byte[rowb.length];
	}

	private PngChunk addChunkToList(ChunkRaw chunk) {
		// this requires that the currentChunkGroup is ok
		PngChunk chunkType = PngChunk.factory(chunk, imgInfo);
		if (!chunkType.crit) {
			bytesChunksLoaded += chunk.len;
		}
		if (bytesChunksLoaded > maxBytesChunksToLoad) {
			logWarn("Chunk exceeded available space (" + maxBytesChunksToLoad + ") chunk: " + chunk
					+ " See PngReader.setMaxBytesChunksToLoad()\n");
		} else {
			chunksList.appendReadChunk(chunkType, currentChunkGroup);
		}
		return chunkType;
	}

	private void convertRowFromBytes(int[] buffer) {
		// http://www.libpng.org/pub/png/spec/1.2/PNG-DataRep.html
		int i, j;
		if (imgInfo.bitDepth <= 8) {
			for (i = 0, j = 1; i < imgInfo.samplesPerRowP; i++) {
				buffer[i] = (rowb[j++] & 0xFF);
			}
		} else { // 16 bitspc
			for (i = 0, j = 1; i < imgInfo.samplesPerRowP; i++) {
				buffer[i] = ((rowb[j++] & 0xFF) << 8) + (rowb[j++] & 0xFF);
			}
		}
	}

	private boolean firstChunksNotYetRead() {
		return currentChunkGroup < ChunksList.CHUNK_GROUP_1_AFTERIDHR;
	}

	/**
	 * Reads last Internally called after having read the last line. It reads extra chunks after IDAT, if present.
	 */
	private void readLastAndClose() {
		offset = (int) iIdatCstream.getOffset();
		try {
			idatIstream.close();
		} catch (Exception e) {
		}
		readLastChunks();
		if (shouldCloseStream) {
			try {
				is.close();
			} catch (Exception e) {
				throw new PngjInputException("error closing input stream!", e);
			}
		}
	}

	/**
	 * Reads (and processes) chunks after last IDAT.
	 **/
	private void readLastChunks() {
		// PngHelper.logdebug("idat ended? " + iIdatCstream.isEnded());
		currentChunkGroup = ChunksList.CHUNK_GROUP_5_AFTERIDAT;
		if (!iIdatCstream.isEnded())
			iIdatCstream.forceChunkEnd();
		int clen = iIdatCstream.getLenLastChunk();
		byte[] chunkid = iIdatCstream.getIdLastChunk();
		boolean endfound = false;
		boolean first = true;
		boolean ignore = false;
		while (!endfound) {
			ignore = false;
			if (!first) {
				clen = PngHelperInternal.readInt4(is);
				offset += 4;
				if (clen < 0)
					throw new PngjInputException("bad len " + clen);
				PngHelperInternal.readBytes(is, chunkid, 0, 4);
				offset += 4;
			}
			first = false;
			if (Arrays.equals(chunkid, ChunkHelper.b_IDAT)) {
				// PngHelper.logdebug("extra IDAT chunk len - ignoring : ");
				ignore = true;
			} else if (Arrays.equals(chunkid, ChunkHelper.b_IEND)) {
				currentChunkGroup = ChunksList.CHUNK_GROUP_6_END;
				endfound = true;
			}
			ChunkRaw chunk = new ChunkRaw(clen, chunkid, true);
			String chunkids = ChunkHelper.toString(chunkid);
			boolean loadchunk = ChunkHelper.shouldLoad(chunkids, chunkLoadBehaviour);
			offset += chunk.readChunkData(is);
			if (loadchunk && !ignore) {
				addChunkToList(chunk);
			}
		}
		if (!endfound)
			throw new PngjInputException("end chunk not found - offset=" + offset);
		// PngHelper.logdebug("end chunk found ok offset=" + offset);
	}

	private void unfilterRow() {
		int ftn = rowbfilter[0];
		FilterType ft = FilterType.getByVal(ftn);
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

	private void unfilterRowAverage() {
		int i, j, x;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imgInfo.bytesPerRow; i++, j++) {
			x = j > 0 ? (rowb[j] & 0xff) : 0;
			rowb[i] = (byte) (rowbfilter[i] + (x + (rowbprev[i] & 0xFF)) / 2);
		}
	}

	private void unfilterRowNone() {
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			rowb[i] = (byte) (rowbfilter[i]);
		}
	}

	private void unfilterRowPaeth() {
		int i, j, x, y;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imgInfo.bytesPerRow; i++, j++) {
			x = j > 0 ? (rowb[j] & 0xFF) : 0;
			y = j > 0 ? (rowbprev[j] & 0xFF) : 0;
			rowb[i] = (byte) (rowbfilter[i] + PngHelperInternal.filterPaethPredictor(x, rowbprev[i] & 0xFF, y));
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
			rowb[i] = (byte) (rowbfilter[i] + rowbprev[i]);
		}
	}

	/**
	 * Reads chunks before first IDAT. Normally this is called automatically
	 * <p>
	 * Position before: after IDHR (crc included) Position after: just after the first IDAT chunk id
	 * <P>
	 * This can be called several times (tentatively), it does nothing if already run
	 * <p>
	 * (Note: when should this be called? in the constructor? hardly, because we loose the opportunity to call
	 * setChunkLoadBehaviour() and perhaps other settings before reading the first row? but sometimes we want to access
	 * some metadata (plte, phys) before. Because of this, this method can be called explicitly but is also called
	 * implicititly in some methods (getMetatada(), getChunksList())
	 */
	void readFirstChunks() {
		if (!firstChunksNotYetRead())
			return;
		int clen = 0;
		boolean found = false;
		byte[] chunkid = new byte[4]; // it's important to reallocate in each iteration
		currentChunkGroup = ChunksList.CHUNK_GROUP_1_AFTERIDHR;
		while (!found) {
			clen = PngHelperInternal.readInt4(is);
			offset += 4;
			if (clen < 0)
				break;
			PngHelperInternal.readBytes(is, chunkid, 0, 4);
			offset += 4;
			if (Arrays.equals(chunkid, ChunkHelper.b_IDAT)) {
				found = true;
				currentChunkGroup = ChunksList.CHUNK_GROUP_4_IDAT;
				// add dummy idat chunk to list
				ChunkRaw chunk = new ChunkRaw(0, chunkid, false);
				addChunkToList(chunk);
				break;
			} else if (Arrays.equals(chunkid, ChunkHelper.b_IEND)) {
				throw new PngjInputException("END chunk found before image data (IDAT) at offset=" + offset);
			}
			ChunkRaw chunk = new ChunkRaw(clen, chunkid, true);
			String chunkids = ChunkHelper.toString(chunkid);
			boolean loadchunk = ChunkHelper.shouldLoad(chunkids, chunkLoadBehaviour);
			offset += chunk.readChunkData(is);
			if (chunkids.equals(ChunkHelper.PLTE))
				currentChunkGroup = ChunksList.CHUNK_GROUP_2_PLTE;
			if (loadchunk)
				addChunkToList(chunk);
			if (chunkids.equals(ChunkHelper.PLTE))
				currentChunkGroup = ChunksList.CHUNK_GROUP_3_AFTERPLTE;
		}
		int idatLen = found ? clen : -1;
		if (idatLen < 0)
			throw new PngjInputException("first idat chunk not found!");
		iIdatCstream = new PngIDatChunkInputStream(is, idatLen, offset);
		idatIstream = new InflaterInputStream(iIdatCstream);
	}

	/**
	 * Logs/prints a warning.
	 * <p>
	 * The default behaviour is print to stderr, but it can be overriden.
	 * <p>
	 * This happens rarely - most errors are fatal.
	 */
	protected void logWarn(String warn) {
		System.err.println(warn);
	}

	/**
	 * Dummy method
	 * <p>
	 * Since version 0.88 (Apr 2012) the ending chunks are read automatically, internally, after reading the last row.
	 * This does nothing now, just kept for backward compatibily
	 */
	public void end() {
	}

	public ChunkLoadBehaviour getChunkLoadBehaviour() {
		return chunkLoadBehaviour;
	}

	/**
	 * All loaded chunks.
	 * <p>
	 * Critical chunks are included, except that all IDAT chunks appearance are replaced by a single dummy-marker IDAT
	 * chunk. These might be copied to the PngWriter
	 */
	public ChunksList getChunksList() {
		if (firstChunksNotYetRead())
			readFirstChunks();
		return chunksList;
	}

	public int getCurrentChunkGroup() {
		return currentChunkGroup;
	}

	/**
	 * High level wrapper over chunksList
	 */
	public PngMetadata getMetadata() {
		if (firstChunksNotYetRead())
			readFirstChunks();
		return metadata;
	}

	/**
	 * Calls <code>readRow(int[] buffer, int nrow)</code> using internal ImageLine as buffer. This doesn't allocate or
	 * copy anything.
	 * 
	 * @return The ImageLine that also is available inside this object.
	 */
	public ImageLine readRow(int nrow) {
		readRow(imgLine.scanline, nrow);
		imgLine.filterUsed = FilterType.getByVal(rowbfilter[0]);
		imgLine.setRown(nrow);
		return imgLine;
	}

	/**
	 * Reads a line and returns it as a int[] array.
	 * <p>
	 * You can pass (optionally) a prealocatted buffer.
	 * 
	 * @param buffer
	 *            Prealocated buffer, or null.
	 * @param nrow
	 *            Row number (0 is top). This is mostly for checking, because this library reads rows in sequence.
	 * 
	 * @return The scanline in the same passwd buffer if it was allocated, a newly allocated one otherwise
	 */
	public int[] readRow(int[] buffer, final int nrow) {
		if (nrow < 0 || nrow >= imgInfo.rows)
			throw new PngjInputException("invalid line");
		if (nrow != rowNum + 1)
			throw new PngjInputException("invalid line (expected: " + (rowNum + 1));
		if (nrow == 0 && firstChunksNotYetRead())
			readFirstChunks();
		rowNum++;
		if (buffer == null || buffer.length < imgInfo.samplesPerRowP)
			buffer = new int[imgInfo.samplesPerRowP];
		// swap
		byte[] tmp = rowb;
		rowb = rowbprev;
		rowbprev = tmp;
		// loads in rowbfilter "raw" bytes, with filter
		PngHelperInternal.readBytes(idatIstream, rowbfilter, 0, rowbfilter.length);
		rowb[0] = 0;
		unfilterRow();
		rowb[0] = rowbfilter[0];
		convertRowFromBytes(buffer);
		// new: if last row, automatically call end()
		if (nrow == imgInfo.rows - 1)
			readLastAndClose();
		return buffer;
	}

	public void setChunkLoadBehaviour(ChunkLoadBehaviour chunkLoadBehaviour) {
		this.chunkLoadBehaviour = chunkLoadBehaviour;
	}

	/**
	 * Total maximum bytes to load from ancillary ckunks (default: 1Mb)
	 * <p>
	 * If exceeded, chunks will be ignored
	 */
	public void setMaxBytesChunksToLoad(int maxBytesChunksToLoad) {
		this.maxBytesChunksToLoad = maxBytesChunksToLoad;
	}

	/**
	 * if true, input stream will be closed after ending read 
	 * <p>
	 * default=true
	 */
	public void setShouldCloseStream(boolean shouldCloseStream) {
		this.shouldCloseStream = shouldCloseStream;
	}

	/**
	 * Basic info, for debugging.
	 */
	public String toString() { // basic info
		return "filename=" + filename + " " + imgInfo.toString();
	}

}
