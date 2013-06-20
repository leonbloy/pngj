package ar.com.hjg.pngj;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/* Use exampe: 
 * a. sync mode
 *    for(int row=0;row<nrows;row++) { // in interlaced mode, nrows should be computed differently
 *         whilte(! isRowDone()) { // feed bytes till a new row is available
 *         		bytes[] b =getBytesFromIs(); <- can block.
 *         		readerChunk -> feed with b -> calls this .processBytes()	 
 *         }
 *         n = getRowFilled();
 *         inflatedRow = getRow();
 *         // do whatever with 
 *         setNextRowSize(bytesperrow+1);
 *    }
 *    // assert isAllDone()
 * 
 *   
 *         
 */

/**
 * A set of IDAT-like chunks that, concatenated, form a zlib stream , which is
 * inside a sequence of "rows"
 * 
 */
public class ChunkReaderDeflatedSet {

	protected final Inflater inf;

	protected byte[] row; // a "row" here means a image (or subimage for interlaced) row plus a filter byte
	protected int rowlen; // what amount of bytes is to be interpreted as a "row". this should equals row.length ; can change (for interlaced)
	int rowfilled; //  should grow until rowlen
	protected int rown; // only coincide with image row if non-interlaced  - incremented by setNextRowSize()
	boolean allDone = false; // done: means that we have already read what we want (Inflater should normally agree)

	private ChunkReaderDeflated curChunk;
	protected boolean dataPendingForConsumer = false;

	public ChunkReaderDeflatedSet(int initialRowLen, int maxRowLen) {
		this.rowlen = initialRowLen;
		this.inf = new Inflater();
		if (initialRowLen < 1)
			throw new PngjException("bad inital row len " + initialRowLen);
		this.row = new byte[maxRowLen];
		rown = -1;
		setNextRowLen(initialRowLen);
	}

	public void newChunk(ChunkReaderDeflated cr) {
		// all chunks must have same id
		if (this.curChunk != null && !this.curChunk.getChunkRaw().id.equals(cr.getChunkRaw().id))
			throw new PngjInputException("Bad chunk inside IdatSet, id:" + cr.getChunkRaw().id + ", expected:"
					+ this.curChunk.getChunkRaw().id);
		this.curChunk = cr;
	}

	/**
	 * Feeds the inflater with the compressed bytes
	 * 
	 * The caller should not call repeatedly this, without calling in between
	 * 
	 * @param buf
	 * @param off
	 * @param len
	 */
	protected void processBytes(byte[] buf, int off, int len) {
		if (rowlen <= 0)
			throw new PngjException("ChunkProcessorIdat error: rowsize not set?");
		if (!allDone) {
			if (len > 0) {// First, feed the inflater
				if (inf.needsDictionary() || !inf.needsInput())
					throw new RuntimeException("cannot feed inflater");
				inf.setInput(buf, off, len);
			}
			inflateData();
		}
		if (allDone)
			inf.end();
	}

	/* returns true if it could descompress unless one byte */
	private boolean inflateData() {
		try {
			if (dataPendingForConsumer)
				throw new PngjException("This should not happen");
			int ninflated = 0;
			if (row == null || row.length < rowlen)
				row = new byte[rowlen];
			if (rowfilled < rowlen && !allDone) {
				ninflated = inf.inflate(row, rowfilled, rowlen - rowfilled);
				rowfilled += ninflated;
			}
			if (inf.finished())
				allDone = true;
			if (rowfilled == rowlen || (allDone && rowfilled > 0)) {
				dataPendingForConsumer = true;
				if (isAsyncMode()) { // callback mode, this can recursively call this method again!
					int nextRowLen = eatRow();
					setNextRowLen(nextRowLen);
				}
			}
			return ninflated != 0;
		} catch (DataFormatException e) {
			throw new PngjInputException("error decompressing ", e);
		}
	}


	protected boolean isAsyncMode() {
		return false;
	}

	/** must be implemented if isAsyncMode */
	protected int eatRow() {
		throw new PngjInputException("not implemented");
	}

	
	/* the effective length is getRowFilled() */
	public byte[] getRow() {
		return row;
	}

	/**
	 * Pass 0 or negative to signal that we are done (not expecting more bytes)
	 * 
	 * This resets rowfilled,dataPendingForConsumer
	 */
	public void setNextRowLen(int len) {
		rowfilled = 0;
		dataPendingForConsumer = false;
		rown++;
		if (len > 0) {
			rowlen = len;
		} else {
			allDone = true;
		}
		if (!allDone)
			inflateData();
	}

	public boolean isAllDone() {
		return allDone;
	}

	public void end() {
		if(inf!=null && !inf.finished())
			inf.end();
		allDone=true;
	}

	public boolean isDataPendingForConsumer() {
		return dataPendingForConsumer;
	}

	/** This the the target size of the row , should coincide with row.length */
	public int getRowLen() {
		return rowlen;
	}

	/** This the amount of valid bytes in the buffer */
	public int getRowFilled() {
		return rowfilled;
	}

	public int getRown() {
		return rown;
	}

	public boolean allowOtherChunksInBetween() {
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("idatSet : " + curChunk.getChunkRaw().id + " done=" + allDone + " rows="
				+ rown);
		return sb.toString();
	}

}
