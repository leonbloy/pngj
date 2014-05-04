package ar.com.hjg.pngj;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

import ar.com.hjg.pngj.chunks.ChunkLoadBehaviour;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.PngChunkIDAT;
import ar.com.hjg.pngj.chunks.PngMetadata;

/**
 * Reads a PNG image (pixels and/or metadata) from a file or stream.
 * <p>
 * Each row is read as an {@link ImageLineInt} object (one int per sample), but this can be changed by setting a
 * different ImageLineFactory
 * <p>
 * Internally, this wraps a {@link ChunkSeqReaderPng} with a {@link BufferedStreamFeeder}
 * <p>
 * The reading sequence is as follows: <br>
 * 1. At construction time, the header and IHDR chunk are read (basic image info) <br>
 * 2. Afterwards you can set some additional global options. Eg. {@link #setCrcCheckDisabled()}.<br>
 * 3. Optional: If you call getMetadata() or getChunksLisk() before start reading the rows, all the chunks before IDAT
 * are then loaded and available <br>
 * 4a. The rows are read in order by calling {@link #readRow()}. You can also call {@link #readRow(int)} to skip rows
 * -but you can't go backwards, at least not with this implementation. This method returns a {@link IImageLine} object
 * which can be casted to the concrete class. This class returns by default a {@link ImageLineInt}, but this can be
 * changed.<br>
 * 4b. Alternatively, you can read all rows, or a subset, in a single call: {@link #readRows()},
 * {@link #readRows(int, int, int)} ,etc. In general this consumes more memory, but for interlaced images this is
 * equally efficient, and more so if reading a small subset of rows.<br>
 * 5. Reading of the last row automatically loads the trailing chunks, and ends the reader.<br>
 * 6. end() also loads the trailing chunks, if not done, and finishes cleanly the reading and closes the stream.
 */
public class PngReader {
	// some performance/defensive limits
	/**
	 * Defensive limit: refuse to read more than 900MB, can be changed with {@link #setMaxTotalBytesRead(long)}
	 */
	public static final long MAX_TOTAL_BYTES_READ_DEFAULT = 901001001L; // ~ 900MB
	/**
	 * Defensive limit: refuse to load more than 5MB of ancillary metadata, see {@link #setMaxBytesMetadata(long)} and
	 * also {@link #addChunkToSkip(String)}
	 */
	public static final long MAX_BYTES_METADATA_DEFAULT = 5024024; // for ancillary chunks
	/**
	 * Skip ancillary chunks greater than 2MB, see {@link #setSkipChunkMaxSize(long)}
	 */
	public static final long MAX_CHUNK_SIZE_SKIP = 2024024; // chunks exceeding this size will be skipped (nor even CRC checked)

	/**
	 * Basic image info - final and inmutable.
	 */
	public final ImageInfo imgInfo;
	/**
	 * flag: image was in interlaced format
	 */
	public final boolean interlaced;

	protected ChunkSeqReaderPng chunkseq;
	protected BufferedStreamFeeder streamFeeder;

	protected final PngMetadata metadata; // this a wrapper over chunks
	protected int rowNum = -1; // current row number (already read)

	CRC32 idatCrca;//for internal testing
	Adler32 idatCrcb;//for internal testing

	protected IImageLineSet<? extends IImageLine> imlinesSet;
	private IImageLineSetFactory<? extends IImageLine> imageLineSetFactory;

	/**
	 * Construct a PngReader object from a stream, with default options. This reads the signature and the first IHDR
	 * chunk only.
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
	 * Constructs a PngReader opening a file. If it succeeds, it sets <tt>setShouldCloseStream(true)</tt> In case of
	 * exception the file stream is closed
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
			setMaxBytesMetadata(MAX_BYTES_METADATA_DEFAULT);
			setMaxTotalBytesRead(MAX_TOTAL_BYTES_READ_DEFAULT);
			setSkipChunkMaxSize(MAX_CHUNK_SIZE_SKIP);
			this.metadata = new PngMetadata(chunkseq.chunksList);
			// sets a default factory (with ImageLineInt), 
			// this can be overwriten by a extended constructor, or by a setter
			setLineSetFactory(ImageLineSetDefault.getFactoryInt());
			rowNum = -1;
		} catch (RuntimeException e) {
			try {
				if (closeStreamIfError && inputStream != null)
					inputStream.close();
			} catch (IOException e1) {
			}
			if (streamFeeder != null)
				streamFeeder.close();
			if (chunkseq != null)
				chunkseq.close();
			throw e;
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
	protected void readFirstChunks() {
		while (chunkseq.currentChunkGroup < ChunksList.CHUNK_GROUP_4_IDAT)
			streamFeeder.feed(chunkseq);
	}

	/**
	 * Determines which ancillary chunks (metadata) are to be loaded and which skipped.
	 * <p>
	 * Additional restrictions may apply. See also {@link #setChunksToSkip(String...)}, {@link #addChunkToSkip(String)},
	 * {@link #setMaxBytesMetadata(long)}, {@link #setSkipChunkMaxSize(long)}
	 * 
	 * @param chunkLoadBehaviour
	 *            {@link ChunkLoadBehaviour}
	 */
	public void setChunkLoadBehaviour(ChunkLoadBehaviour chunkLoadBehaviour) {
		this.chunkseq.setChunkLoadBehaviour(chunkLoadBehaviour);
	}

	/**
	 * All loaded chunks (metada). If we have not yet end reading the image, this will include only the chunks before
	 * the pixels data (IDAT)
	 * <p>
	 * Critical chunks are included, except that all IDAT chunks appearance are replaced by a single dummy-marker IDAT
	 * chunk. These might be copied to the PngWriter
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

	/**
	 * True if last row has not yet been read
	 */
	public boolean hasMoreRows() {
		return rowNum < imgInfo.rows - 1;
	}

	/**
	 * The row number is mostly meant as a check, the rows must be called in ascending order (not necessarily
	 * consecutive)
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
				chunkseq.getIdatSet().updateCrcs(idatCrca,idatCrcb);
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

	/**
	 * Reads all rows in a ImageLineSet This is handy, but less memory-efficient (except for interlaced)
	 */
	public IImageLineSet<? extends IImageLine> readRows() {
		return readRows(imgInfo.rows, 0, 1);
	}

	/**
	 * Reads a subset of rows.
	 * <p>
	 * This method should called once, and not be mixed with {@link #readRow()}
	 * 
	 * @param nRows
	 *            how many rows to read (default: imageInfo.rows)
	 * @param rowOffset
	 *            rows to skip (default:0)
	 * @param rowStep
	 *            step between rows to load( default:1)
	 */
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
				chunkseq.getIdatSet().updateCrcs(idatCrca,idatCrcb);
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
		chunkseq.getIdatSet().done();
		end();
		return imlinesSet;
	}

	/**
	 * Sets the factory that creates the ImageLine. By default, this implementation uses ImageLineInt but this can be
	 * changed (at construction time or later) by calling this method.
	 * <p>
	 * See also {@link #createLineSet(boolean, int, int, int)}
	 * 
	 * @param factory
	 */
	public void setLineSetFactory(IImageLineSetFactory<? extends IImageLine> factory) {
		imageLineSetFactory = factory;
	}

	/**
	 * By default this uses the factory (which, by default creates ImageLineInt). You should rarely override this.
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
			chunkseq.getIdatSet().updateCrcs(idatCrca,idatCrcb);
			int rowNumreal = idat.rowinfo.rowNreal;
			boolean inset = (rowNumreal - rowOffset) % rowStep == 0;
			if (inset) {
				imlinesSet.getImageLine(rowNumreal).readFromPngRaw(idat.getUnfilteredRow(), idat.rowinfo.buflen,
						idat.rowinfo.oX, idat.rowinfo.dX);
				nread++;
			}
			idat.advanceToNextRow();
		} while (nread < nRows || !idat.isDone());
		idat.done();
		for (int i = 0, j = rowOffset; i < nRows; i++, j += rowStep) {
			imlinesSet.getImageLine(j).endReadFromPngRaw();
		}
	}

	/**
	 * Reads all the (remaining) file, skipping the pixels data. This is much more efficient that calling
	 * {@link #readRow()}, specially for big files (about 10 times faster!), because it doesn't even decompress the IDAT
	 * stream and disables CRC check Use this if you are not interested in reading pixels,only metadata.
	 */
	public void readSkippingAllRows() {
		chunkseq.addChunkToSkip(PngChunkIDAT.ID);
		if (chunkseq.firstChunksNotYetRead())
			readFirstChunks();
		end();
	}

	/**
	 * Set total maximum bytes to read (0: unlimited; default: 200MB). <br>
	 * These are the bytes read (not loaded) in the input stream. If exceeded, an exception will be thrown.
	 */
	public void setMaxTotalBytesRead(long maxTotalBytesToRead) {
		chunkseq.setMaxTotalBytesRead(maxTotalBytesToRead);
	}

	/**
	 * Set total maximum bytes to load from ancillary chunks (0: unlimited; default: 5Mb).<br>
	 * If exceeded, some chunks will be skipped
	 */
	public void setMaxBytesMetadata(long maxBytesMetadata) {
		chunkseq.setMaxBytesMetadata(maxBytesMetadata);
	}

	/**
	 * Set maximum size in bytes for individual ancillary chunks (0: unlimited; default: 2MB). <br>
	 * Chunks exceeding this length will be skipped (the CRC will not be checked) and the chunk will be saved as a
	 * PngChunkSkipped object. See also setSkipChunkIds
	 */
	public void setSkipChunkMaxSize(long skipChunkMaxSize) {
		chunkseq.setSkipChunkMaxSize(skipChunkMaxSize);
	}

	/**
	 * Chunks ids to be skipped. <br>
	 * These chunks will be skipped (the CRC will not be checked) and the chunk will be saved as a PngChunkSkipped
	 * object. See also setSkipChunkMaxSize
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
		if (streamFeeder != null)
			streamFeeder.setCloseStream(shouldCloseStream);
	}

	/**
	 * Reads till end of PNG stream and call <tt>close()</tt>
	 * 
	 * This should normally be called after reading the pixel data, to read the trailing chunks and close the stream.
	 * But it can be called at anytime. This will also read the first chunks if not still read, and skip pixels (IDAT)
	 * if still pending.
	 * 
	 * If you want to read all metadata skipping pixels, readSkippingAllRows() is a little more efficient.
	 * 
	 * If you want to abort immediately, call instead <tt>close()</tt>
	 */
	public void end() {
		try {
			if (chunkseq.firstChunksNotYetRead())
				readFirstChunks();
			if (chunkseq.getIdatSet() != null && !chunkseq.getIdatSet().isDone())
				chunkseq.getIdatSet().done();
			while (!chunkseq.isDone())
				streamFeeder.feed(chunkseq);
		} finally {
			close();
		}
	}

	/**
	 * Releases resources, and closes stream if corresponds. Idempotent, secure, no exceptions.
	 * 
	 * This can be also called for abort. It is recommended to call this in case of exceptions
	 */
	public void close() {
		try {
			if (chunkseq != null)
				chunkseq.close();
		} catch (Exception e) {
			PngHelperInternal.LOGGER.warning("error closing chunk sequence:" + e.getMessage());
		}
		if (streamFeeder != null)
			streamFeeder.close();
	}

	/**
	 * Interlaced PNG is accepted -though not welcomed- now...
	 */
	public boolean isInterlaced() {
		return interlaced;
	}

	/**
	 * Disables the CRC integrity check in IDAT chunks and ancillary chunks, this gives a slight increase in reading
	 * speed for big files
	 */
	public void setCrcCheckDisabled() {
		chunkseq.setCheckCrc(false);
	}

	/**
	 * Gets wrapped {@link ChunkSeqReaderPng} object
	 */
	public ChunkSeqReaderPng getChunkseq() {
		return chunkseq;
	}

	public void prepareSimpleDigestComputation() {
		if(idatCrca==null) idatCrca=new CRC32(); else idatCrca.reset();
		if(idatCrcb==null) idatCrcb=new Adler32(); else idatCrcb.reset();
		idatCrca.update((byte)imgInfo.rows);
		idatCrca.update((byte)(imgInfo.rows>>8));
		idatCrca.update((byte)(imgInfo.rows>>16));
		idatCrca.update((byte)imgInfo.cols);
		idatCrca.update((byte)(imgInfo.cols>>8));
		idatCrca.update((byte)(imgInfo.cols>>16));
		idatCrca.update((byte)(imgInfo.channels));
		idatCrca.update((byte)(imgInfo.bitDepth));
		idatCrca.update((byte)((imgInfo.indexed?10:20)));
		idatCrcb.update((byte)((imgInfo.bytesPerRow)));
		idatCrcb.update((byte)((imgInfo.channels)));
		idatCrcb.update((byte)((imgInfo.rows)));// whatever
	}
	
	long getSimpleDigest() {
		if(idatCrca==null) return 0;
		else return (idatCrca.getValue() ^ (idatCrcb.getValue()<<31));
	}
	
	public String getSimpleDigestHex() {
		String s = Long.toHexString(getSimpleDigest()).toUpperCase();
		if(s.length()<16) {
			s = "0000000000000000" + s;
			return s.substring(s.length()-16);
		} else return s;
	}
	
	/**
	 * Basic info, for debugging.
	 */
	public String toString() { // basic info
		return imgInfo.toString() + " interlaced=" + interlaced;
	}

	/**
	 * Basic info, in a compact format, apt for scripting
	 * COLSxROWS[dBITDEPTH][a][p][g][i] ( the default dBITDEPTH='d8' is ommited) 
	 * @return
	 */
	public String toStringCompact() { 
		return imgInfo.toStringBrief() + (interlaced? "i":"");
	}

	 
	
}
