package ar.com.hjg.pngj;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.ChunkPredicate;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.ChunksListForWrite;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkIEND;
import ar.com.hjg.pngj.chunks.PngChunkIHDR;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngMetadata;

/**
 * Writes a PNG image
 */
public class PngWriter {

	public final ImageInfo imgInfo;

	/**
	 * last writen row number, starting from 0
	 */
	protected int rowNum = -1;

	private final ChunksListForWrite chunksList;

	private final PngMetadata metadata; // high level wrapper over chunkList

	/**
	 * Current chunk grounp, (0-6) already read or reading
	 * <p>
	 * see {@link ChunksList}
	 */
	protected int currentChunkGroup = -1;

	/**
	 * PNG filter strategy
	 */
	protected IFilterWriteStrategy filterStrat;

	/**
	 * If the ImageLine has a valid filterType, and the image is not interlaced,
	 * that file type will be used
	 */
	protected boolean filterPreserve = false;

	/**
	 * zip compression level 0 - 9
	 */
	private int compLevel = 6;
	private boolean shouldCloseStream = true; // true: closes stream after ending write

	private PngIDatChunkOutputStream datStream;

	private DeflaterOutputStream datStreamDeflated;

	/**
	 * Deflate algortithm compression strategy
	 */
	private int deflaterStrategy = Deflater.FILTERED;

	private int idatMaxSize = 0; // 0=use default (PngIDatChunkOutputStream 64k)

	private final OutputStream os;

	protected byte[] rowb = null; // element 0 is filter type!
	protected byte[] rowbfilter = null; // current line with filter

	protected byte[] rowbprev = null; // rowb prev

	private ChunkPredicate copyFromPredicate = null;
	private ChunksList copyFromList = null;

	/**
	 * Same as PngWriter(File file, ImageInfo imgInfo, boolean allowoverwrite)
	 * 
	 * @param file
	 * @param imgInfo
	 */
	public PngWriter(File file, ImageInfo imgInfo, boolean allowoverwrite) {
		this(PngHelperInternal.ostreamFromFile(file, allowoverwrite), imgInfo);
		setShouldCloseStream(true);
	}

	/**
	 * Same as PngWriter(File file, ImageInfo imgInfo, boolean allowoverwrite)
	 * 
	 * @param file
	 * @param imgInfo
	 */
	public PngWriter(File file, ImageInfo imgInfo) {
		this(file, imgInfo, true);
	}

	/**
	 * Constructs a new PngWriter from a output stream. After construction
	 * nothing is writen yet. You still can set some parameters (compression,
	 * filters) and queue chunks before start writing the pixels.
	 * <p>
	 * See also <code>FileHelper.createPngWriter()</code> if available.
	 * 
	 * @param outputStream
	 *            Opened stream for binary writing
	 * @param imgInfo
	 *            Basic image parameters
	 * @param filenameOrDescription
	 *            Optional, just for error/debug messages
	 */
	public PngWriter(OutputStream outputStream, ImageInfo imgInfo) {
		this.os = outputStream;
		this.imgInfo = imgInfo;
		// prealloc
		rowb = new byte[imgInfo.bytesPerRow + 1];
		rowbprev = new byte[rowb.length];
		rowbfilter = new byte[rowb.length];
		chunksList = new ChunksListForWrite(imgInfo);
		metadata = new PngMetadata(chunksList);
		filterStrat = new FilterWriteStrategy(imgInfo, FilterType.FILTER_DEFAULT); // can be changed
	}

	private void initIdat() { // this triggers the writing of first chunks
		datStream = new PngIDatChunkOutputStream(this.os, idatMaxSize);
		Deflater def = new Deflater(compLevel);
		def.setStrategy(deflaterStrategy);
		datStreamDeflated = new DeflaterOutputStream(datStream, def);
		writeSignatureAndIHDR();
		writeFirstChunks();
	}

	private void writeEndChunk() {
		PngChunkIEND c = new PngChunkIEND(imgInfo);
		c.createRawChunk().writeChunk(os);
		chunksList.getChunks().add(c);
	}

	private void writeFirstChunks() {
		int nw = 0;
		currentChunkGroup = ChunksList.CHUNK_GROUP_1_AFTERIDHR;
		queueChunksFromOther();
		nw = chunksList.writeChunks(os, currentChunkGroup);
		currentChunkGroup = ChunksList.CHUNK_GROUP_2_PLTE;
		nw = chunksList.writeChunks(os, currentChunkGroup);
		if (nw > 0 && imgInfo.greyscale)
			throw new PngjOutputException("cannot write palette for this format");
		if (nw == 0 && imgInfo.indexed)
			throw new PngjOutputException("missing palette");
		currentChunkGroup = ChunksList.CHUNK_GROUP_3_AFTERPLTE;
		nw = chunksList.writeChunks(os, currentChunkGroup);
		currentChunkGroup = ChunksList.CHUNK_GROUP_4_IDAT;
	}

	private void writeLastChunks() { // not including end
		queueChunksFromOther();
		currentChunkGroup = ChunksList.CHUNK_GROUP_5_AFTERIDAT;
		chunksList.writeChunks(os, currentChunkGroup);
		// should not be unwriten chunks
		List<PngChunk> pending = chunksList.getQueuedChunks();
		if (!pending.isEmpty())
			throw new PngjOutputException(pending.size() + " chunks were not written! Eg: " + pending.get(0).toString());
		currentChunkGroup = ChunksList.CHUNK_GROUP_6_END;
	}

	/**
	 * Write id signature and also "IHDR" chunk
	 */
	private void writeSignatureAndIHDR() {
		currentChunkGroup = ChunksList.CHUNK_GROUP_0_IDHR;

		PngHelperInternal.writeBytes(os, PngHelperInternal.getPngIdSignature()); // signature
		PngChunkIHDR ihdr = new PngChunkIHDR(imgInfo);
		// http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
		ihdr.setCols(imgInfo.cols);
		ihdr.setRows(imgInfo.rows);
		ihdr.setBitspc(imgInfo.bitDepth);
		int colormodel = 0;
		if (imgInfo.alpha)
			colormodel += 0x04;
		if (imgInfo.indexed)
			colormodel += 0x01;
		if (!imgInfo.greyscale)
			colormodel += 0x02;
		ihdr.setColormodel(colormodel);
		ihdr.setCompmeth(0); // compression method 0=deflate
		ihdr.setFilmeth(0); // filter method (0)
		ihdr.setInterlaced(0); // we never interlace
		ihdr.createRawChunk().writeChunk(os);
		chunksList.getChunks().add(ihdr);
	}

	private void filterRow() {
		FilterType filterType = FilterType.FILTER_UNKNOWN;
		if (filterPreserve && FilterType.isValidStandard(rowb[0])) {
			filterType = FilterType.getByVal(rowb[0]); // preserve original filter
		} else {
			for (FilterType ftype : filterStrat.shouldTest(rowNum)) {
				filterRowWithFilterType(ftype);
				filterStrat.reportResultsForFilter(rowNum, ftype, rowbfilter, true);
			}
			filterType = filterStrat.preferedType(rowNum);
		}
		filterRowWithFilterType(filterType);
		filterStrat.reportResultsForFilter(rowNum, filterType, rowbfilter, false);
	}

	private void filterRowWithFilterType(FilterType filterType) {
		// warning: filters operation rely on: "previous row" (rowbprev) is
		// initialized to 0 the first time
		rowbfilter[0] = (byte) filterType.val;
		switch (filterType) {
		case FILTER_NONE:
			filterRowNone();
			break;
		case FILTER_SUB:
			filterRowSub();
			break;
		case FILTER_UP:
			filterRowUp();
			break;
		case FILTER_AVERAGE:
			filterRowAverage();
			break;
		case FILTER_PAETH:
			filterRowPaeth();
			break;
		default:
			throw new PngjUnsupportedException("Filter type " + filterType + " not recognized");
		}
	}

	private void filterAndSend() {
		filterRow();
		try {
			datStreamDeflated.write(rowbfilter, 0, imgInfo.bytesPerRow + 1);
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	protected void filterRowAverage() {
		int i, j, imax;
		imax = imgInfo.bytesPerRow;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imax; i++, j++) {
			rowbfilter[i] = (byte) (rowb[i] - ((rowbprev[i] & 0xFF) + (j > 0 ? (rowb[j] & 0xFF) : 0)) / 2);
		}
	}

	protected void filterRowNone() {
		System.arraycopy(rowb, 1, rowbfilter, 1, imgInfo.bytesPerRow);
	}

	protected void filterRowPaeth() {
		int i, j, imax;
		imax = imgInfo.bytesPerRow;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imax; i++, j++) {
			rowbfilter[i] = (byte) PngHelperInternal.filterRowPaeth(rowb[i], j > 0 ? (rowb[j] & 0xFF) : 0,
					rowbprev[i] & 0xFF, j > 0 ? (rowbprev[j] & 0xFF) : 0);
		}
	}

	protected void filterRowSub() {
		int i, j;
		for (i = 1; i <= imgInfo.bytesPixel; i++)
			rowbfilter[i] = (byte) rowb[i];
		for (j = 1, i = imgInfo.bytesPixel + 1; i <= imgInfo.bytesPerRow; i++, j++) {
			rowbfilter[i] = (byte) (rowb[i] - rowb[j]);
		}
	}

	protected void filterRowUp() {
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			rowbfilter[i] = (byte) (rowb[i] - rowbprev[i]);
		}
	}

	protected int sumRowbfilter() { // sums absolute value
		int s = 0;
		for (int i = 1; i <= imgInfo.bytesPerRow; i++)
			if (rowbfilter[i] < 0)
				s -= (int) rowbfilter[i];
			else
				s += (int) rowbfilter[i];
		return s;
	}

	protected void queueChunksFromOther() {
		if (copyFromList == null || copyFromPredicate == null)
			return;
		boolean idatDone = currentChunkGroup >= ChunksList.CHUNK_GROUP_4_IDAT;
		for (PngChunk chunk : copyFromList.getChunks()) {
			if (chunk.getRaw().data == null)
				continue; // we cannot copy skipped chunks?
			int group = chunk.getChunkGroup();
			if (group <= ChunksList.CHUNK_GROUP_4_IDAT && idatDone)
				continue;
			if (group >= ChunksList.CHUNK_GROUP_4_IDAT && !idatDone)
				continue;
			if (chunk.crit && !chunk.id.equals(PngChunkPLTE.ID))
				continue; // critical chunks (except perhaps PLTE) are never copied
			boolean copy = copyFromPredicate.match(chunk);
			if (copy) {
				// but if the chunk is already queued or writen, it's ommited!
				if (chunksList.getEquivalent(chunk).isEmpty() && chunksList.getQueuedEquivalent(chunk).isEmpty()) {
					chunksList.queue(chunk);
					//PngChunk newchunk = ChunkHelper.cloneForWrite(chunk, imgInfo);
					//chunksList.queue(newchunk);
				}
			}
		}
	}

	public void queueChunk(PngChunk chunk) {
		for (PngChunk other : chunksList.getQueuedEquivalent(chunk)) {
			getChunksList().removeChunk(other);
		}
		chunksList.queue(chunk);
	}

	/**
	 * Sets an origin (typically from a PngReader) of Chunks to be copied. This
	 * should be called only once, before starting writing the rows. It doesn't
	 * matter the current state of the PngReader reading, this is a live object
	 * and what matters is that when the writer writes the pixels (IDAT) the
	 * reader has already read them, and that when the writer ends, the reader
	 * is already ended (all this is very natural).
	 * 
	 * Apart from the copyMask, there is some addional heuristics:
	 * 
	 * - The chunks will be queued, but will be written as late as possible
	 * (unless you explicitly set priority=true)
	 * 
	 * - The chunk will not be queued if an "equivalent" chunk was already
	 * queued explicitly. And it will be overwriten another is queued
	 * explicitly.
	 * 
	 * @param chunks
	 * @param copyMask
	 */
	public void copyChunksFrom(ChunksList chunks, int copyMask) {
		copyChunksFrom(chunks, ChunkCopyBehaviour.getPredicate(copyMask, imgInfo));
	}

	public void copyChunksFrom(ChunksList chunks) {
		copyChunksFrom(chunks, ChunkCopyBehaviour.COPY_ALL);
	}

	/**
	 * The chunk (ancillary or PLTE) will be copy if and only if predicate
	 * matches
	 * 
	 * @param chunks
	 * @param predicate
	 */
	public void copyChunksFrom(ChunksList chunks, ChunkPredicate predicate) {
		if (copyFromList != null && chunks != null)
			PngHelperInternal.LOGGER.warning("copyChunksFrom should only be called once");
		if (predicate == null)
			throw new PngjOutputException("copyChunksFrom requires a predicate");
		this.copyFromList = chunks;
		this.copyFromPredicate = predicate;
	}

	/**
	 * Computes compressed size/raw size, approximate.
	 * <p>
	 * Actually: compressed size = total size of IDAT data , raw size =
	 * uncompressed pixel bytes = rows * (bytesPerRow + 1).
	 * 
	 * This must be called after pngw.end()
	 */
	public double computeCompressionRatio() {
		if (currentChunkGroup < ChunksList.CHUNK_GROUP_6_END)
			throw new PngjOutputException("must be called after end()");
		double compressed = (double) datStream.getCountFlushed();
		double raw = (imgInfo.bytesPerRow + 1) * imgInfo.rows;
		return compressed / raw;
	}

	/**
	 * Finalizes the image creation and closes the stream. This MUST be called
	 * after writing the lines.
	 */
	public void end() {
		if (rowNum != imgInfo.rows - 1)
			throw new PngjOutputException("all rows have not been written");
		try {
			datStreamDeflated.finish(); // this should release deflater internal native resources
			datStream.flush();
			writeLastChunks();
			writeEndChunk();
		} catch (IOException e) {
			throw new PngjOutputException(e);
		} finally {
			close();
		}
	}

	/** releases resources (stream) */
	public void close() {
		if (shouldCloseStream)
			try {
				os.close();
			} catch (Exception e) {
				PngHelperInternal.LOGGER.warning("Error closing writer " + e.toString());
			}
		datStreamDeflated = null;
		datStream = null;
	}

	/**
	 * returns the chunks list (queued and writen chunks)
	 */
	public ChunksListForWrite getChunksList() {
		return chunksList;
	}

	/**
	 * High level wrapper over chunksList for metadata handling
	 */
	public PngMetadata getMetadata() {
		return metadata;
	}

	/**
	 * Sets compression level of ZIP algorithm.
	 * <p>
	 * This must be called just after constructor, before starting writing.
	 * <p>
	 * See also setFilterType()
	 * 
	 * @param compLevel
	 *            between 0 and 9 (default:6 , recommended: 6 or more)
	 */
	public void setCompLevel(int compLevel) {
		if (compLevel < 0 || compLevel > 9)
			throw new PngjOutputException("Compression level invalid (" + compLevel + ") Must be 0..9");
		this.compLevel = compLevel;
	}

	/**
	 * Sets internal prediction filter type, or strategy to choose it.
	 * <p>
	 * This must be called just after constructor, before starting writing.
	 * <p>
	 * See also setFilterStrategy setCompLevel()
	 * 
	 * @param filterType
	 *            One of the five prediction types or strategy to choose it (see
	 *            <code>PngFilterType</code>) Recommended values: DEFAULT
	 *            (default) or AGGRESIVE
	 */
	public void setFilterType(FilterType filterType) {
		filterStrat = new FilterWriteStrategy(imgInfo, filterType);
	}

	public void setFilterStrategy(IFilterWriteStrategy filterS) {
		filterStrat = filterS;
	}

	public boolean isFilterPreserve() {
		return filterPreserve;
	}

	public void setFilterPreserve(boolean filterPreserve) {
		this.filterPreserve = filterPreserve;
	}

	/**
	 * Sets maximum size of IDAT fragments. This has little effect on
	 * performance you should rarely call this
	 * <p>
	 * 
	 * @param idatMaxSize
	 *            default=0 : use defaultSize (64K)
	 */
	public void setIdatMaxSize(int idatMaxSize) {
		this.idatMaxSize = idatMaxSize;
	}

	/**
	 * if true, input stream will be closed after ending write
	 * <p>
	 * default=true
	 */
	public void setShouldCloseStream(boolean shouldCloseStream) {
		this.shouldCloseStream = shouldCloseStream;
	}

	/**
	 * Deflater strategy: one of Deflater.FILTERED Deflater.HUFFMAN_ONLY
	 * Deflater.DEFAULT_STRATEGY
	 * <p>
	 * Default: Deflater.FILTERED . This should be changed very rarely.
	 */
	public void setDeflaterStrategy(int deflaterStrategy) {
		this.deflaterStrategy = deflaterStrategy;
	}

	public void writeRow(IImageLine imgline) {
		writeRow(imgline, rowNum + 1);
	}

	public void writeRows(IImageLineSet<? extends IImageLine> imglines) {
		for (int i = 0; i < imgInfo.rows; i++)
			writeRow(imglines.getImageLine(i));
	}

	public void writeRow(IImageLine imgline, int rownumber) {
		rowNum++;
		if (rownumber >= 0 && rowNum != rownumber)
			throw new PngjOutputException("rows must be written in order: expected:" + rowNum + " passed:" + rownumber);
		if (datStream == null)
			initIdat();
		// swap
		byte[] tmp = rowb;
		rowb = rowbprev;
		rowbprev = tmp;
		rowb[0] = (byte) FilterType.FILTER_UNKNOWN.val; // writeToPngRaw can overwrite this	
		imgline.writeToPngRaw(rowb);
		filterAndSend();
	}

	public void writeRowInt(int[] buf) {
		writeRow(new ImageLineInt(imgInfo, buf));
	}

}