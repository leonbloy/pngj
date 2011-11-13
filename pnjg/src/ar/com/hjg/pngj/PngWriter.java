package ar.com.hjg.pngj;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunksToWrite;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkIEND;
import ar.com.hjg.pngj.chunks.PngChunkIHDR;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;

/**
 * Writes a PNG image, line by line.
 */
public class PngWriter {
	public final ImageInfo imgInfo;
	protected int compLevel = 6; // zip compression level 0 - 9
	protected FilterWriteStrategy filterStrat;
	protected int rowNum = -1; // current line number
	// current line, one (packed) sample per element (layout differnt from rowb!)
	protected int[] scanline = null;
	protected byte[] rowb = null; // element 0 is filter type!
	protected byte[] rowbprev = null; // rowb prev
	protected byte[] rowbfilter = null; // current line with filter  
	protected final OutputStream os;
	protected final String filename; // optional
	private PngIDatChunkOutputStream datStream;
	private DeflaterOutputStream datStreamDeflated;
	public ChunksToWrite chunks;

	private enum WriteStep {
		START, HEADER, HEADER_DONE, IDHR, IDHR_DONE, FIRST_CHUNKS, FIRST_CHUNKS_DONE, IDAT, IDAT_DONE, LAST_CHUNKS, LAST_CHUNKS_DONE, END;
	}

	private WriteStep step;

	public PngWriter(OutputStream outputStream, ImageInfo imgInfo) {
		this(outputStream, imgInfo, "[NO FILENAME AVAILABLE]");
	}

/**
 * Constructs a new PngWriter from a output stream.
 *<p>
 * See also <code>FileHelper.createPngWriter()</code> if available.
 * 
 * @param outputStream Opened stream for binary writing
 * @param imgInfo Basic image parameters
 * @param filenameOrDescription Optional, just for error/debug messages
 */
	public PngWriter(OutputStream outputStream, ImageInfo imgInfo,
			String filenameOrDescription) {
		this.filename = filenameOrDescription == null ? "" : filenameOrDescription;
		this.os = outputStream;
		this.imgInfo = imgInfo;
		// prealloc
		scanline = new int[imgInfo.samplesPerRowP];
		rowb = new byte[imgInfo.bytesPerRow + 1];
		rowbprev = new byte[rowb.length];
		rowbfilter = new byte[rowb.length];
		datStream = new PngIDatChunkOutputStream(this.os);
		chunks = new ChunksToWrite(imgInfo);
		step = WriteStep.START;
		filterStrat = new FilterWriteStrategy(imgInfo, PngFilterType.FILTER_DEFAULT);
	}

	/**
	 * Write id signature and also "IHDR" chunk
	 */
	private void writeSignatureAndIHDR() {
		if (datStreamDeflated == null)
			datStreamDeflated = new DeflaterOutputStream(datStream, new Deflater(compLevel),8192);
		PngHelper.writeBytes(os, PngHelper.pngIdBytes); // signature
		step = WriteStep.IDHR;
		PngChunkIHDR ihdr = new PngChunkIHDR(imgInfo);
		// http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
		ihdr.cols = imgInfo.cols;
		ihdr.rows = imgInfo.rows;
		ihdr.bitspc = imgInfo.bitDepth;
		int colormodel = 0;
		if (imgInfo.alpha)
			colormodel += 0x04;
		if (imgInfo.indexed)
			colormodel += 0x01;
		if (!imgInfo.greyscale)
			colormodel += 0x02;
		ihdr.colormodel = colormodel;
		ihdr.compmeth = 0; // compression method 0=deflate
		ihdr.filmeth = 0; // filter method (0)
		ihdr.interlaced = 0; // never interlace
		ihdr.createChunk().writeChunk(os);
		step = WriteStep.IDHR_DONE;
	}

	private void writeFirstChunks() {
		step = WriteStep.FIRST_CHUNKS;
		PngChunkPLTE paletteChunk = null;
		// first pass: before palette (and saves palette chunk if found)
		for (PngChunk chunk : chunks.getPending()) {
			if (chunk.beforePLTE)
				chunk.writeAndMarkAsWrite(os);
			if (chunk instanceof PngChunkPLTE)
				paletteChunk = (PngChunkPLTE) chunk;
		}
		// writes palette?
		if (paletteChunk != null) {
			if (imgInfo.greyscale)
				throw new PngjOutputException("cannot write palette for this format");
			paletteChunk.writeAndMarkAsWrite(os);
		} else { // no palette
			if (imgInfo.indexed)
				throw new PngjOutputException("missing palette");
		}
		// second pass: after palette
		for (PngChunk chunk : chunks.getPending()) {
			boolean prio = chunk.getWriteStatus() == 1;
			if (chunk.beforeIDAT || prio)
				chunk.writeAndMarkAsWrite(os);
		}
		step = WriteStep.FIRST_CHUNKS_DONE;
	}

	private void writeLastChunks() { // not including end
		step = WriteStep.LAST_CHUNKS;
		for (PngChunk chunk : chunks.getPending()) {
			if (chunk.beforePLTE || chunk.beforeIDAT)
				throw new PngjOutputException("too late to write this chunk: " + chunk.id);
			chunk.writeAndMarkAsWrite(os);
		}
		step = WriteStep.LAST_CHUNKS_DONE;
	}

	private void writeEndChunk() {
		PngChunkIEND c = new PngChunkIEND(imgInfo);
		c.createChunk().writeChunk(os);
		step = WriteStep.END;
	}

	private void writeDataBeforeIDAT() {
		// notice that this if() are not exclusive
		if (step == WriteStep.START)
			writeSignatureAndIHDR(); // now we are in IDHR_DONE
		if (step == WriteStep.IDHR_DONE)
			writeFirstChunks(); // now we are in FIRST_CHUNKS_DONE
		if (step != WriteStep.FIRST_CHUNKS_DONE) // check
			throw new PngjOutputException("unexpected state before idat write: " + step);
	}

	/**
	 * Writes a full image row. This must be called sequentially from n=0 to
	 * n=rows-1 One integer per sample , in the natural order: R G B R G B ... (or
	 * R G B A R G B A... if has alpha) The values should be between 0 and 255 for
	 * 8 bitspc images, and between 0- 65535 form 16 bitspc images (this applies
	 * also to the alpha channel if present) The array can be reused.
	 * 
	 * @param newrow
	 *          Array of pixel values
	 * @param n
	 *          Number of row, from 0 (top) to rows-1 (bottom)
	 */
	public void writeRow(int[] newrow, int n) {
		if (step != WriteStep.IDAT) {
			writeDataBeforeIDAT();
			step = WriteStep.IDAT;
		}
		if (n < 0 || n > imgInfo.rows)
			throw new RuntimeException("invalid value for row " + n);
		rowNum++;
		if (rowNum != n)
			throw new RuntimeException("write order must be strict for rows " + n
					+ " (expected=" + rowNum + ")");
		scanline = newrow;
		// swap
		byte[] tmp = rowb;
		rowb = rowbprev;
		rowbprev = tmp;
		convertRowToBytes();
		filterRow(n);
		try {
			datStreamDeflated.write(rowbfilter, 0, imgInfo.bytesPerRow + 1);
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	/**
	 * this uses the row number from the imageline!
	 */
	public void writeRow(ImageLine imgline) {
		writeRow(imgline.scanline, imgline.getRown());
	}

	/**
	 * Finalizes the image creation and closes the stream. 
	 * This MUST be called after writing the lines.
	 */
	public void end() {
		if (rowNum != imgInfo.rows - 1)
			throw new PngjOutputException("all rows have not been written");
		try {
			datStreamDeflated.finish();
			datStream.flush();
			step = WriteStep.IDAT_DONE;
			writeLastChunks();
			writeEndChunk();
			os.close();
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	private void filterRow(int rown) {
		// warning: filters operation rely on: "previos row" (rowbprev) is
		// initialized to 0 the first time
		if (filterStrat.shouldTestAll(rown)) {
			filterRowNone();
			filterStrat.fillResultsForFilter(rown, PngFilterType.FILTER_NONE, sumRowbfilter());
			filterRowSub();
			filterStrat.fillResultsForFilter(rown, PngFilterType.FILTER_SUB, sumRowbfilter());
			filterRowUp();
			filterStrat.fillResultsForFilter(rown, PngFilterType.FILTER_UP, sumRowbfilter());
			filterRowAverage();
			filterStrat.fillResultsForFilter(rown, PngFilterType.FILTER_AVERAGE,
					sumRowbfilter());
			filterRowPaeth();
			filterStrat.fillResultsForFilter(rown, PngFilterType.FILTER_PAETH, sumRowbfilter());
		}
		PngFilterType filterType = filterStrat.gimmeFilterType(rown);
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
			throw new PngjOutputException("Filter type " + filterType + " not implemented");
		}
	}

	private int sumRowbfilter() { // sums absolute value 
		int s = 0;
		for (int i = 1; i <= imgInfo.bytesPerRow; i++)
			if (rowbfilter[i] < 0)
				s -= (int ) rowbfilter[i];
			else
				s += (int ) rowbfilter[i];
		return s;
	}

	protected void filterRowNone() {
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			rowbfilter[i] = (byte) rowb[i];
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

	protected void filterRowAverage() {
		int i, j;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imgInfo.bytesPerRow; i++, j++) {
			rowbfilter[i] = (byte) (rowb[i] - ((rowbprev[i]&0xFF) + (j > 0 ? (rowb[j]&0xFF) : 0)) / 2);
		}
	}

	protected void filterRowPaeth() {
		int i, j;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imgInfo.bytesPerRow; i++, j++) {
			rowbfilter[i] = (byte) (rowb[i] - PngFilterType.filterPaethPredictor(j > 0 ? (rowb[j]&0xFF) : 0, rowbprev[i]&0xFF, j > 0 ? (rowbprev[j] &0xFF): 0));
		}
	}

	protected void convertRowToBytes() {
		// http://www.libpng.org/pub/png/spec/1.2/PNG-DataRep.html
		int i, j;
		if (imgInfo.bitDepth <= 8) {
			for (i = 0, j = 1; i < imgInfo.samplesPerRowP; i++) {
				rowb[j++] = (byte)(scanline[i]);
			}
		} else { // 16 bitspc
			for (i = 0, j = 1; i < imgInfo.samplesPerRowP; i++) {
				//x = (int) (scanline[i]) & 0xFFFF;
				rowb[j++] = (byte)(scanline[i] >> 8);
				rowb[j++] = (byte)(scanline[i] );
			}
		}
	}

	// /// several getters / setters - all this setters are optional
	/**
	 * Set physical resolution, in DPI (dots per inch).
	 * This goes inside a chunk, should be set before writing lines.
	 */
	public void setDpi(double dpi) {
		chunks.setPHYSdpi(dpi);
	}

	/**
	 * Sets internal prediction filter type, or strategy to choose it.
	 * <p>
	 * This must be called just after constructor, before starting writing.
	 * <p>
	 * See also setCompLevel()
	 * 
	 * @param filterType One of the five prediction types or strategy to choose it
	 * (see <code>PngFilterType</code>) Recommended values: DEFAULT (default) or AGGRESIVE 
	 */
	public void setFilterType(PngFilterType filterType) {
		filterStrat = new FilterWriteStrategy(imgInfo, filterType);
	}

	/**
	 * Sets compression level of ZIP algorithm.
	 * <p>
	 * This must be called just after constructor, before starting writing.
	 * <p>
	 * See also setFilterType()
	 * 
	 * @param compLevel between 0 and 9 (default:6 , recommended: 6 or more) 
	 */
	public void setCompLevel(int compLevel) {
		if (compLevel < 0 || compLevel > 9)
			throw new PngjException("Compression level invalid (" + compLevel
					+ ") Must be 0..9");
		this.compLevel = compLevel;
	}

	/**
	 * Filename or description, from the optional constructor argument.
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * copy chunks from reader - copy_mask : see ChunksToWrite.COPY_XXX
	 * 
	 * If we are after idat, only considers those chunks after IDAT in PngReader
	 * TODO: this should be more customizable
	 */
	private void copyChunks(PngReader reader, int copy_mask, boolean onlyAfterIdat) {
		boolean idatDone = step.compareTo(WriteStep.IDAT) >= 0;
		int posidat = reader.chunks.positionIDAT();
		if (onlyAfterIdat && posidat < 0)
			return; // nothing to do
		List<PngChunk> chunksR = reader.chunks.getChunks();
		for (int i = 0; i < chunksR.size(); i++) {
			if (i < posidat && onlyAfterIdat)
				continue;
			boolean copy = false;
			PngChunk chunk = chunksR.get(i);
			if (chunk.crit) {
				if (chunk.id.equals(ChunkHelper.PLTE_TEXT)) {
					if (imgInfo.indexed
							&& ChunksToWrite.maskMatch(copy_mask, ChunksToWrite.COPY_PALETTE))
						copy = true;
					if (!imgInfo.greyscale
							&& ChunksToWrite.maskMatch(copy_mask, ChunksToWrite.COPY_ALL))
						copy = true;
				}
			} else { // ancillary
				boolean text = (chunk instanceof PngChunkTextVar);
				boolean safe = chunk.safe;
				if (ChunksToWrite.maskMatch(copy_mask, ChunksToWrite.COPY_ALL))
					copy = true;
				if (safe && ChunksToWrite.maskMatch(copy_mask, ChunksToWrite.COPY_ALL_SAFE))
					copy = true;
				if (chunk.id.equals(ChunkHelper.tRNS_TEXT)
						&& ChunksToWrite.maskMatch(copy_mask, ChunksToWrite.COPY_TRANSPARENCY))
					copy = true;
				if (chunk.id.equals(ChunkHelper.pHYs_TEXT)
						&& ChunksToWrite.maskMatch(copy_mask, ChunksToWrite.COPY_PHYS))
					copy = true;
				if (text && ChunksToWrite.maskMatch(copy_mask, ChunksToWrite.COPY_TEXTUAL))
					copy = true;
			}
			if (copy) {
				if (chunk.beforeIDAT && idatDone) {
					System.err.println("too late to add pre-idat chunk - " + chunk);
					continue;
				}
				chunks.cloneAndAdd(chunk, false);
			}
		}
	}

	/** 
	 * Copies first (pre IDAT) ancillary chunks from a PngReader.
	 * <p>
	 * Should be called when creating an image from another,
	 * before starting writing lines, to copy relevant chunks.
	 * <p>
	 * @param reader  : PngReader object, already opened.
	 * @param copy_mask : Mask bit (OR), see <code>ChunksToWrite.COPY_XXX</code> constants
	 */
	public void copyChunksFirst(PngReader reader, int copy_mask) {
		copyChunks(reader, copy_mask, false);
	}

	/** 
	 * Copies last (post IDAT) ancillary chunks from a PngReader.
	 * <p>
	 * Should be called when creating an image from another,
	 * after writing all lines,  before closing the writer, to copy additional chunks.
	 * <p>
	 * @param reader  : PngReader object, already opened and fully read.
	 * @param copy_mask : Mask bit (OR), see <code>ChunksToWrite.COPY_XXX</code> constants
	 */
	public void copyChunksLast(PngReader reader, int copy_mask) {
		copyChunks(reader, copy_mask, true);
	}
	
}
