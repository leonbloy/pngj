package ar.com.hjg.pngj;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ar.com.hjg.pngj.ChunkReader.ChunkReaderMode;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunksList;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkIDAT;
import ar.com.hjg.pngj.chunks.PngChunkIEND;
import ar.com.hjg.pngj.chunks.PngChunkIHDR;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkUnbuffered;

public class ChunkReaderFullSequence2 extends ChunkReaderFullSequence implements IChunkProcessor {
	ImageInfo imageInfo; // initialized at parsing the IHDR
	PngDeinterlacer deinterlacer;
	protected int currentChunkGroup = -1;

	private List<PngChunk> chunks = new ArrayList<PngChunk>();
	private Set<String> chunksToSkip = new HashSet<String>();

	protected int maxTotalBytesRead = 0;
	private int skipChunkMaxSize = 0;
	private int maxBytesMetadata = 0;
	private long bytesChunksLoaded = 0; // bytes loaded from buffered anciallary chunks

	public ChunkReaderFullSequence2() {
		setChunkProcessor((IChunkProcessor) this);
	}

	public void processChunkStart(ChunkReader chunkReader) {
		PngHelperInternal.debug("starting chunk " + chunkReader);
		curChunkReader = chunkReader;
		if (chunkReader.getChunkRaw().id.equals(PngChunkIHDR.ID)) { // IDHR
			if (currentChunkGroup < 0)
				currentChunkGroup = ChunksList.CHUNK_GROUP_0_IDHR;
			else
				throw new PngjInputException("bad chunk");
		} else if (chunkReader.getChunkRaw().id.equals(PngChunkPLTE.ID)) { // PLTE
			if ((currentChunkGroup == ChunksList.CHUNK_GROUP_0_IDHR || currentChunkGroup == ChunksList.CHUNK_GROUP_1_AFTERIDHR))
				currentChunkGroup = ChunksList.CHUNK_GROUP_2_PLTE;
			else
				throw new PngjInputException("bad chunk");
		} else if (chunkReader.getChunkRaw().id.equals(PngChunkIDAT.ID)) { // IDAT (no necessarily the first)
			if ((currentChunkGroup >= ChunksList.CHUNK_GROUP_0_IDHR && currentChunkGroup <= ChunksList.CHUNK_GROUP_4_IDAT))
				currentChunkGroup = ChunksList.CHUNK_GROUP_4_IDAT;
			else
				throw new PngjInputException("bad chunk");
		} else if (chunkReader.getChunkRaw().id.equals(PngChunkIEND.ID)) { // END
			if ((currentChunkGroup >= ChunksList.CHUNK_GROUP_4_IDAT))
				currentChunkGroup = ChunksList.CHUNK_GROUP_6_END;
			else
				throw new PngjInputException("bad chunk");
		} else { // ancillary
			if (currentChunkGroup <= ChunksList.CHUNK_GROUP_1_AFTERIDHR)
				currentChunkGroup = ChunksList.CHUNK_GROUP_1_AFTERIDHR;
			else if (currentChunkGroup <= ChunksList.CHUNK_GROUP_3_AFTERPLTE)
				currentChunkGroup = ChunksList.CHUNK_GROUP_3_AFTERPLTE;
			else
				currentChunkGroup = ChunksList.CHUNK_GROUP_5_AFTERIDAT;
		}
	}

	public void processChunkEnd(ChunkReader chunkReader) {
		PngChunk pngChunk = null;
		if (chunkReader.mode == ChunkReaderMode.BUFFER) {
			bytesChunksLoaded += chunkReader.getChunkRaw().len + 12;
			pngChunk = PngChunk.factory(chunkReader.getChunkRaw(), imageInfo);
			if (pngChunk instanceof PngChunkIHDR) {
				if (chunks.size() != 0)
					throw new PngjInputException("IHDR must be first chunk");
				imageInfo = ((PngChunkIHDR) pngChunk).createImageInfo();
				boolean interlaced = ((PngChunkIHDR) pngChunk).isInterlaced();
				if (interlaced)
					initInterlaced();
			}
			if (!ChunkHelper.isCritical(chunkReader.getChunkRaw().id))
				bytesChunksLoaded += chunkReader.getChunkRaw().len + 12;
		} else { // non buffered 
			pngChunk = new PngChunkUnbuffered(chunkReader.getChunkRaw().id, imageInfo, chunkReader.getChunkRaw().len);
		}
		pngChunk.setOffset(chunkReader.getChunkRaw().getOffset());
		if (imageInfo == null)
			throw new PngjInputException("IHDR not yet read?");
		chunks.add(pngChunk);
		PngHelperInternal.debug("ending chunk- adding to list " + pngChunk);
	}

	protected void initInterlaced() {
		deinterlacer = new PngDeinterlacer(imageInfo);
	}

	public boolean shouldCheckCrc(int len, String id) {
		return true;
	}

	public boolean shouldSkipContent(int len, String id) {
		if (maxTotalBytesRead > 0 && len + bytesRead > maxTotalBytesRead)
			throw new PngjInputException("Maximum total bytes to read exceeeded: " + maxTotalBytesRead + " offset:"
					+ bytesRead + " len=" + len);
		if (chunksToSkip.contains(id))
			return true; // specific skip
		if (skipChunkMaxSize > 0 && len > skipChunkMaxSize)
			return true; // too big chunk
		if (maxBytesMetadata > 0 && len > maxBytesMetadata - bytesChunksLoaded)
			return false; // too much ancillary chunks loaded 
		return false;
	}

	public boolean isIdatLike(String id) {
		return id.equals(PngChunkIDAT.ID);
	}

	public PngDeinterlacer getDeinterlacer() {
		return deinterlacer;
	}

	public List<PngChunk> getChunks() {
		return chunks;
	}

	public ChunkReader getCurChunkReader() {
		return curChunkReader;
	}

	@Override
	public int feed(byte[] buf, int off, int len) {
		return super.feed(buf, off, len);
	}

	public long getBytesChunksLoaded() {
		return bytesChunksLoaded;
	}

	public int getCurrentChunkGroup() {
		return currentChunkGroup;
	}

	public ChunkReaderDeflatedSet createNewIdatSetReader(String id) {
		ChunkReaderIdatSet ids = new ChunkReaderIdatSet(imageInfo, deinterlacer);
		return ids;
	}

	public ChunkReaderIdatSet getCurReaderIdatSet(){
		ChunkReaderDeflatedSet c = getCurReaderDeflatedSet();
		return c instanceof ChunkReaderIdatSet ? (ChunkReaderIdatSet)c : null;
	}

	public void setChunksToSkip(String... chunksToSkip) {
		this.chunksToSkip.clear();
		for (String c : chunksToSkip)
			this.chunksToSkip.add(c);
	}
	
	public void addChunkToSkip(String chunkToSkip) {
		this.chunksToSkip.add(chunkToSkip);
	}

	public void setMaxTotalBytesRead(int maxTotalBytesRead) {
		this.maxTotalBytesRead = maxTotalBytesRead;
	}

	@Override
	public void close() {
		if (currentChunkGroup != ChunksList.CHUNK_GROUP_6_END)// this could only happen if forced close
			currentChunkGroup = ChunksList.CHUNK_GROUP_6_END;
		super.close();
	}

	public boolean firstChunksNotYetRead() {
		return getCurrentChunkGroup()< ChunksList.CHUNK_GROUP_1_AFTERIDHR;
	}
	

}
