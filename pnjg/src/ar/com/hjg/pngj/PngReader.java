package ar.com.hjg.pngj;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.InflaterInputStream;

import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkLoadBehaviour;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkIDAT;
import ar.com.hjg.pngj.chunks.PngChunkIHDR;
import ar.com.hjg.pngj.chunks.PngChunkSkipped;
import ar.com.hjg.pngj.chunks.PngMetadata;

/**
 * Reads a PNG image, line by line.
 * <p>
 * The reading sequence is as follows: <br>
 * 1. At construction time, the header and IHDR chunk are read (basic image info) <br>
 * 2. Optional: If you call getMetadata() or getChunksLisk() before start reading the rows, the chunks before IDAT are automatically loaded <br>
 * 3. The rows are read in strict sequence, from 0 to nrows-1 (you can skip rows by calling getRow() )<br>
 * 4. Reading of the last row triggers the loading of trailing chunks, and ends the reader.<br>
 * 5. end() forcibly finishes/aborts the reading and closes the stream  
 * 
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

	private ChunkLoadBehaviour chunkLoadBehaviour = ChunkLoadBehaviour.LOAD_CHUNK_ALWAYS; // see setter/getter

	private boolean shouldCloseStream = true; // true: closes stream after ending - see setter/getter

	// some performance/defensive limits
	private long maxTotalBytesRead = 200 * 1024 * 1024; // 200MB
	private int maxBytesMetadata = 5 * 1024 * 1024; // for ancillary chunks - see setter/getter
	private int skipChunkMaxSize = 2 * 1024 * 1024; // chunks exceeding this size will be skipped (nor even CRC checked)
	private String[] skipChunkIds = { "fdAT" }; // chunks with these ids will be skipped (nor even CRC checked)
	private HashSet<String> skipChunkIdsSet; // lazily created from skipChunksById

	protected final PngMetadata metadata; // this a wrapper over chunks
	protected final ChunksList chunksList;

	protected ImageLine imgLine;

	// line as bytes, counting from 1 (index 0 is reserved for filter type)
	protected byte[] rowb = null;
	protected byte[] rowbprev = null; // rowb previous
	protected byte[] rowbfilter = null; // current line 'filtered': exactly as in uncompressed stream

	/**
	 * Current chunk group, (0-6) already read or reading
	 * <p>
	 * see {@link ChunksList}
	 */
	protected int currentChunkGroup = -1;

	protected int rowNum = -1; // last read row number, starting from 0
	private long offset = 0; // offset in InputStream = bytes read
	private int bytesChunksLoaded; // bytes loaded from anciallary chunks

	protected final InputStream inputStream;
	protected InflaterInputStream idatIstream;
	protected PngIDatChunkInputStream iIdatCstream;

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
		this.inputStream = inputStream;
		this.chunksList = new ChunksList(null);
		this.metadata = new PngMetadata(chunksList);
		// starts reading: signature
		byte[] pngid = new byte[PngHelperInternal.pngIdBytes.length];
		PngHelperInternal.readBytes(inputStream, pngid, 0, pngid.length);
		offset += pngid.length;
		if (!Arrays.equals(pngid, PngHelperInternal.pngIdBytes))
			throw new PngjInputException("Bad PNG signature");
		// reads first chunk
		currentChunkGroup = ChunksList.CHUNK_GROUP_0_IDHR;
		int clen = PngHelperInternal.readInt4(inputStream);
		offset += 4;
		if (clen != 13)
			throw new RuntimeException("IDHR chunk len != 13 ?? " + clen);
		byte[] chunkid = new byte[4];
		PngHelperInternal.readBytes(inputStream, chunkid, 0, 4);
		if (!Arrays.equals(chunkid, ChunkHelper.b_IHDR))
			throw new PngjInputException("IHDR not found as first chunk??? [" + ChunkHelper.toString(chunkid) + "]");
		offset += 4;
		PngChunkIHDR ihdr = (PngChunkIHDR) readChunk(chunkid, clen, false);
		boolean alpha = (ihdr.getColormodel() & 0x04) != 0;
		boolean palette = (ihdr.getColormodel() & 0x01) != 0;
		boolean grayscale = (ihdr.getColormodel() == 0 || ihdr.getColormodel() == 4);
		// creates ImgInfo and imgLine, and allocates buffers
		imgInfo = new ImageInfo(ihdr.getCols(), ihdr.getRows(), ihdr.getBitspc(), alpha, grayscale, palette);
		imgLine = new ImageLine(imgInfo);
		// allocation: one extra byte for filter type one pixel
		rowbfilter = new byte[imgInfo.bytesPerRow + 1];
		rowb = new byte[imgInfo.bytesPerRow + 1];
		rowbprev = new byte[rowb.length];
		// some checks
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
		close();
	}

	private void close() {
		if (currentChunkGroup < ChunksList.CHUNK_GROUP_6_END) { // this could only happen if forced close
			try {
				idatIstream.close();
			} catch (Exception e) {
			}
			currentChunkGroup = ChunksList.CHUNK_GROUP_6_END;
		}
		if (shouldCloseStream) {
			try {
				inputStream.close();
			} catch (Exception e) {
				throw new PngjInputException("error closing input stream!", e);
			}
		}
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
			clen = PngHelperInternal.readInt4(inputStream);
			offset += 4;
			if (clen < 0)
				break;
			PngHelperInternal.readBytes(inputStream, chunkid, 0, 4);
			offset += 4;
			if (Arrays.equals(chunkid, ChunkHelper.b_IDAT)) {
				found = true;
				currentChunkGroup = ChunksList.CHUNK_GROUP_4_IDAT;
				// add dummy idat chunk to list
				chunksList.appendReadChunk(new PngChunkIDAT(imgInfo, clen, offset - 8), currentChunkGroup);
				break;
			} else if (Arrays.equals(chunkid, ChunkHelper.b_IEND)) {
				throw new PngjInputException("END chunk found before image data (IDAT) at offset=" + offset);
			}
			if (Arrays.equals(chunkid, ChunkHelper.b_PLTE))
				currentChunkGroup = ChunksList.CHUNK_GROUP_2_PLTE;
			readChunk(chunkid, clen, false);
			if (Arrays.equals(chunkid, ChunkHelper.b_PLTE))
				currentChunkGroup = ChunksList.CHUNK_GROUP_3_AFTERPLTE;
		}
		int idatLen = found ? clen : -1;
		if (idatLen < 0)
			throw new PngjInputException("first idat chunk not found!");
		iIdatCstream = new PngIDatChunkInputStream(inputStream, idatLen, offset);
		idatIstream = new InflaterInputStream(iIdatCstream);
	}

	/**
	 * Reads (and processes) chunks after last IDAT.
	 **/
	void readLastChunks() {
		// PngHelper.logdebug("idat ended? " + iIdatCstream.isEnded());
		currentChunkGroup = ChunksList.CHUNK_GROUP_5_AFTERIDAT;
		if (!iIdatCstream.isEnded())
			iIdatCstream.forceChunkEnd();
		int clen = iIdatCstream.getLenLastChunk();
		byte[] chunkid = iIdatCstream.getIdLastChunk();
		boolean endfound = false;
		boolean first = true;
		boolean skip = false;
		while (!endfound) {
			skip = false;
			if (!first) {
				clen = PngHelperInternal.readInt4(inputStream);
				offset += 4;
				if (clen < 0)
					throw new PngjInputException("bad len " + clen);
				PngHelperInternal.readBytes(inputStream, chunkid, 0, 4);
				offset += 4;
			}
			first = false;
			if (Arrays.equals(chunkid, ChunkHelper.b_IDAT)) {
				skip = true; // extra dummy (empty?) idat chunk, it can happen, ignore it
			} else if (Arrays.equals(chunkid, ChunkHelper.b_IEND)) {
				currentChunkGroup = ChunksList.CHUNK_GROUP_6_END;
				endfound = true;
			}
			readChunk(chunkid, clen, skip);
		}
		if (!endfound)
			throw new PngjInputException("end chunk not found - offset=" + offset);
		// PngHelper.logdebug("end chunk found ok offset=" + offset);
	}

	/**
	 * Reads chunkd from input stream, adds to ChunksList, and returns it <br>
	 * If it's skipped, a PngChunkSkipped object is created
	 */
	private PngChunk readChunk(byte[] chunkid, int clen, boolean skipforced) {
		// skipChunksByIdSet is created lazyly, if fist IHDR has already been read
		if (skipChunkIdsSet == null && currentChunkGroup > ChunksList.CHUNK_GROUP_0_IDHR)
			skipChunkIdsSet = new HashSet<String>(Arrays.asList(skipChunkIds));
		String chunkidstr = ChunkHelper.toString(chunkid);
		PngChunk pngChunk = null;
		boolean skip = skipforced;
		if (offset + clen > maxTotalBytesRead)
			throw new PngjInputException("Maximum total bytes to read exceeeded: " + maxTotalBytesRead + " offset:"
					+ offset);
		// an ancillary chunks can be skipped because several reasons:
		if (currentChunkGroup > ChunksList.CHUNK_GROUP_0_IDHR && !ChunkHelper.isCritical(chunkidstr))
			skip = skip || clen >= skipChunkMaxSize || skipChunkIdsSet.contains(chunkidstr)
					|| bytesChunksLoaded + clen > maxBytesMetadata
					|| !ChunkHelper.shouldLoad(chunkidstr, chunkLoadBehaviour);
		if (skip) {
			PngHelperInternal.skipBytes(inputStream, clen + 4);
			pngChunk = new PngChunkSkipped(chunkidstr, imgInfo, clen);
		} else {
			ChunkRaw chunk = new ChunkRaw(clen, chunkid, true);
			chunk.readChunkData(inputStream);
			pngChunk = PngChunk.factory(chunk, imgInfo);
			if (!pngChunk.crit)
				bytesChunksLoaded += chunk.len;
		}
		pngChunk.setOffset(offset - 8);
		chunksList.appendReadChunk(pngChunk, currentChunkGroup);
		offset += clen + 4;
		return pngChunk;
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
		imgLine.setFilterUsed(FilterType.getByVal(rowbfilter[0]));
		imgLine.setRown(nrow);
		return imgLine;
	}

	/**
	 * Like <code>readRow(int nrow)</code> but this accepts non consecutive rows.
	 * <p>
	 * If it's the current row, it will just return it. Elsewhere, it will try to read it. This implementation only
	 * accepts nrow greater or equal than current row, but an extended class could implement some partial or full cache
	 * of lines.
	 * <p>
	 * This should not not be mixed with calls to <code>readRow(int[] buffer, final int nrow)</code>
	 * 
	 * @param nrow
	 * @return
	 */
	public ImageLine getRow(int nrow) {
		while (rowNum < nrow)
			readRow(rowNum + 1);
		// now it should be positioned in the desired row
		if (rowNum != nrow || imgLine.getRown() != nrow)
			throw new PngjInputException("Invalid row: " + nrow);
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
		offset = iIdatCstream.getOffset();
		if (offset >= maxTotalBytesRead)
			throw new PngjInputException("Reading IDAT: Maximum total bytes to read exceeeded: " + maxTotalBytesRead
					+ " offset:" + offset);
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
	 * Set total maximum bytes to read (default: 200MB). <br>
	 * If exceeded, an exception will be thrown
	 */
	public void setMaxTotalBytesRead(long maxTotalBytesToRead) {
		this.maxTotalBytesRead = maxTotalBytesToRead;
	}

	/**
	 * @return Total maximum bytes to read.
	 */
	public long getMaxTotalBytesRead() {
		return maxTotalBytesRead;
	}

	/**
	 * Set total maximum bytes to load from ancillary chunks (default: 5Mb).<br>
	 * If exceeded, some chunks will be skipped
	 */
	public void setMaxBytesMetadata(int maxBytesChunksToLoad) {
		this.maxBytesMetadata = maxBytesChunksToLoad;
	}

	/**
	 * @return Total maximum bytes to load from ancillary ckunks.
	 */
	public int getMaxBytesMetadata() {
		return maxBytesMetadata;
	}

	/**
	 * Set maximum size in bytes for individual ancillary chunks (default: 2MB). <br>
	 * Chunks exceeding this length will be skipped (the CRC will not be checked) and the chunk will be saved as a
	 * PngChunkSkipped object. See also setSkipChunkIds
	 */
	public void setSkipChunkMaxSize(int skipChunksBySize) {
		this.skipChunkMaxSize = skipChunksBySize;
	}

	/**
	 * @return maximum size in bytes for individual ancillary chunks.
	 */
	public int getSkipChunkMaxSize() {
		return skipChunkMaxSize;
	}

	/**
	 * Chunks ids to be skipped. 
	 * <br>
	 * These chunks will be skipped (the CRC will not be checked) and the chunk will be saved
	 * as a PngChunkSkipped object. See also setSkipChunkMaxSize
	 */
	public void setSkipChunkIds(String[] skipChunksById) {
		this.skipChunkIds = skipChunksById;
	}

	/**
	 * @return Chunk-IDs to be skipped.
	 */
	public String[] getSkipChunkIds() {
		return skipChunkIds;
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
	 * Normally this does nothing, but it can be used to force a premature closing
	 */
	public void end() {
		if (currentChunkGroup < ChunksList.CHUNK_GROUP_6_END)
			close();
	}

	/**
	 * Basic info, for debugging.
	 */
	public String toString() { // basic info
		return "filename=" + filename + " " + imgInfo.toString();
	}

}
