package ar.com.hjg.pngj.cli;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import ar.com.hjg.pngj.BufferedStreamFeeder;
import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.ChunkSeqReaderPng;
import ar.com.hjg.pngj.DeflatedChunksSet;
import ar.com.hjg.pngj.IdatSet;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.chunks.ChunkHelper;

/**
 * see help
 */
public class ExtractIdat {

	static StringBuilder help = new StringBuilder();

	public static void run(String[] args) {

		help.append(" Decompress and unfilter the IDAT stream (pixels) and writes all to a stream \n");
		help.append("  Options:   \n");
		help.append("    -a: automatic output name : origname + '.idat' (does not require input and output files) \n");
		help.append("    -s: strip filter btye \n");
		help.append("    -q: quiet mode\n");
		help.append("    -f: faster mode: don't check CRC\n");
		help.append(
				" With -i accepts paths in the form 'mypath/*' (all pngs in dir) or 'mypath/**' (idem recursive)  \n");

		CliArgs cli = CliArgs.buildFrom(args, help);
		cli.checkAtLeastNargs(1);
		ExtractIdat me = new ExtractIdat();
		me.automaticRename = cli.hasOpt("a");
		me.quietMode = cli.hasOpt("q");
		me.fastMode = cli.hasOpt("f");
		me.stripFilterByte = cli.hasOpt("s");
		me.listpng = cli.listPngsFromArgs();
		cli.checkNoMoreOpts();
		if (me.listpng.size() != 2 && !(me.automaticRename))
			cli.badUsageAbort("You need to specifiy two files (origin and destination), or automatic rename (-a)");
		me.doit();
	}

	private boolean stripFilterByte;
	private boolean automaticRename;
	private boolean quietMode;
	private boolean fastMode;
	private List<File> listpng = new ArrayList<File>();

	private void doit() {
		if (automaticRename)
			for (File f : listpng)
				doitForFile(f, new File(f.getParent(), f.getName() + ".idat"));
		else
			doitForFile(listpng.get(0), listpng.get(1));
	}

	private void doitForFile(File file1, File file2) {
		try {
			InputStream fin = new BufferedInputStream(new FileInputStream(file1));
			OutputStream fout = new BufferedOutputStream(new FileOutputStream(file2));
			extractIdat(fin, fout, stripFilterByte, !fastMode);
		} catch (Exception e) {
			throw new PngjException("Fatal error processing " + file1 + " -> " + file2, e);
		}
		if (!quietMode) {
			System.out.printf("Extracted idat %s -> %s\n", file1.toString(), file2.toString());
		}
	}

	/** reads a PNG and writes to a file the raw (unfiltered) IDAT stream */
	public static class ChunkSeqPngRawPixels extends ChunkSeqReaderPng {
		private final OutputStream os;
		private final InputStream is;
		private boolean omitFilterByte = false;
		private boolean checkCrc = false;

		public ChunkSeqPngRawPixels(InputStream is, OutputStream os) {
			super(true); // callbackmode
			this.is = is;
			this.os = os;
		}

		public void readAll() {
			BufferedStreamFeeder bs = new BufferedStreamFeeder(is);
			bs.feedAll(this);
		}

		public void setOmitFilterByte(boolean omitFilterByte) {
			this.omitFilterByte = omitFilterByte;
		}

		public void setCheckCrc(boolean checkCrc) {
			this.checkCrc = checkCrc;
		}

		@Override
		protected DeflatedChunksSet createIdatSet(String id) {
			IdatSet ids = new IdatSet(id, true, imageInfo, deinterlacer) {
				@Override
				protected int processRowCallback() {
					int offset = omitFilterByte ? 1 : 0;
					PngHelperInternal.writeBytes(os, getUnfilteredRow(), offset, getRowFilled());
					return super.processRowCallback();
				}

				@Override
				protected void processDoneCallback() {
				}
			};
			return ids;
		}

		@Override
		public boolean shouldSkipContent(int len, String id) {
			return !ChunkHelper.isCritical(id); // skip all except critical!
		}

		@Override
		protected boolean shouldCheckCrc(int len, String id) {
			return checkCrc;
		}

		@Override
		protected void postProcessChunk(ChunkReader chunkR) {
			// System.out.println("chunk processed " + chunkR.getChunkRaw().id + " mode " +
			// chunkR.mode);
			super.postProcessChunk(chunkR);
		}

	}

	/** reads a PNG and and writes the uncompressed unfiltered raw stream to os */
	public static void extractIdat(InputStream is, OutputStream os, boolean stripFilterByte, boolean checkCrc) {
		try {
			ChunkSeqPngRawPixels cr = new ChunkSeqPngRawPixels(is, os);
			cr.setCheckCrc(checkCrc);
			cr.setOmitFilterByte(stripFilterByte);
			cr.readAll();
			is.close();
			os.close();
		} catch (IOException e) {
			throw new PngjException(e);
		}
	}

	public static void main(String[] args) {
		run(args);
	}
}
