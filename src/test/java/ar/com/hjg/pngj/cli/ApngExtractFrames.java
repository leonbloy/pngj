package ar.com.hjg.pngj.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.ChunkSeqReaderPng;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.chunks.PngChunk;
import ar.com.hjg.pngj.chunks.PngChunkACTL;
import ar.com.hjg.pngj.chunks.PngChunkFCTL;
import ar.com.hjg.pngj.chunks.PngChunkFDAT;
import ar.com.hjg.pngj.chunks.PngChunkIDAT;
import ar.com.hjg.pngj.chunks.PngChunkIEND;
import ar.com.hjg.pngj.chunks.PngChunkIHDR;

/**
 * See help
 * 
 * This is low level, it does not use PngReaderApgn
 */
public class ApngExtractFrames {

	static StringBuilder help = new StringBuilder();
	private static final String PREFIX = "apngf";

	public static void run(String[] args) {

		help.append(" Extracts animation frames from APGN file to several PNG files \n");
		help.append(" Low level, very efficient. Does not compose frames \n");
		help.append(" Warning: this writes lots of files, in the same dir as the original PNGs. \n");
		help.append("  Options:   \n");
		help.append("    -q: quiet mode\n");
		help.append("    -p: prefix (default: '" + PREFIX + "') \n");
		help.append(" Accepts paths in the form 'mypath/*' (all pngs in dir) or 'mypath/**' (idem recursive)  \n");

		CliArgs cli = CliArgs.buildFrom(args, help);
		cli.checkAtLeastNargs(1);
		ApngExtractFrames me = new ApngExtractFrames();
		me.quietMode = cli.hasOpt("q");
		me.prefix = cli.getOpt("p", PREFIX);
		me.listpng = cli.listPngsFromArgs();
		cli.checkNoMoreOpts();
		if (me.prefix.matches("[^a-zA-Z0-9_.-]"))
			cli.badUsageAbort(
					"I don't like that prefix (" + me.prefix + "), better use only ascii alphanumeric and -._");
		me.doit();
	}

	private boolean quietMode;
	private String prefix;
	private List<File> listpng;

	private void doit() {
		for (File file : listpng) {
			try {
				int nf = process(file, prefix);
				if (!quietMode) {
					if (nf > 0)
						System.out.printf("%s APNG processed: %d frames extracted \n", file, nf);
					else
						System.out.printf("%s is not APNG \n", file);
				}
			} catch (Exception e) {
				System.err.println("Fatal error: " + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	static class PngReaderBuffered extends PngReader {
		private File orig;
		private String prefix = PREFIX;

		public PngReaderBuffered(File file) {
			super(file);
			this.orig = file;
		}

		FileOutputStream fo = null;
		File dest;
		ImageInfo frameInfo;
		int numframe = -1;

		@Override
		protected ChunkSeqReaderPng createChunkSeqReader() {
			return new ChunkSeqReaderPng(false) {
				@Override
				public boolean shouldSkipContent(int len, String id) {
					return false; // we dont skip anything!
				}

				@Override
				protected boolean isIdatKind(String id) {
					return false; // dont treat idat as special, jsut buffer it as is
				}

				@Override
				protected void postProcessChunk(ChunkReader chunkR) {
					super.postProcessChunk(chunkR);
					try {
						String id = chunkR.getChunkRaw().id;
						PngChunk lastChunk = chunksList.getChunks().get(chunksList.getChunks().size() - 1);
						if (id.equals(PngChunkFCTL.ID)) {
							numframe++;
							frameInfo = ((PngChunkFCTL) lastChunk).getEquivImageInfo();
							startNewFile();
						}
						if (id.equals(PngChunkFDAT.ID) || id.equals(PngChunkIDAT.ID)) {
							if (id.equals(PngChunkIDAT.ID)) {
								// copy IDAT as is (only if file is open == if FCTL previous == if IDAT is part
								// of the animation
								if (fo != null)
									chunkR.getChunkRaw().writeChunk(fo);
							} else {
								// copy fDAT as IDAT, trimming the first 4 bytes
								ChunkRaw crawi = new ChunkRaw(chunkR.getChunkRaw().len - 4, ChunkHelper.b_IDAT, true);
								System.arraycopy(chunkR.getChunkRaw().data, 4, crawi.data, 0, crawi.data.length);
								crawi.writeChunk(fo);
							}
							chunkR.getChunkRaw().data = null; // be kind, release memory
						}
						if (id.equals(PngChunkIEND.ID)) {
							if (fo != null)
								endFile(); // end last file
						}
					} catch (Exception e) {
						throw new PngjException(e);
					}
				}
			};
		}

		private void startNewFile() throws Exception {
			if (fo != null)
				endFile();
			dest = createOutputName();
			fo = new FileOutputStream(dest);
			fo.write(PngHelperInternal.getPngIdSignature());
			PngChunkIHDR ihdr = new PngChunkIHDR(frameInfo);
			ihdr.createRawChunk().writeChunk(fo);
			for (PngChunk chunk : getChunksList(false).getChunks()) {// copy all except actl and fctl, until IDAT
				String id = chunk.id;
				if (id.equals(PngChunkIHDR.ID) || id.equals(PngChunkFCTL.ID) || id.equals(PngChunkACTL.ID))
					continue;
				if (id.equals(PngChunkIDAT.ID))
					break;
				chunk.getRaw().writeChunk(fo);
			}
		}

		private void endFile() throws IOException {
			new PngChunkIEND(null).createRawChunk().writeChunk(fo);
			fo.close();
			fo = null;
		}

		private File createOutputName() {
			File dest = new File(orig.getParent(), prefix + (String.format("_%03d_", numframe)) + orig.getName());
			return dest;
		}

	}

	/**
	 * reads a APNG file and tries to split it into its frames - low level! Returns
	 * number of animation frames extracted
	 */
	public static int process(final File orig, String prefix) throws Exception {
		// we extend PngReader, to have a custom behavior: load all chunks opaquely,
		// buffering all, and react to some
		// special chnks
		PngReaderBuffered pngr = new PngReaderBuffered(orig);
		pngr.prefix = prefix;
		pngr.end(); // read till end - this consumes all the input stream and does all!
		return pngr.numframe + 1;
	}

	public static void main(String[] args) {
		run(args);
	}

}
