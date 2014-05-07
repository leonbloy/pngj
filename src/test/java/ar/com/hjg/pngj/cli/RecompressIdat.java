package ar.com.hjg.pngj.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;

import ar.com.hjg.pngj.ChunkReader;
import ar.com.hjg.pngj.PngIDatChunkOutputStream;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.PngjOutputException;
import ar.com.hjg.pngj.chunks.ChunkRaw;
import ar.com.hjg.pngj.misc.NullOs;
import ar.com.hjg.pngj.pixels.CompressorStream;
import ar.com.hjg.pngj.pixels.CompressorStreamDeflater;
import ar.com.hjg.pngj.samples.ChunkSeqReaderOpaque;

/**
 * see help
 */
public class RecompressIdat {

  static StringBuilder help = new StringBuilder();

  public static void run(String[] args) {

    help.append(" Recompress the ZLIB stream inside the PNG (IDAT chunks).   \n");
    help.append(" This is very efficient because it doesn't decode the image.   \n");
    help.append("  Options:   \n");
    help.append("    -i: inplace (does not require input and output files) \n");
    help.append("    -cLEVEL: zlib compression level (0-9) \n");
    help.append("    -sSTRATEGY: zlib strategy (0:default 1:filtered 2:Huffman) \n");
    help.append("    -q: quite mode\n");
    help.append("    -f: faster mode: don't check CRC\n");
    help.append("    -n: null output: don't write, just compute compression\n");
    help.append(" With -i accepts paths in the form 'mypath/*' (all pngs in dir) or 'mypath/**' (idem recursive)  \n");

    CliArgs cli = CliArgs.buildFrom(args, help);
    cli.checkAtLeastNargs(1);
    RecompressIdat me = new RecompressIdat();
    me.nullOutput = cli.hasOpt("n");
    me.inplace = cli.hasOpt("i");
    me.quietMode = cli.hasOpt("q");
    me.fastMode = cli.hasOpt("f");
    me.clevel = Integer.decode(cli.getOpt("c", "6"));
    me.strat = Integer.decode(cli.getOpt("s", "0"));
    me.listpng = cli.listPngsFromArgs();
    cli.checkNoMoreOpts();
    if (me.clevel < 0 || me.clevel > 9)
      cli.badUsageAbort("bad compression level (0-9)");
    if (me.strat < 0 || me.strat > 2)
      cli.badUsageAbort("bad strategy (0-2)");
    if (me.listpng.isEmpty())
      cli.badUsageAbort("No input files");
    if (me.listpng.size() != 2 && !(me.inplace || me.nullOutput))
      cli.badUsageAbort("You need to specifiy two files (origin and destination), or inplace (-i)");
    me.doit();
  }

  private boolean nullOutput;
  private boolean inplace;
  private boolean quietMode;
  private boolean fastMode;
  private Integer clevel;
  private Integer strat;
  private List<File> listpng;

  private void doit() {
    if (inplace || nullOutput)
      for (File f : listpng)
        doitForFile(f, f);
    else
      doitForFile(listpng.get(0), listpng.get(1));
  }

  private void doitForFile(File file1, File file2) {
    // long sizeori = file1.length();
    File dest = file1.equals(file2) ? new File(file2.getAbsolutePath() + ".tmp_pngj") : file2;
    long[] sizes;
    try {
      FileInputStream fin = new FileInputStream(file1);
      OutputStream fout = nullOutput ? new NullOs() : new FileOutputStream(dest);
      sizes = recompressPng(fin, fout, clevel, strat, !fastMode);
    } catch (Exception e) {
      throw new PngjException("Fatal error processing " + file1 + " -> " + file2, e);
    }
    if (!dest.equals(file2) && (!nullOutput)) {
      boolean ok = dest.renameTo(file2);
      if (!ok)
        throw new PngjException("Could not rename " + dest + " to " + file2);
    }
    if (!quietMode) {
      long sizeori = sizes[0];
      long sizef = sizes[2];
      double ratio = sizef / (double) sizeori - 1.0;
      boolean decrease = ratio < 0;
      // if(decrease) ratio =--ratio;
      System.out.printf("Recompressed %s (c=%d s=%d) Size %s %.3f%% %s\n",
          (inplace | nullOutput) ? file1.toString() : file1.toString() + " -> " + file2.toString(),
          clevel, strat, decrease ? "decreased" : "increased", ratio * 100, Arrays.toString(sizes));
    }
  }

  public static class ChunkSeqReaderIdatRaw extends ChunkSeqReaderOpaque {
    private final OutputStream rawOs;
    private final OutputStream idatOs;
    private boolean checkCrc = true;

    /**
     * This reader writes the chunks directly to the rawOs output, except that the IDAT stream is decompressed and writen to idatOs
     * 
     * @param is
     * @param rawOs
     * @param idatOs
     * @param rowsize
     */
    public ChunkSeqReaderIdatRaw(OutputStream rawOs, OutputStream idatOs) {
      super();
      this.rawOs = rawOs;
      this.idatOs = idatOs;
      bufferChunks = false; // we force processing for all chunks

    }

    @Override
    protected void checkSignature(byte[] buf) {
      super.checkSignature(buf);
      // we also write this to rawOs
      writeToOs(rawOs, buf, 0, buf.length);
    }

    @Override
    protected void processChunkContent(ChunkRaw chunkRaw, int offInChunk, byte[] buf, int off,
        int len) {
      if (offInChunk == 0 && rawOs != null) { // this can be called several times for a single
                                              // chunk, we do this only the first time
        chunkRaw.writeChunkHeader(rawOs);
      }
      writeToOs(rawOs, buf, off, len);
      // PngHelperInternal.debug("processChunkContent " + chunkRaw);

    }

    @Override
    protected void startNewChunk(int len, String id, long offset) {
      // PngHelperInternal.debug("startNewChunk " + id);
      super.startNewChunk(len, id, offset);
    }

    @Override
    protected void postProcessChunk(ChunkReader chunkR) {
      if (!isIdatKind(chunkR.getChunkRaw().id))
        chunkR.getChunkRaw().writeChunkCrc(rawOs);
      // PngHelperInternal.debug("postprocessing " + chunkR.getChunkRaw());
      super.postProcessChunk(chunkR);
    }

    @Override
    protected void processIdatInflatedData(byte[] inflatedRow, int off, int len) {
      if (len > 0)
        writeToOs(idatOs, inflatedRow, off, len);
    }

    @Override
    protected void processIdatDone() {
      try {
        if (idatOs != null)
          idatOs.close();
      } catch (IOException e) {
        throw new PngjOutputException(e);
      }
    }

    @Override
    protected boolean shouldCheckCrc(int len, String id) {
      return checkCrc;
    }

    protected void writeToOs(OutputStream o, byte[] buf, int off, int len) {
      try {
        if (o != null)
          o.write(buf, off, len);
      } catch (IOException e) {
        throw new PngjOutputException(e);
      }
    }

  }

  /** reads a PNG and writes to a file the raw (unfiltered) IDAT stream */
  /*
   * public static void writeRawIdatToFile(File png, File rawIdat) { try { InputStream is = new FileInputStream(png); OutputStream osIdat = new FileOutputStream(rawIdat);
   * OutputStream osRaw = new NullOs(); ChunkSeqReaderIdatRaw1 reader = new ChunkSeqReaderIdatRaw1(is, osRaw, osIdat); reader.readAll(); is.close(); osIdat.close(); osRaw.close();
   * } catch (IOException e) { throw new PngjException(e); } }
   */

  /** reads a PNG and rewrites it recompressing the IDAT stream . this closes all streams */
  public static long[] recompressPng(InputStream is, OutputStream os, int clevel,
      int deflaterStrategy, boolean checkCrc) {
    try {
      PngIDatChunkOutputStream idatSt = new PngIDatChunkOutputStream(os);
      Deflater def = new Deflater(clevel);
      def.setStrategy(deflaterStrategy);
      CompressorStream cs = new CompressorStreamDeflater(idatSt, 8000, Long.MAX_VALUE, def);
      ChunkSeqReaderIdatRaw reader = new ChunkSeqReaderIdatRaw(os, cs);
      reader.checkCrc = checkCrc;
      reader.feedFromInputStream(is);
      is.close();
      cs.close();
      idatSt.close();
      os.close();
      long inputbytes = reader.getCurReaderDeflatedSet().getBytesIn();
      long rawbytes = reader.getCurReaderDeflatedSet().getBytesOut();
      long outputbytes = cs.getBytesCompressed();
      return new long[] {inputbytes, rawbytes, outputbytes};
    } catch (Exception ex) {
      throw new PngjException(ex);
    }
  }

  public static void main(String[] args) {
    run(args);
  }
}
