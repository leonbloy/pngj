package ar.com.hjg.pngj;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A set of IDAT like chunks that are concatenated
 */
public class ChunkReaderIdatSet {

	final Inflater inf;
	String chunkid = null; // all chunks must be of the same type
	byte[] row;
	int rowsize = 0; // what amount of bytes is to be interpreted as a "row". this should equals inflated.length ; can change (for interlaced)
	int rowfilled = 0; //  should grow until rowsize
	int rown = -1; // only coincide with image row if non-interlaced 
	public final boolean asyncMode;
	boolean weAreDone = false;

	List<Integer> chunksLenghts = new ArrayList<Integer>(); // informational
	List<Long> chunksOffsets = new ArrayList<Long>(); // informational

	public boolean isDone() {
		return weAreDone;
	}

	IdatProcessRow callbackRow; // only for asyncmode

	public ChunkReaderIdatSet() {
		this(true);
	}

	public ChunkReaderIdatSet(boolean async) {
		this.inf = new Inflater();
		this.asyncMode = async;
	}

	public void setCallbackRow(IdatProcessRow callbackRow) {
		this.callbackRow = callbackRow;
	}

	public boolean filled() {
		return rowfilled == rowsize;
	}

	public int getRowsize() {
		return rowsize;
	}

	void process() { // this does never block  - in async mode, a single call to this method can result in processing more than one row
		if (rowsize == 0)
			throw new PngjException("ChunkProcessorIdat error: rowsize not set?");
		if (inf.finished() && !weAreDone)
			throw new PngjInputException("ChunkProcessorIdat error: compressed stream ended prematurely ?");
		if (asyncMode) { // 
			while (!(inf.needsInput() || weAreDone || inf.finished())) {
				trytofillrow();
				if (filled()) {
					int n = callbackRow.processRow(row, 0, rowsize, rown);
					setNextRowSize(n);
				}
			}
		} else { // sync mode
			while (!(inf.finished() || inf.needsInput() || filled())) {
				trytofillrow();
			}
		}
		if (weAreDone)
			inf.end();
	}

	//To be used in sync mode- you should call setNextRowSize after
	public byte[] getRow() {
		if (!filled())
			throw new PngjExceptionInternal("getRow should only be called when filled==true");
		return row;
	}

	/* 0 or negative if done if done - should be called before start processing */
	public void setNextRowSize(int size) {
		rowfilled = 0;
		rown++;
		if (size > 0) {
			rowsize = size;
			if (row == null || row.length != rowsize)
				row = new byte[rowsize];
		} else {
			weAreDone = true;
			if (!inf.finished()) // TODO: how to deal with this?
				System.err.println("IDAT set ended but still the inflater has data??");
		}
	}

	private void trytofillrow() {
		int n;
		try {
			n = inf.inflate(row, rowfilled, rowsize - rowfilled);
		} catch (DataFormatException e) {
			throw new PngjInputException("error decompressing ", e);
		}
		rowfilled += n;
	}

	public void reportNewChunk(String id, int len, long offsetInPng) {
		if (this.chunkid != null && !chunkid.equals(id))
			throw new PngjInputException("Bad chunk id " + id + " expected " + chunkid);
		this.chunkid = id;
		chunksLenghts.add(len);
		chunksOffsets.add(offsetInPng);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("idatSet : " + chunkid + " done=" + weAreDone + " rows=" + rown);
		for (int i = 0; i < chunksLenghts.size(); i++) {
			sb.append("\n[").append(chunksLenghts.get(i)).append(",").append(chunksOffsets.get(i)).append("]\n");
		}
		return sb.toString();
	}
}
