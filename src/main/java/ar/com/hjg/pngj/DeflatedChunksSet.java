package ar.com.hjg.pngj;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A set of IDAT-like chunks which, concatenated, form a zlib stream.
 * <p>
 * The inflated stream is intented to be read as a sequence of "rows", of which
 * the caller knows the lengths (not necessary equal) and number.
 * <p>
 * Eg: For IDAT non-interlaced images, a row has bytesPerRow + 1 filter byte<br>
 * For interlaced images, the lengths are variable.
 * <p>
 * This class can work in sync (polled) mode or async (callback) mode
 * <p>
 * See {@link IdatSet}, which is mostly used and has a slightly simpler use.<br>
 * See <code>DeflatedChunkSetTest</code> for example of use.
 */
public class DeflatedChunksSet {

	protected byte[] row; // a "row" here means a raw (filtered) image row (or subimage row for interlaced) plus a filter byte
	private int rowfilled; //  effective/valid length of row
	private int rowlen; // what amount of bytes is to be interpreted as a complete "row". can change (for interlaced)
	private int rown; // only coincide with image row if non-interlaced  - incremented by setNextRowSize()

	/*
	 * States
	 * 
	 * processBytes() is externally called, prohibited in  READY
	 * (in DONE it's ignored)
	 * 
	 * WARNING: inflater.finished() != DONE (not enough, not neccesary)
	 *  DONE means that we have already uncompressed all the data of interest.
	 *  
	 * In non-callback mode, prepareForNextRow() is also externally called, in NORMAL_MODE
	 * 
	 * Flow:
	 *   - processBytes() calls inflateData()
	 *   - inflateData() : if buffer is filled goes to READY  
	 *                     else if ! inf.finished goes to WAITING
	 *                     else if any data goes to READY (incomplete data to be read)
	 *                     else goes to DONE      
	 *   - in Callback mode, after going to READY, n=processCallback() is called
	 *    and then prepareForNextRow(n) is called.
	 *   - in Polled mode,  prepareForNextRow(n) must be called from outside (after checking state=READY)
	 *   - prepareForNextRow(n) goes to DONE if n==0 
	 *   calls inflateData() again
	 *   - end() goes to  DONE
	 */
	private enum State {
		WAITING, // waiting for more input
		READY, // ready for consumption (might be less than fully filled), ephemeral for CALLBACK mode
		DONE;

		public boolean isFinished() {
			return this == DONE;
		} // the caller has already uncompressed all the data of interest or EOF  
	}

	State state;

	final Inflater inf;
	private final boolean infOwn; // true if the inflater is our own 

	private DeflatedChunkReader curChunk;

	private boolean callbackMode = true;
	private int nFedBytes = 0; // count the total compressed bytes that have been fed

	/**
	 * All IDAT-like chunks that form a same DeflatedChunksSet should have the
	 * same id
	 */
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
		nFedBytes += len;
		//PngHelperInternal.LOGGER.info("processing compressed bytes in chunkreader : " + len);
		if (len < 1 || state.isFinished())
			return;
		if (state == State.READY)
			throw new PngjInputException("this should only be called if waitingForMoreInput");
		if (inf.needsDictionary() || !inf.needsInput())
			throw new RuntimeException("should not happen");
		inf.setInput(buf, off, len);
		inflateData();
	}

	/* 
	 * This never inflates more than one row
	 * (but it can recurse!) 
	 */
	private void inflateData() {
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
		State nextstate = null;
		if (rowfilled == rowlen)
			nextstate = State.READY; // complete row, process it
		else if (!inf.finished())
			nextstate = State.WAITING;
		else if (rowfilled > 0)
			nextstate = State.READY; // complete row, process it
		else
			nextstate = State.DONE; // eof, no more data
		state = nextstate;
		if (state == State.READY) {
			preProcessRow();
			if (isCallbackMode()) { // callback mode
				int nextRowLen = processRowCallback();
				prepareForNextRow(nextRowLen);
			}
		}
	}

	/**
	 * Called automatically in all modes when a full row has been inflated.
	 */
	protected void preProcessRow() {

	}

	/**
	 * callback, must be implemented in callbackMode Must return byes of next
	 * row, for next callback
	 */
	protected int processRowCallback() {
		throw new PngjInputException("not implemented");
	}

	/**
	 * Inflated buffer.
	 * 
	 * The effective length is given by {@link #getRowFilled()}
	 */
	public byte[] getInflatedRow() {
		return row;
	}

	/**
	 * Should be called after the previous row was processed
	 * <p>
	 * Pass 0 or negative to signal that we are done (not expecting more bytes)
	 * <p>
	 * This resets {@link #rowfilled}
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
			inflateData();
		}
	}

	/**
	 * In this state, the object is waiting for more input to deflate.
	 * <p>
	 * Only in this state it's legal to feed this
	 */
	public boolean isWaitingForMoreInput() {
		return state == State.WAITING;
	}

	/**
	 * In this state, the object is waiting the caller to retrieve inflated data
	 * <p>
	 * Effective length: see {@link #getRowFilled()}
	 */
	public boolean isRowReady() {
		return state == State.READY;
	}

	/**
	 * In this state, all relevant data has been uncompressed and retrieved.
	 * <p>
	 * We can still feed this object, but the bytes will be swallowed/ignored.
	 */
	public boolean isDone() {
		return state.isFinished();
	}

	/** this should be called only when discarding this object, or for aborting */
	public void end() {
		try {
			if (!state.isFinished())
				state = State.DONE;
			if (infOwn)
				inf.end();// we end the Inflater only if we created it
		} catch (Exception e) {

		}
	}

	/**
	 * Target size of the current row, including filter byte. <br>
	 * should coincide (or be less than) with row.length
	 */
	public int getRowLen() {
		return rowlen;
	}

	/** This the amount of valid bytes in the buffer */
	public int getRowFilled() {
		return rowfilled;
	}

	/**
	 * Get current (last) row number.
	 * <p>
	 * This corresponds to the raw numeration of rows as seen by the deflater.
	 * Not the same as the real image row, if interlaced.
	 * 
	 */
	public int getRown() {
		return rown;
	}

	/**
	 * Some IDAT-like set can allow other chunks in between (APGN?).
	 * <p>
	 * Normally false.
	 * 
	 * @param id
	 *            Id of the other chunk that appeared in middel of this set.
	 * @return true if allowed
	 */
	public boolean allowOtherChunksInBetween(String id) {
		return false;
	}

	/**
	 * Callback mode = async processing
	 */
	public boolean isCallbackMode() {
		return callbackMode;
	}

	public void setCallbackMode(boolean callbackMode) {
		this.callbackMode = callbackMode;
	}

	/** total number of bytes that have been fed to this object */
	public long getnFedBytes() {
		return nFedBytes;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("idatSet : " + curChunk.getChunkRaw().id + " state=" + state + " rows="
				+ rown);
		return sb.toString();
	}

}
