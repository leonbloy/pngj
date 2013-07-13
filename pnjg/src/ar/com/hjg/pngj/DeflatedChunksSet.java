package ar.com.hjg.pngj;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A set of IDAT-like chunks which, concatenated, form a zlib stream.
 * 
 * The inflated stream is intented to be read as a sequence of "rows", of which
 * the caller knows the length and number.
 * 
 * Eg: For IDAT non-interlaced images, a row has bytesPerRow + 1 filter byte
 * 
 * For interlaced images, the lengths are variable.
 * 
 * Can work in sync (polled) mode or async (callback) mode
 * 
 * See DeflatedChunkSetTest for example of use. See IdatSet, which is mostly
 * used and has a slightly simpler use
 */
public class DeflatedChunksSet {

	protected byte[] row; // a "row" here means a raw (filtered) image row (or subimage row for interlaced) plus a filter byte
	private int rowfilled; //  effective/valid length of row
	private int rowlen; // what amount of bytes is to be interpreted as a complete "row". can change (for interlaced)
	private int rown; // only coincide with image row if non-interlaced  - incremented by setNextRowSize()

	/*
	 * States
	 * 
	 * processBytes() is externally called, prohibited in  S_READY
	 * (in DONE/DONE_ABORTED it's ignored)
	 * 
	 * In non-callback mode, prepareForNextRow() is also externally called, in NORMAL_MODE
	 * 
	 * Flow:
	 *   - processBytes() calls inflateData()
	 *   - inflateData() goes to READY if buffer is filled or inf.finished, 
	 *            elsewhere to WAITING
	 *   - in Callback mode, after going to READY, n=processCallback() is called
	 *    and then prepareForNextRow(n) is called.
	 *   - in Polled mode,  prepareForNextRow(n) must be called from outside (after checking state=READY)
	 *   - prepareForNextRow(n) goes to DONE if n==0, to ABORTED if inf.finished, elsewhere
	 *   calls inflateData() again
	 *   - end() goes to ABORTED (unless already in DONE)
	 */
	private enum State {
		WAITING, // waiting for more input
		READY, // ready for consumption (might be less than fully filled), ephemeral for CALLBACK mode
		DONE, ABORTED;
		public boolean isCompleted() {
			return this == DONE || this == ABORTED;
		}
	}

	private State state;

	private final Inflater inf;
	private final boolean infOwn; // true if the inflater is our own 

	private DeflatedChunkReader curChunk;

	private boolean callbackMode = true;
	public final String chunkid;

	/**
	 * @param initialRowLen
	 *            Length in bytes of first "row" (see description)
	 * @param maxRowLen
	 *            Max length in bytes of "rows"
	 * @param inflater
	 *            Can be null. If not null, will be reset
	 */
	public DeflatedChunksSet(String chunkid, int initialRowLen, int maxRowLen, Inflater inflater, byte[] buffer) {
		this.chunkid = chunkid;
		this.rowlen = initialRowLen;
		this.inf = inflater != null ? inflater : new Inflater();
		if (inflater != null) {
			inf.reset();
			infOwn = false;
		} else
			infOwn = true;
		if (initialRowLen < 1 || maxRowLen < initialRowLen)
			throw new PngjException("bad inital row len " + initialRowLen);
		this.row = buffer != null && buffer.length >= initialRowLen ? buffer : new byte[maxRowLen];
		state = State.WAITING;
		rown = -1;
		prepareForNextRow(initialRowLen);
	}

	public DeflatedChunksSet(String chunkid, int initialRowLen, int maxRowLen) {
		this(chunkid, initialRowLen, maxRowLen, null, null);
	}

	protected void appendNewChunk(DeflatedChunkReader cr) {
		// all chunks must have same id
		if (!this.chunkid.equals(cr.getChunkRaw().id))
			throw new PngjInputException("Bad chunk inside IdatSet, id:" + cr.getChunkRaw().id + ", expected:"
					+ this.chunkid);
		this.curChunk = cr;
	}

	/**
	 * Feeds the inflater with the compressed bytes
	 * 
	 * In poll mode, the caller should not call repeatedly this, without
	 * consuming first, checking isDataReadyForConsumer()
	 * 
	 * @param buf
	 * @param off
	 * @param len
	 */
	protected void processBytes(byte[] buf, int off, int len) {
		//PngHelperInternal.LOGGER.info("processing compressed bytes in chunkreader : " + len);
		if (len < 1 || state.isCompleted())
			return;
		if (state == State.READY)
			throw new PngjInputException("this should only be called if waitingForMoreInput");
		if (inf.needsDictionary() || !inf.needsInput())
			throw new RuntimeException("should not happen");
		inf.setInput(buf, off, len);
		inflateData1();
	}

	/* 
	 * This never inflates more than one row
	 * (but it can recurse!) 
	 **/
	private void inflateData1() {
		int ninflated = 0;
		if (row == null || row.length < rowlen)
			row = new byte[rowlen]; // should not happen
		if (rowfilled < rowlen) {
			try {
				ninflated = inf.inflate(row, rowfilled, rowlen - rowfilled);
			} catch (DataFormatException e) {
				throw new PngjInputException("error decompressing zlib stream ", e);
			}
			rowfilled += ninflated;
		}
		if (rowfilled == rowlen || inf.finished()) {
			if (getRowLen() > -1) {
				state = State.READY;
				preProcessRow();
				if (isCallbackMode()) { // callback mode
					int nextRowLen = processRowCallback();
					prepareForNextRow(nextRowLen);
				}
			} else {

			}
		} else {
			state = State.WAITING;
		}
	}

	/** called automatically in all modes when a full row has been inflated */
	protected void preProcessRow() {

	}

	/**
	 * callback, must be implemented in callbackMode Must return byes of next
	 * row, for next callback
	 * */
	protected int processRowCallback() {
		throw new PngjInputException("not implemented");
	}

	/* the effective length is getRowFilled() */
	public byte[] getInflatedRow() {
		return row;
	}

	/**
	 * Should be called after the previous row was processed
	 * 
	 * Pass 0 or negative to signal that we are done (not expecting more bytes)
	 * 
	 * This resets rowfilled
	 */
	protected void prepareForNextRow(int len) {
		rowfilled = 0;
		rown++;
		if (len < 1) {
			rowlen = 0;
			state = State.DONE;
		} else if (inf.finished()) {
			rowlen = 0;
			state = State.DONE;
		} else {
			rowlen = len;
			inflateData1();
		}
	}

	public boolean isWaitingForMoreInput() {
		return state == State.WAITING;
	}

	public boolean isRowReady() {
		return state == State.READY;
	}

	public boolean isDone() {
		return state.isCompleted();
	}

	/** this should be called only when discarding this object */
	public void end() {
		if (!state.isCompleted())
			state = State.ABORTED;
		if (infOwn)
			inf.end();// we end the Inflater only if we created it
	}

	/**
	 * This the the target size of the current row, should coincide (or be less
	 * than) with row.length
	 */
	public int getRowLen() {
		return rowlen;
	}

	/** This the amount of valid bytes in the buffer */
	public int getRowFilled() {
		return rowfilled;
	}

	/**
	 * This corresponds to the raw numeration of rows as seen by the deflater
	 * 
	 * @return
	 */
	public int getRown() {
		return rown;
	}

	public boolean allowOtherChunksInBetween(String id) {
		return false;
	}

	public boolean isCallbackMode() {
		return callbackMode;
	}

	public void setCallbackMode(boolean callbackMode) {
		this.callbackMode = callbackMode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("idatSet : " + curChunk.getChunkRaw().id + " state=" + state + " rows="
				+ rown);
		return sb.toString();
	}

}
