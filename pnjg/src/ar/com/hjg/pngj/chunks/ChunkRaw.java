package ar.com.hjg.pngj.chunks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngjBadCrcException;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.PngjInputException;
import ar.com.hjg.pngj.PngjOutputException;

/**
 * Raw (physical) chunk.
 * <p>
 * Short lived object, to be created while serialing/deserializing Do not reuse
 * it for different chunks. <br>
 * See http://www.libpng.org/pub/png/spec/1.2/PNG-Structure.html
 */
public class ChunkRaw {
	/**
	 * The length counts only the data field, not itself, the chunk type code,
	 * or the CRC. Zero is a valid length. Although encoders and decoders should
	 * treat the length as unsigned, its value must not exceed 231-1 bytes.
	 */
	public final int len;

	/**
	 * A 4-byte chunk type code. uppercase and lowercase ASCII letters
	 */
	public final byte[] idbytes;
	public final String id;

	/**
	 * The data bytes appropriate to the chunk type, if any. This field can be
	 * of zero length. Does not include crc. If it's null, it means that the
	 * data is ot available
	 */
	public byte[] data = null;

	/**
	 * A 4-byte CRC (Cyclic Redundancy Check) calculated on the preceding bytes
	 * in the chunk, including the chunk type code and chunk data fields, but
	 * not including the length field.
	 */
	public byte[] crcval = new byte[4];

	/**
	 * offset in the full PNG stream, only informational, for read
	 */
	private long offset = 0;

	
	private CRC32 crcengine;
	
	public  ChunkRaw(int len, String id, boolean alloc) {
		this.len = len;
		this.id = id;
		this.idbytes = ChunkHelper.toBytes(id);
		for(int i=0;i<4;i++) {
			if( idbytes[i] <65 || idbytes[i]>122 || (idbytes[i]>90&&idbytes[i]<97)) throw new PngjException("Bad id chunk: must be ascii letters " + id);
		}
		if (alloc)
			allocData();
	}
	
	public  ChunkRaw(int len, byte[] idbytes, boolean alloc) {
		this(len,ChunkHelper.toString(idbytes),alloc);
	}
	
	
	public void allocData() { // TODO: not public
		if (data == null || data.length < len)
			data = new byte[len];
	}

	/**
	 * this is called after setting data, before writing to os
	 */
	private void computeCrcForWriting() {
		crcengine =new CRC32();
		crcengine.update(idbytes, 0, 4);
		if (len > 0)
			crcengine.update(data, 0, len); //
		PngHelperInternal.writeInt4tobytes((int) crcengine.getValue(), crcval, 0);
	}

	/**
	 * Computes the CRC and writes to the stream. If error, a
	 * PngjOutputException is thrown
	 */
	public void writeChunk(OutputStream os) {
		if (idbytes.length != 4)
			throw new PngjOutputException("bad chunkid [" + ChunkHelper.toString(idbytes) + "]");
		PngHelperInternal.writeInt4(os, len);
		PngHelperInternal.writeBytes(os, idbytes);
		if (len > 0)
			PngHelperInternal.writeBytes(os, data, 0, len);
		computeCrcForWriting();
		PngHelperInternal.writeBytes(os, crcval, 0, 4);
	}

	public void checkCrc() {
		if(crcengine==null) crcengine = new CRC32();
		int crcComputed = (int) crcengine.getValue();
		int crcExpected = PngHelperInternal.readInt4fromBytes(crcval, 0);
		if (crcComputed != crcExpected)
			throw new PngjBadCrcException("chunk: " + this.toString() + " expected=" + crcExpected + " read="
					+ crcComputed);
	}
	
	ByteArrayInputStream getAsByteStream() { // only the data
		return new ByteArrayInputStream(data);
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public String toString() {
		return "chunkid=" + ChunkHelper.toString(idbytes) + " len=" + len;
	}

	public void updateCrc(byte[] buf, int off, int len) {
		if(crcengine==null) crcengine = new CRC32();
		crcengine.update(buf, off, len);
	}

}
