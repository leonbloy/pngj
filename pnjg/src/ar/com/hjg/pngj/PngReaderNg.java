package ar.com.hjg.pngj;

import java.io.InputStream;
import java.util.HashSet;
import java.util.zip.CRC32;

import ar.com.hjg.pngj.ImageLine.SampleType;
import ar.com.hjg.pngj.chunks.ChunkLoadBehaviour;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.PngChunkIDAT;
import ar.com.hjg.pngj.chunks.PngMetadata;

/**
 * Reads a PNG image, line by line.
 * <p>
 * The reading sequence is as follows: <br>
 * 1. At construction time, the header and IHDR chunk are read (basic image
 * info) <br>
 * 2. Afterwards you can set some additional global options. Eg.
 * {@link #setUnpackedMode(boolean)}, {@link #setCrcCheckDisabled()}.<br>
 * 3. Optional: If you call getMetadata() or getChunksLisk() before start
 * reading the rows, all the chunks before IDAT are automatically loaded and
 * available <br>
 * 4a. The rows are read onen by one of the <tt>readRowXXX</tt> methods:
 * {@link #readRowInt(int)}, {@link PngReaderNg#readRowByte(int)}, etc, in
 * order, from 0 to nrows-1 (you can skip or repeat rows, but not go backwards)<br>
 * 4b. Alternatively, you can read all rows, or a subset, in a single call:
 * {@link #readRowsInt()}, {@link #readRowsByte()} ,etc. In general this
 * consumes more memory, but for interlaced images this is equally efficient,
 * and more so if reading a small subset of rows.<br>
 * 5. Read of the last row auyomatically loads the trailing chunks, and ends the
 * reader.<br>
 * 6. end() forcibly finishes/aborts the reading and closes the stream
 */
public class PngReaderNg {

	/**
	 * Basic image info - final and inmutable.
	 */
	public final ImageInfo imgInfo;
	/**
	 * not necesarily a filename, can be a description - merely informative
	 */
	protected final String filename;
	private ChunkLoadBehaviour chunkLoadBehaviour = ChunkLoadBehaviour.LOAD_CHUNK_ALWAYS; // see setter/getter
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
	// only set for interlaced PNG
	private final boolean interlaced;
	private boolean crcEnabled = true;
	// this only influences the 1-2-4 bitdepth format
	private boolean unpackedMode = false;
	/**
	 * Current chunk group, (0-6) already read or reading
	 * <p>
	 * see {@link ChunksList}
	 */
	protected CRC32 crctest; // If set to non null, it gets a CRC of the unfiltered bytes, to check for images equality
	private final ChunkReaderFullSequence2 chunkseq;
	private final BufferedStreamFeeder streamFeeder;
	private int rowNum;

	/**
	 * Constructs a PngReader from an InputStream.
	 * <p>
	 * See also <code>FileHelper.createPngReader(File f)</code> if available.
	 * 
	 * Reads only the signature and first chunk (IDHR)
	 * 
	 * @param filenameOrDescription
	 *            : Optional, can be a filename or a description. Just for
	 *            error/debug messages
	 * 
	 */
	public PngReaderNg(InputStream inputStream, String filenameOrDescription) {
		this.filename = filenameOrDescription == null ? "" : filenameOrDescription;
		this.chunksList = new ChunksList(null);
		this.metadata = new PngMetadata(chunksList);
		this.chunkseq = new ChunkReaderFullSequence2();
		streamFeeder = new BufferedStreamFeeder(inputStream);
		if (streamFeeder.feed(chunkseq, 21) != 21) // 8+13: signature+IHDR
			throw new PngjInputException("error reading first 21 bytes");
		imgInfo = chunkseq.imageInfo;
		interlaced = chunkseq.getDeinterlacer() != null;
	}

	/**
	 * Reads last Internally called after having read the last line. It reads
	 * extra chunks after IDAT, if present.
	 */

	private void close() {
		chunkseq.close();
		try {
			streamFeeder.end();
		} catch (Exception e) {
			throw new PngjInputException("error closing input stream!", e);
		}
	}

	private void readLastAndClose() {
		readLastChunks();
		close();
	}

	/**
	 * Reads chunks before first IDAT. Normally this is called automatically
	 * <p>
	 * Position before: after IDHR (crc included) Position after: just after the
	 * first IDAT chunk id
	 * <P>
	 * This can be called several times (tentatively), it does nothing if
	 * already run
	 * <p>
	 * (Note: when should this be called? in the constructor? hardly, because we
	 * loose the opportunity to call setChunkLoadBehaviour() and perhaps other
	 * settings before reading the first row? but sometimes we want to access
	 * some metadata (plte, phys) before. Because of this, this method can be
	 * called explicitly but is also called implicititly in some methods
	 * (getMetatada(), getChunksList())
	 */
	private final void readFirstChunks() {
		while (chunkseq.currentChunkGroup < ChunksList.CHUNK_GROUP_4_IDAT)
			streamFeeder.feed(chunkseq);
	}

	/**
	 * Reads (and processes) chunks after last IDAT.
	 **/
	void readLastChunks() {
		while (chunkseq.currentChunkGroup < ChunksList.CHUNK_GROUP_6_END && !streamFeeder.isEof())
			streamFeeder.feed(chunkseq);
	}

	protected ChunkReaderFullSequence2 chunkReaderFullSequence2Factory() {
		return new ChunkReaderFullSequence2() {

		};
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
	 * @see #setChunkLoadBehaviour(ChunkLoadBehaviour)
	 */
	public ChunkLoadBehaviour getChunkLoadBehaviour() {
		return chunkLoadBehaviour;
	}

	/**
	 * Determines which ancillary chunks (metada) are to be loaded
	 * 
	 * @param chunkLoadBehaviour
	 *            {@link ChunkLoadBehaviour}
	 */
	public void setChunkLoadBehaviour(ChunkLoadBehaviour chunkLoadBehaviour) {
		this.chunkLoadBehaviour = chunkLoadBehaviour;
	}

	/**
	 * All loaded chunks (metada). If we have not yet end reading the image,
	 * this will include only the chunks before the pixels data (IDAT)
	 * <p>
	 * Critical chunks are included, except that all IDAT chunks appearance are
	 * replaced by a single dummy-marker IDAT chunk. These might be copied to
	 * the PngWriter
	 * <p>
	 * 
	 * @see #getMetadata()
	 */
	public ChunksList getChunksList() {
		if (chunkseq.firstChunksNotYetRead())
			readFirstChunks();
		return chunksList;
	}

	int getCurrentChunkGroup() {
		return chunkseq.currentChunkGroup;
	}

	/**
	 * High level wrapper over chunksList
	 * 
	 * @see #getChunksList()
	 */
	public PngMetadata getMetadata() {
		if (chunkseq.firstChunksNotYetRead())
			readFirstChunks();
		return metadata;
	}
	
	protected ImageLine createImageLine() {
		return new ImageLine(imgInfo, SampleType.INT, unpackedMode);
	}
	
	/**
	 * If called for first time, calls readRowInt. Elsewhere, it calls the
	 * appropiate readRowInt/readRowByte
	 * <p>
	 * In general, specifying the concrete readRowInt/readRowByte is preferrable
	 * 
	 * @see #readRowInt(int) {@link #readRowByte(int)}
	 */
	public ImageLine readRow(int nrow) {
		if (imgLine == null)
			imgLine = createImageLine();
		if (imgLine.getRown() == nrow) // already read
			return imgLine;
		if (!interlaced) {
			if (nrow <= rowNum)
				throw new PngjInputException("rows must be read in increasing order: " + nrow);
			int bytesread = 0;
			while (rowNum < nrow)
				bytesread = readRowRaw(rowNum + 1); // read rows, perhaps skipping if necessary
			imgLine.fromPngRaw(chunkseq.getCurReaderIdatSet().getUnfilteredRow(),bytesread);
			return imgLine;
		} else { // interlaced
			/*if (deinterlacer.getImageInt() == null)
			deinterlacer.setImageInt(readRowsInt().scanlines); // read all image and store it in deinterlacer
		System.arraycopy(deinterlacer.getImageInt()[nrow], 0, buffer, 0, unpackedMode ? imgInfo.samplesPerRow
				: imgInfo.samplesPerRowPacked);
				*/
		throw new RuntimeException("interlaced not implemented");
		}
	}

	

	/**
	 * Reads the row as INT, storing it in the {@link #imgLine} property and
	 * returning it.
	 * 
	 * The row must be greater or equal than the last read row.
	 * 
	 * @param nrow
	 *            Row number, from 0 to rows-1. Increasing order.
	 * @return ImageLine object, also available as field. Data is in
	 *         {@link ImageLine#scanline} (int) field.
	 */
	public ImageLine readRowInt(int nrow) {
		if (imgLine == null)
			imgLine = new ImageLine(imgInfo, SampleType.INT, unpackedMode);
		if(imgLine.sampleType!=SampleType.INT) throw new PngjException("cannot mix int and byte reading");
		return readRow(nrow);
	}

	/**
	 * Reads the row as BYTES, storing it in the {@link #imgLine} property and
	 * returning it.
	 * 
	 * The row must be greater or equal than the last read row. This method
	 * allows to pass the same row that was last read.
	 * 
	 * @param nrow
	 *            Row number, from 0 to rows-1. Increasing order.
	 * @return ImageLine object, also available as field. Data is in
	 *         {@link ImageLine#scanlineb} (byte) field.
	 */
	public ImageLine readRowByte(int nrow) {
		if (imgLine == null)
			imgLine = new ImageLine(imgInfo, SampleType.BYTE, unpackedMode);
		if(imgLine.sampleType!=SampleType.BYTE) throw new PngjException("cannot mix int and byte reading");
		return readRow(nrow);
	}

	/**
	 * @see #readRowInt(int[], int)
	 */
	public final int[] readRow(int[] buffer, final int nrow) {
		return readRowInt(buffer, nrow);
	}

	/**
	 * Reads a line and returns it as a int[] array.
	 * <p>
	 * You can pass (optionally) a prealocatted buffer.
	 * <p>
	 * If the bitdepth is less than 8, the bytes are packed - unless
	 * {@link #unpackedMode} is true.
	 * 
	 * @param buffer
	 *            Prealocated buffer, or null.
	 * @param nrow
	 *            Row number (0 is top). Most be strictly greater than the last
	 *            read row.
	 * 
	 * @return The scanline in the same passwd buffer if it was allocated, a
	 *         newly allocated one otherwise
	 */
	public final int[] readRowInt(int[] buffer, final int nrow) {
		if(imgLine==null) imgLine = new ImageLine(imgInfo, SampleType.INT, unpackedMode ,buffer,null);
		return readRowInt(nrow).scanline;
	}

	/**
	 * Reads a line and returns it as a byte[] array.
	 * <p>
	 * You can pass (optionally) a prealocatted buffer.
	 * <p>
	 * If the bitdepth is less than 8, the bytes are packed - unless
	 * {@link #unpackedMode} is true. <br>
	 * If the bitdepth is 16, the least significant byte is lost.
	 * <p>
	 * 
	 * @param buffer
	 *            Prealocated buffer, or null.
	 * @param nrow
	 *            Row number (0 is top). Most be strictly greater than the last
	 *            read row.
	 * 
	 * @return The scanline in the same passwd buffer if it was allocated, a
	 *         newly allocated one otherwise
	 */
	public final byte[] readRowByte(byte[] buffer, final int nrow) {
		if(imgLine==null) imgLine = new ImageLine(imgInfo, SampleType.BYTE, unpackedMode ,null,buffer);
		return readRowByte(nrow).scanlineb;
	}

	/*
	 * For the interlaced case, nrow indicates the subsampled image - the pass must be set already.
	 * 
	 * This must be called in strict order, both for interlaced or no interlaced.
	 * 
	 * Updates rowNum.
	 * 
	 * result is in idat.getRow (with the filter byte prepended)
	 * 
	 * Returns bytes actually read (not including the filter byte)
	 */
	private int readRowRaw(final int nrow) {
		if (nrow == 0) {
			if (chunkseq.firstChunksNotYetRead())
				readFirstChunks();
		}
		int bytesRead = imgInfo.bytesPerRow; // NOT including the filter byte
		if (interlaced) {/*
			if (nrow < 0 || nrow > deinterlacer.getRows() || (nrow != 0 && nrow != deinterlacer.getCurrRowSubimg() + 1))
				throw new PngjInputException("invalid row in interlaced mode: " + nrow);
			deinterlacer.setRow(nrow);
			bytesRead = (imgInfo.bitspPixel * deinterlacer.getPixelsToRead() + 7) / 8;
			if (bytesRead < 1)
				throw new PngjExceptionInternal("wtf??");
				*/
			throw new PngjExceptionInternal("itnerlaced not done");
			
		} else { // check for non interlaced
			if (nrow < 0 || nrow >= imgInfo.rows || nrow != rowNum + 1)
				throw new PngjInputException("invalid row: " + nrow);
		}
		rowNum = nrow;
		ChunkReaderIdatSet idat = chunkseq.getCurReaderIdatSet();
		boolean ok=readNextRowFromIdat();
		if(!ok) throw new PngjInputException("error reading row " +rowNum);
		if(idat.getRowFilled()!=bytesRead+1) throw new PngjInputException("error reading row " +rowNum + ", read  " + idat.getRowFilled() + " bytes");
		if ((rowNum == imgInfo.rows - 1 && !interlaced) || (interlaced && chunkseq.deinterlacer.isAtLastRow()))
			readLastAndClose();
		return bytesRead;
	}

	/**
	 * if returns true, a new row is available in idat.
	 * @return
	 */
	private boolean readNextRowFromIdat() {
		while (!chunkseq.getCurReaderIdatSet().isDataPendingForConsumer() && !chunkseq.getCurReaderIdatSet().isAllDone()) {
			if(streamFeeder.feed(chunkseq)<1) return false;
		}
		if(chunkseq.getCurReaderIdatSet().isDataPendingForConsumer()) {
			return true;
		} 
		else return false;
	}
	
	/**
	 * Reads all the (remaining) file, skipping the pixels data. This is much
	 * more efficient that calling readRow(), specially for big files (about 10
	 * times faster!), because it doesn't even decompress the IDAT stream and
	 * disables CRC check Use this if you are not interested in reading
	 * pixels,only metadata.
	 */
	public void readSkippingAllRows() {
		chunkseq.addChunkToSkip(PngChunkIDAT.ID);
		if (chunkseq.firstChunksNotYetRead())
			readFirstChunks();
		readLastAndClose();
	}

	/**
	 * Set total maximum bytes to read (0: unlimited; default: 200MB). <br>
	 * These are the bytes read (not loaded) in the input stream. If exceeded,
	 * an exception will be thrown.
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
	 * Set total maximum bytes to load from ancillary chunks (0: unlimited;
	 * default: 5Mb).<br>
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
	 * Set maximum size in bytes for individual ancillary chunks (0: unlimited;
	 * default: 2MB). <br>
	 * Chunks exceeding this length will be skipped (the CRC will not be
	 * checked) and the chunk will be saved as a PngChunkSkipped object. See
	 * also setSkipChunkIds
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
	 * Chunks ids to be skipped. <br>
	 * These chunks will be skipped (the CRC will not be checked) and the chunk
	 * will be saved as a PngChunkSkipped object. See also setSkipChunkMaxSize
	 */
	public void setSkipChunkIds(String[] skipChunksById) {
		this.skipChunkIds = skipChunksById == null ? new String[] {} : skipChunksById;
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
		streamFeeder.setCloseOnEof(shouldCloseStream);
	}

	/**
	 * Normally this does nothing, but it can be used to force a premature
	 * closing. Its recommended practice to call it after reading the image
	 * pixels.
	 */
	public void end() {
		if (chunkseq.currentChunkGroup < ChunksList.CHUNK_GROUP_6_END)
			close();
	}

	/**
	 * Interlaced PNG is accepted -though not welcomed- now...
	 */
	public boolean isInterlaced() {
		return interlaced;
	}

	/**
	 * set/unset "unpackedMode"<br>
	 * If false (default) packed types (bitdepth=1,2 or 4) will keep several
	 * samples packed in one element (byte or int) <br>
	 * If true, samples will be unpacked on reading, and each element in the
	 * scanline will be sample. This implies more processing and memory, but
	 * it's the most efficient option if you intend to read individual pixels. <br>
	 * This option should only be set before start reading.
	 * 
	 * @param unPackedMode
	 */
	public void setUnpackedMode(boolean unPackedMode) {
		this.unpackedMode = unPackedMode;
	}

	/**
	 * @see PngReaderNg#setUnpackedMode(boolean)
	 */
	public boolean isUnpackedMode() {
		return unpackedMode;
	}

	/**
	 * Tries to reuse the allocated buffers from other already used PngReader
	 * object. This will have no effect if the buffers are smaller than
	 * necessary. It also reuses the inflater.
	 * 
	 * @param other
	 *            A PngReader that has already finished reading pixels. Can be
	 *            null.
	 */
	public void reuseBuffersFrom(PngReaderNg other) {
		throw new RuntimeException("not implemenetd");
	}

	/**
	 * Disables the CRC integrity check in IDAT chunks and ancillary chunks,
	 * this gives a slight increase in reading speed for big files
	 */
	public void setCrcCheckDisabled() {
		crcEnabled = false;
	}

	/**
	 * Just for testing. TO be called after ending reading, only if
	 * initCrctest() was called before start
	 * 
	 * @return CRC of the raw pixels values
	 */
	long getCrctestVal() {
		return crctest.getValue();
	}

	/**
	 * Inits CRC object and enables CRC calculation
	 */
	void initCrctest() {
		this.crctest = new CRC32();
	}

	/**
	 * Basic info, for debugging.
	 */
	public String toString() { // basic info
		return "filename=" + filename + " " + imgInfo.toString();
	}
}
