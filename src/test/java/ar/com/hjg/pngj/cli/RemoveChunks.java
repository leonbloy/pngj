package ar.com.hjg.pngj.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.ChunkReader.ChunkReaderMode;
import ar.com.hjg.pngj.PngjOutputException;
import ar.com.hjg.pngj.chunks.ChunkHelper;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.misc.MappedCounterInt;
import ar.com.hjg.pngj.samples.ChunkSeqReaderOpaque;

/**
 * See help
 */
public class RemoveChunks {
	static StringBuilder help = new StringBuilder();
	
	public static void run(String[] args) throws Exception {

		help.append(" Removes ancillary chunks.   \n");
		help.append("  Options:   \n");
		help.append("    -i: inplace (does not require input and output files) \n");
		help.append("    -r: list of chunks to remove, comma separated. Case insensitive \n");
		help.append("    -k: list of ancillary chunks to keep, comma separated. Case insensitive \n");
		help.append("    -q: quite mode\n");
		help.append("    -f: faster mode: don't check CRC\n");
		help.append(" With -i accepts paths in the form 'mypath/*' (all pngs in dir) or 'mypath/**' (idem recursive)  \n");
		help.append(" Examples:  \n");
		help.append("   RemoveChunks -rTEXT ori.png dest.png    \n");
		help.append("        (removes TEXT from ori.png, saves in dest.png ) \n");
		help.append("   RemoveChunks -i -q -rTRNS,BKGD  mydir/**  \n");
		help.append("   RemoveChunks -i -kTRNS,BKGD  a.png b.png c.png  \n");
		help.append("        (removes all ancillary chunks except TRNS BKGD, in place in a.png b.png c.png) \n");

		CliArgs cli = CliArgs.buildFrom(args, help);
		cli.checkAtLeastNargs(1);
		RemoveChunks me = new RemoveChunks();
		me.inplace = cli.hasOpt("i");
		me.quietMode = cli.hasOpt("q");
		me.fastMode = cli.hasOpt("f");
		String tokeep = cli.getOpt("k");
		String toremove = cli.getOpt("r");
		me.listpng = cli.listPngsFromArgs();
		cli.checkNoMoreOpts();
		if (tokeep != null && toremove != null)
			cli.badUsageAbort("You must either specify a group to remove or to keep, not both");
		if (tokeep == null && toremove == null)
			cli.badUsageAbort("You must either specify a group to remove or to keep");
		me.keep = tokeep != null;
		String errmsg = me.parseChunks(me.keep ? tokeep : toremove);
		if (errmsg != null)
			cli.badUsageAbort(errmsg);
		if (me.listpng.isEmpty())
			cli.badUsageAbort("No input files");
		if (me.listpng.size() != 2 && !me.inplace)
			cli.badUsageAbort("You need to specifiy two files (origin and destination), or inplace (-i)");
		me.doit();
	}

	private String parseChunks(String chunkslist) {
		String[] chunksx = chunkslist.split(",");
		chunks = new HashSet<String>();
		for (String c : chunksx) {
			if (c.length() != 4)
				return "Bad chunk id '" + c + "from list [" + chunkslist + "]";
			chunks.add(c.toUpperCase());
		}
		return null;
	}

	private List<File> listpng;
	private boolean inplace = false;
	private boolean quietMode = false;
	private boolean fastMode=false;
	private boolean keep = false;
	private Set<String> chunks; // uppercase!

	private void doit() throws Exception {
		if (inplace)
			for (File f : listpng)
				doitForFile(f, f);
		else
			doitForFile(listpng.get(0), listpng.get(1));
	}

	public void doitForFile(File file1, File file2) throws Exception {// warning: file1 can be the same as file2 (in place)
		File dest = file1.equals(file2) ? new File(file2.getAbsolutePath() + ".tmp_pngj") : file2;
		FileInputStream fin = new FileInputStream(file1);
		FileOutputStream fout = new FileOutputStream(dest);
		ChunkSeqReaderRemoveChunks c = new ChunkSeqReaderRemoveChunks(fout);
		c.quiet = quietMode;
		c.chunksIds.addAll(chunks);
		c.keep = keep;
		c.checkrc=!fastMode;
		c.feedFromInputStream(fin);
		fin.close();// should not be necessary
		fout.flush();
		fout.close();// should not be necessary
		if (!dest.equals(file2)) {
			boolean ok = dest.renameTo(file2);
			if(!ok) throw new Exception("Could not rename " + dest + " to " +file2 );
		}
		if (!quietMode) {
			int r = c.getNremoved();
			if(r==0)
				System.out.printf("no chunks removed : %s -> %s\n",file1,file2);
			else
				System.out.printf("%d chunks removed (%s): %s -> %s\n",r,c.reportRemoved(),file1,file2);
		}
	}

	public static class ChunkSeqReaderRemoveChunks extends ChunkSeqReaderOpaque {

		public boolean checkrc=false;
		private final OutputStream os;
		public boolean quiet;
		public Set<String> chunksIds = new HashSet<String>(); // uppercase!
		public MappedCounterInt removedChunks = new MappedCounterInt();
		public boolean keep;

		public ChunkSeqReaderRemoveChunks(OutputStream os) {
			super();
			this.os = os;
		}

		@Override
		protected void processChunkContent(ChunkRaw chunkRaw, int offInChunk, byte[] buf, int off, int len) {
			if (offInChunk == 0) { // this can be called several times for a single chunk, we do this only the first time
				chunkRaw.writeChunkHeader(os);
			}
			writeos(buf, off, len);
		}

		@Override
		protected void postProcessChunk(ChunkReader chunkR) {
			String id = chunkR.getChunkRaw().id;
			if (chunkR.mode == ChunkReaderMode.SKIP) {
				removedChunks.add(id, 1);
			} else {
				chunkR.getChunkRaw().writeChunkCrc(os);
			}
			super.postProcessChunk(chunkR);
		}

		@Override
		protected void checkSignature(byte[] buf) {
			super.checkSignature(buf);
			writeos(buf, 0, buf.length);
		}

		private void writeos(byte[] buf, int off, int len) {
			try {
				os.write(buf, off, len);
			} catch (IOException e) {
				throw new PngjOutputException(e);
			}
		}

		@Override
		protected boolean isIdatKind(String id) {
			return false; // We treat IDAT as any other chunk here
		}

		@Override
		protected boolean shouldSkipContent(int len, String id) {
			if (ChunkHelper.isCritical(id))
				return false;
			boolean ispresent = chunksIds.contains(id.toUpperCase());
			if (keep && !ispresent)
				return true; // skip
			if (ispresent && !keep)
				return true;// skip
			return false;
		}

		@Override
		protected boolean shouldCheckCrc(int len, String id) {
			return checkrc; // more speed
		}

		public int getNremoved() {
			int r = 0;
			for (String id : removedChunks.getKeys())
				r += removedChunks.get(id);
			return r;
		}

		public String reportRemoved() {
			StringBuilder sb = new StringBuilder();
			for (String id : removedChunks.getKeys())
				sb.append(id + ":" + Integer.toString(removedChunks.get(id)) + " ");
			return sb.toString();
		}
	}

	public static void main(String[] args) throws Exception {
		run(args);
	}

}
