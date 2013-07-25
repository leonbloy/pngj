package ar.com.hjg.pngj;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

import ar.com.hjg.pngj.chunks.ChunkLoadBehaviour;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.PngChunkIDAT;
import ar.com.hjg.pngj.chunks.PngMetadata;

/**
 * Reads a PNG image (pixels and/or metadata) from a file or stream. Each row is
 * read as an {@link ImageLineInt} object (one int per sample), but this can be
 * changed by setting a different ImageLineFactory
 * 
 * Internally, this wraps a {@link ChunkSeqReaderPng} with a
 * {@link BufferedStreamFeeder}
 * 
 * The reading sequence is as follows: <br>
 * 1. At construction time, the header and IHDR chunk are read (basic image
 * info) <br>
 * 2. Afterwards you can set some additional global options. Eg.
 * {@link #setCrcCheckDisabled()}.<br>
 * 3. Optional: If you call getMetadata() or getChunksLisk() before start
 * reading the rows, all the chunks before IDAT are automatically loaded and
 * available <br>
 * 4a. The rows are read onen by one of the <tt>readRowXXX</tt> methods:
 * {@link #readRow(int)}, {@link PngReader#readRowByte(int)}, etc, in order,
 * from 0 to nrows-1 (you can skip or repeat rows, but not go backwards)<br>
 * 4b. Alternatively, you can read all rows, or a subset, in a single call:
 * {@link #readRowsInt()}, {@link #readRowsByte()} ,etc. In general this
 * consumes more memory, but for interlaced images this is equally efficient,
 * and more so if reading a small subset of rows.<br>
 * 5. Read of the last row auyomatically loads the trailing chunks, and ends the
 * reader.<br>
 * 6. end() forcibly finishes/aborts the reading and closes the stream
 */
public class PngReader {
	// some performance/defensive limits
	private static final long maxTotalBytesReadDefault = 901001001L; // ~ 900MB
	private static final long maxBytesMetadataDefault = 5024024; // for ancillary chunks 
	private static final long skipChunkMaxSizeDefault = 2024024; // chunks exceeding this size will be skipped (nor even CRC checked)

	/**
	 * Basic image info - final and inmutable.
	 */
	public final ImageInfo imgInfo;
	public final boolean interlaced;

	protected ChunkSeqReaderPng chunkseq;
	protected BufferedStreamFeeder streamFeeder;

	private ChunkLoadBehaviour chunkLoadBehaviour = ChunkLoadBehaviour.LOAD_CHUNK_ALWAYS; // see setter/getter
	protected final PngMetadata metadata; // this a wrapper over chunks
	protected int rowNum; // current row number (already read)

	CRC32 idatCrc;//for testing

	protected IImageLineSet<? extends IImageLine> imlinesSet;
	private IImageLineSetFactory<? extends IImageLine> imageLineSetFactory;

	/**
	 * Construct a PngReader object from a stream, with default options. This
	 * reads the signature and the first IHDR chunk only.
	 * 
	 * Warning: In case of exception the stream is NOT closed.
	 * 
	 * @param inputStream
	 *            PNG stream
	 */
	public PngReader(InputStream inputStream) {
		this(inputStream, false);
	}

	/**
	 * Constructs a PngReader opening a file. If it succeeds, it sets
	 * <tt>setShouldCloseStream(true)</tt> In case of exception the file stream
	 * is closed
	 * 
	 * @param file
	 *            PNG image file
	 */
	public PngReader(File file) {
		this(PngHelperInternal.istreamFromFile(file), true);
		setShouldCloseStream(true);
	}

	private PngReader(InputStream inputStream, boolean closeStreamIfError) {
		try {
			this.chunkseq = new ChunkSeqReaderPng(false); // this works only in polled mode
			streamFeeder = new BufferedStreamFeeder(inputStream);
			streamFeeder.setFailIfNoFeed(true);
			if (!streamFeeder.feedFixed(chunkseq, 36)) // 8+13+12=36 PNG signature+IHDR chunk
				throw new PngjInputException("error reading first 21 bytes");
			imgInfo = chunkseq.getImageInfo();
			interlaced = chunkseq.getDeinterlacer() != null;
			setMaxBytesMetadata(maxBytesMetadataDefault);
			setMaxTotalBytesRead(maxTotalBytesReadDefault);
			setSkipChunkMaxSize(skipChunkMaxSizeDefault);
			this.metadata = new PngMetadata(chunkseq.chunksList);
			// sets a default factory (with ImageLineInt), 
			// this can be overwrite by a extended constructor, or by a setter
			setLineSetFactory(ImageLineSetDefault.getFactoryInt());
			rowNum = -1;
		} catch (RuntimeException e) {
			if (closeStreamIfError)
				try {
					inputStream.close();
				} catch (IOException e1) {
				}
			throw e;
		}
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
	protected void readFirstChunks() {
		while (chunkseq.currentChunkGroup < ChunksList.CHUNK_GROUP_4_IDAT)
			streamFeeder.feed(chunkseq);
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

	/**
	 * Reads next row. 
	 * 
	 * The caller must know that there are more rows to read.
	 * 
	 * @return Never null. Throws PngInputException if no more
	 */
	public IImageLine readRow() {
		return readRow(rowNum + 1);
	}

	public boolean hasMoreRows() {
		return rowNum < imgInfo.rows - 1;
	}

	/**
	 * The row number is mostly meant as a check, the rows must be called in
	 * ascending order (not necessarily consecutive)
	 */
	public IImageLine readRow(int nrow) {
		if (chunkseq.firstChunksNotYetRead())
			readFirstChunks();
		if (!interlaced) {
			if (imlinesSet == null)
				imlinesSet = createLineSet(true, 1, 0, 1);
			IImageLine line = imlinesSet.getImageLine(nrow);
			if (nrow == rowNum)
				return line; // already read??
			else if (nrow < rowNum)
				throw new PngjInputException("rows must be read in increasing order: " + nrow);
			while (rowNum < nrow) {
				while (!chunkseq.getIdatSet().isRowReady())
					streamFeeder.feed(chunkseq);
				rowNum++;
				chunkseq.getIdatSet().updateCrc(idatCrc);
				if (rowNum == nrow) {
					line.readFromPngRaw(chunkseq.getIdatSet().getUnfilteredRow(), imgInfo.bytesPerRow + 1, 0, 1);
					line.endReadFromPngRaw();
				}
				chunkseq.getIdatSet().advanceToNextRow();
			}
			return line;
		} else { // and now, for something completely different (interlaced!)
			if (imlinesSet == null) {
				imlinesSet = createLineSet(false, imgInfo.rows, 0, 1);
				loadAllInterlaced(imgInfo.rows, 0, 1);
			}
			rowNum = nrow;
			return imlinesSet.getImageLine(nrow);
		}

	}

	public IImageLineSet<? extends IImageLine> readRows() {
		return readRows(imgInfo.rows, 0, 1);
	}

	public IImageLineSet<? extends IImageLine> readRows(int nRows, int rowOffset, int rowStep) {
		if (chunkseq.firstChunksNotYetRead())
			readFirstChunks();
		if (nRows < 0)
			nRows = (imgInfo.rows - rowOffset) / rowStep;
		if (rowStep < 1 || rowOffset < 0 || nRows == 0 || nRows * rowStep + rowOffset > imgInfo.rows)
			throw new PngjInputException("bad args");
		if (rowNum >= 0)
			throw new PngjInputException("readRows cannot be mixed with readRow");
		imlinesSet = createLineSet(false, nRows, rowOffset, rowStep);
		if (!interlaced) {
			int m = -1; // last row already read in 
			while (m < nRows - 1) {
				while (!chunkseq.getIdatSet().isRowReady())
					streamFeeder.feed(chunkseq);
				rowNum++;
				chunkseq.getIdatSet().updateCrc(idatCrc);
				m = (rowNum - rowOffset) / rowStep;
				if (rowNum >= rowOffset && rowStep * m + rowOffset == rowNum) {
					IImageLine line = imlinesSet.getImageLine(rowNum);
					line.readFromPngRaw(chunkseq.getIdatSet().getUnfilteredRow(), imgInfo.bytesPerRow + 1, 0, 1);
					line.endReadFromPngRaw();
				}
				chunkseq.getIdatSet().advanceToNextRow();
			}
		} else { // and now, for something completely different (interlaced)
			loadAllInterlaced(nRows, rowOffset, rowStep);
		}
		chunkseq.getIdatSet().end();
		end();
		return imlinesSet;
	}

	/**
	 * Sets the factory that creates the ImageLine. By default, this
	 * implementation uses ImageLineInt but this can be changed (at construction
	 * time or later) by calling this method.
	 * 
	 * See also createLineSet
	 * 
	 * @param factory
	 */
	public void setLineSetFactory(IImageLineSetFactory<? extends IImageLine> factory) {
		imageLineSetFactory = factory;
	}

	/**
	 * By default this uses the factory (which, by default creates
	 * ImageLineInt). You should rarely override this.
	 */
	protected IImageLineSet<? extends IImageLine> createLineSet(boolean singleCursor, int nlines, int noffset, int step) {
		return imageLineSetFactory.create(imgInfo, singleCursor, nlines, noffset, step);
	}

	protected void loadAllInterlaced(int nRows, int rowOffset, int rowStep) {
		IdatSet idat = chunkseq.getIdatSet();
		int nread = 0;
		do {
			while (!chunkseq.getIdatSet().isRowReady())
				streamFeeder.feed(chunkseq);
			if (idatCrc != null)
				chunkseq.getIdatSet().updateCrc(idatCrc);
			int rowNumreal = idat.rowinfo.rowNreal;
			boolean inset = (rowNumreal - rowOffset) % rowStep == 0;
			if (inset) {
				imlinesSet.getImageLine(rowNumreal).readFromPngRaw(idat.getUnfilteredRow(), idat.rowinfo.buflen,
						idat.rowinfo.oX, idat.rowinfo.dX);
				nread++;
			}
			idat.advanceToNextRow();
		} while (nread < nRows || !idat.isDone());
		idat.end();
		for (int i = 0, j = rowOffset; i < nRows; i++, j += rowStep) {
			imlinesSet.getImageLine(j).endReadFromPngRaw();
		}
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
		streamFeeder.setCloseStream(shouldCloseStream);
	}

	/**
	 * Reads till end of PNG stream and call <tt>close()</tt>
	 * 
	 * This should normally be called after reading the pixel data, to read the
	 * trailing chunks and close the stream. But it can be called at anytime.
	 * This will also read the first chunks if not still read, and skip pixels
	 * (IDAT) if still pending.
	 * 
	 * If you want to read all metadata skipping pixels, readSkippingAllRows()
	 * is a little more efficient.
	 * 
	 * If you want to abort immediately, call instead <tt>close()</tt>
	 */
	public void end() {
		try {
			if (chunkseq.firstChunksNotYetRead())
				readFirstChunks();
			if (chunkseq.getIdatSet() != null && !chunkseq.getIdatSet().isDone())
				chunkseq.getIdatSet().end();
			while (!chunkseq.isDone())
				streamFeeder.feed(chunkseq);
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
