package ar.com.hjg.pngj;

import java.io.File;
import java.io.InputStream;
import java.util.zip.CRC32;

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
 * {@link #readRowInt(int)}, {@link PngReader#readRowByte(int)}, etc, in
 * order, from 0 to nrows-1 (you can skip or repeat rows, but not go backwards)<br>
 * 4b. Alternatively, you can read all rows, or a subset, in a single call:
 * {@link #readRowsInt()}, {@link #readRowsByte()} ,etc. In general this
 * consumes more memory, but for interlaced images this is equally efficient,
 * and more so if reading a small subset of rows.<br>
 * 5. Read of the last row auyomatically loads the trailing chunks, and ends the
 * reader.<br>
 * 6. end() forcibly finishes/aborts the reading and closes the stream
 */
public class PngReader {

	/**
	 * Basic image info - final and inmutable.
	 */
	public final ImageInfo imgInfo;
	private final boolean interlaced;
	private ChunkSeqReaderPng chunkseq;
	private BufferedStreamFeeder streamFeeder;

	private ChunkLoadBehaviour chunkLoadBehaviour = ChunkLoadBehaviour.LOAD_CHUNK_ALWAYS; // see setter/getter
	// some performance/defensive limits
	private static final long maxTotalBytesReadDefault = 901001001L; // ~ 900MB
	private static final long maxBytesMetadataDefault = 5024024; // for ancillary chunks 
	private static final long skipChunkMaxSizeDefault = 2024024; // chunks exceeding this size will be skipped (nor even CRC checked)
	CRC32 idatCrc;//for testing
	protected final PngMetadata metadata; // this a wrapper over chunks

	protected IImageLine imgLine;  // storage for each line, in non-interlaced mode 
	protected ImageLines imgLines; // storage for set of lines, in interlaced mode (or groupal reading)

	private int rowNum; // current row number (already read)

	/**
	 * factory to be used to create new lines. This is used in the default 
	 */
	protected IImageLineFactory<? extends IImageLine> imageLineFactory;

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
	public PngReader(InputStream inputStream) {
		try {
			this.chunkseq = new ChunkSeqReaderPng(false); // this works only in polled mode
			streamFeeder = new BufferedStreamFeeder(inputStream);
			streamFeeder.setFailIfNoFeed(true);
			if (!streamFeeder.feedFixed(chunkseq, 36)) // 8+13+12: signature+IHDR
				throw new PngjInputException("error reading first 21 bytes");
			imgInfo = chunkseq.imageInfo;
			interlaced = chunkseq.getDeinterlacer() != null;
			setMaxBytesMetadata(maxBytesMetadataDefault);
			setMaxTotalBytesRead(maxTotalBytesReadDefault);
			setSkipChunkMaxSize(skipChunkMaxSizeDefault);
			this.metadata = new PngMetadata(chunkseq.chunksList);
			rowNum = -1;
		} catch (RuntimeException e) { // in case of exception in constructor, we close the input stream (seems reasonable)
			try {
				inputStream.close();
			} catch (Exception ee) {
			}
			throw e;
		}
	}

	public PngReader(File file) {
		this(PngHelperInternal.istreamFromFile(file));
		setShouldCloseStream(true);
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
	public void readFirstChunks() {
		while (chunkseq.currentChunkGroup < ChunksList.CHUNK_GROUP_4_IDAT)
			streamFeeder.feed(chunkseq);
	}

	/**
	 * Reads (and processes) chunks after last IDAT.
	 **/
	public void readLastChunks() {
		while (chunkseq.getIdatSet() != null && !chunkseq.getIdatSet().isDone())
			streamFeeder.feed(chunkseq);
		while (!chunkseq.isDone())
			streamFeeder.feed(chunkseq);
	}

	/**
	 * Logs/prints a warning.
	 * <p>
	 * The default behaviour is print to stderr, but it can be overriden.
	 * <p>
	 * This happens rarely - most errors are fatal.
	 */
	protected void logWarn(String warn) {
		PngHelperInternal.LOGGER.warning(warn);
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
		return chunkseq.chunksList;
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

	public IImageLine readRow() {
		return readRow(rowNum + 1);
	}

	/**
	 * This must be called in ascending order (not necessarily consecutive)
	 */
	public IImageLine readRow(int nrow) {
		if (chunkseq.firstChunksNotYetRead())
			readFirstChunks();
		if (!interlaced) {
			if (imgLine == null)
				imgLine = getImageLineFactory().createImageLine(imgInfo);
			if (nrow == rowNum)
				return imgLine;
			else if (nrow < rowNum)
				throw new PngjInputException("rows must be read in increasing order: " + nrow);
			while (rowNum < nrow) {
				while (!chunkseq.getIdatSet().isRowReady())
					streamFeeder.feed(chunkseq);
				rowNum++;
				chunkseq.getIdatSet().updateCrc(idatCrc);
				if (rowNum == nrow) {
					imgLine.fromPngRaw(chunkseq.getIdatSet().getUnfilteredRow(), imgInfo.bytesPerRow + 1, 0, 1);
					imgLine.end();
				}
				chunkseq.getIdatSet().advanceToNextRow();
			}
			return imgLine;
		} else { // and now, for something completely different (interlaced!)
			if (imgLines == null) {
				//TODO fix this
				imgLines = new ImageLines(imgInfo.rows, 0, 1, getImageLineFactory(), imgInfo);
				loadAllInterlaced(imgInfo.rows, 0, 1);
			}
			rowNum = nrow;
			return imgLines.getImageLine(nrow);
		}

	}

	public ImageLines readRows() {
		return readRows(imgInfo.rows, 0, 1);
	}

	public ImageLines readRows(int nRows, int rowOffset, int rowStep) {
		if (chunkseq.firstChunksNotYetRead())
			readFirstChunks();
		if (nRows < 0)
			nRows = (imgInfo.rows - rowOffset) / rowStep;
		if (rowStep < 1 || rowOffset < 0 || nRows == 0 || nRows * rowStep + rowOffset > imgInfo.rows)
			throw new PngjInputException("bad args");
		if (rowNum >= 0)
			throw new PngjInputException("readRows cannot be mixed with readRow");
		imgLines = new ImageLines(nRows, rowOffset, rowStep, getImageLineFactory(), imgInfo);
		if (!interlaced) {
			int m = -1; // last row already read in ImageLinesN
			while (m < nRows - 1) {
				while (!chunkseq.getIdatSet().isRowReady())
					streamFeeder.feed(chunkseq);
				rowNum++;
				chunkseq.getIdatSet().updateCrc(idatCrc);
				m = (rowNum - rowOffset) / rowStep;
				if (rowNum >= rowOffset && rowStep * m + rowOffset == rowNum) {
					imgLines.getImageLine(m).fromPngRaw(chunkseq.getIdatSet().getUnfilteredRow(),
							imgInfo.bytesPerRow + 1, 0, 1);
					imgLines.getImageLine(m).end();
				}
				chunkseq.getIdatSet().advanceToNextRow();
			}
		} else { // and now, for something completely different (interlaced)
			loadAllInterlaced(nRows, rowOffset, rowStep);
		}
		chunkseq.getIdatSet().end();
		end();
		return imgLines;
	}

	/**
	 * By default this factory method uses the factory, but you can override
	 * this
	 * 
	 * @return
	 */
	protected IImageLine getImageLineInstance() {
		return getImageLineFactory().createImageLine(imgInfo);
	}

	/**
	 * lazy creation: uses the default INT
	 */
	public IImageLineFactory<? extends IImageLine> getImageLineFactory() {
		if (imageLineFactory == null)
			imageLineFactory = ImageLine.getFactory(imgInfo);
		return imageLineFactory;
	}

	public void setImageLineFactory(IImageLineFactory<? extends IImageLine> imageLineFactory) {
		this.imageLineFactory = imageLineFactory;
	}

	protected void loadAllInterlaced(int nRows, int rowOffset, int rowStep) {
		IdatSet idat = chunkseq.getIdatSet();
		do {
			while (!chunkseq.getIdatSet().isRowReady())
				streamFeeder.feed(chunkseq);
			if (idatCrc != null)
				chunkseq.getIdatSet().updateCrc(idatCrc);
			int i = idat.rowinfo.rowNreal;
			int m = imgLines.imageRowToMatrixRowStrict(i);
			if (m >= 0) {
				imgLines.getImageLine(m).fromPngRaw(idat.getUnfilteredRow(), idat.rowinfo.buflen, idat.rowinfo.oX,
						idat.rowinfo.dX);
			}
			chunkseq.getIdatSet().advanceToNextRow();
		} while (!idat.isDone());
		for (int i = 0; i < imgLines.size(); i++)
			imgLines.getImageLine(i).end();
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
		end();
	}

	/**
	 * Set total maximum bytes to read (0: unlimited; default: 200MB). <br>
	 * These are the bytes read (not loaded) in the input stream. If exceeded,
	 * an exception will be thrown.
	 */
	public void setMaxTotalBytesRead(long maxTotalBytesToRead) {
		chunkseq.setMaxTotalBytesRead(maxTotalBytesToRead);
	}

	/**
	 * Set total maximum bytes to load from ancillary chunks (0: unlimited;
	 * default: 5Mb).<br>
	 * If exceeded, some chunks will be skipped
	 */
	public void setMaxBytesMetadata(long maxBytesMetadata) {
		chunkseq.setMaxBytesMetadata(maxBytesMetadata);
	}

	/**
	 * Set maximum size in bytes for individual ancillary chunks (0: unlimited;
	 * default: 2MB). <br>
	 * Chunks exceeding this length will be skipped (the CRC will not be
	 * checked) and the chunk will be saved as a PngChunkSkipped object. See
	 * also setSkipChunkIds
	 */
	public void setSkipChunkMaxSize(long skipChunkMaxSize) {
		chunkseq.setSkipChunkMaxSize(skipChunkMaxSize);
	}

	/**
	 * Chunks ids to be skipped. <br>
	 * These chunks will be skipped (the CRC will not be checked) and the chunk
	 * will be saved as a PngChunkSkipped object. See also setSkipChunkMaxSize
	 */
	public void setChunksToSkip(String... chunksToSkip) {
		chunkseq.setChunksToSkip(chunksToSkip);
	}

	public void addChunkToSkip(String chunkToSkip) {
		chunkseq.addChunkToSkip(chunkToSkip);
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
	 * 
	 * Normally this does nothing, but it can be used to force a premature
	 * closing. Its recommended practice to call it after reading the image
	 * pixels.
	 */
	public void end() {
		try {
			readLastChunks();
		} finally {
			close();
		}
	}

	/**
	 * Releases resources, and closes stream if corresponds. Idempotent, secure,
	 * no exceptions.
	 * 
	 * This can be also called for abort. It is recommended to call this in case
	 * of exceptions
	 */
	public void close() {
		try {
			chunkseq.close();
		} catch (Exception e) {
			PngHelperInternal.LOGGER.warning("error closing chunk sequence:" + e.toString());
		}
		try {
			streamFeeder.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Interlaced PNG is accepted -though not welcomed- now...
	 */
	public boolean isInterlaced() {
		return interlaced;
	}

	/**
	 * Disables the CRC integrity check in IDAT chunks and ancillary chunks,
	 * this gives a slight increase in reading speed for big files
	 */
	public void setCrcCheckDisabled() {
		chunkseq.setCheckCrc(false);
	}

	/**
	 * Basic info, for debugging.
	 */
	public String toString() { // basic info
		return imgInfo.toString() + " interlaced=" + interlaced;
	}

	public ChunkSeqReaderPng getChunkseq() {
		return chunkseq;
	}

}
