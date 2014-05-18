package ar.com.hjg.pngj.cli;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReaderInt;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjException;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;

public class CropImage {

  static StringBuilder help = new StringBuilder();
  private String geom;
  private List<File> listpng;
  private int w;
  private int h;
  private int ox;
  private int oy;
  private boolean forceOverwrite;
  private boolean quietMode;
  private FilterType filter;

  public static void run(String[] args) {

    help.append(" Crops a image   \n");
    help.append(" Syntax:  crop [options] [orig.png] [dest.png]\n");
    help.append("  Options:   \n");
    help.append("    -gGEOMETRY: crop size and (optioally) offset position (eg 40x30+10+10  40x30) \n");
    help.append("    -f Force overwriting \n");
    help.append("    -q Quiet mode \n");
    help.append("    -pPREDICTOR: PNG predictor type filter (0-5) By default the original is kept \n");
    help.append(" The geometry syntax is similar to ImageMagick (but more restricted) \n");

    CliArgs cli = CliArgs.buildFrom(args, help);
    cli.checkAtLeastNargs(2);
    CropImage me = new CropImage();
    me.geom = cli.getOpt("g", "");
    me.listpng = cli.listPngsFromArgs();
    me.quietMode = cli.hasOpt("q");
    me.forceOverwrite = cli.hasOpt("f");
    me.filter =
        FilterType.getByVal(Integer.parseInt(cli.getOpt("p",
            String.valueOf(FilterType.FILTER_PRESERVE.val))));
    cli.checkNoMoreOpts();
    if (me.geom.isEmpty())
      cli.badUsageAbort("must specify a geometry");
    if (me.listpng.size() != 2 || me.listpng.get(0).equals(me.listpng.get(1)))
      cli.badUsageAbort("Must specify origin and target (distinct)");
    if (me.listpng.get(1).exists() && !me.forceOverwrite)
      cli.badUsageAbort("Target exists, specify -f if you want to overwrite");
    if (!me.parseGeom())
      cli.badUsageAbort("Bad geometry (Examples: 40x30   40  40x30+10 40x30+10   ");
    me.doit();
  }

  private boolean parseGeom() {
    try {
      Pattern p = Pattern.compile("^(\\d+)(x\\d+)?([+-]\\d+)?([+-]\\d+)?$");
      Matcher m = p.matcher(geom);
      if (!m.matches())
        return false;
      int gc = m.groupCount();
      this.w = Integer.parseInt(m.group(1));
      this.h = m.group(2) != null ? Integer.parseInt(m.group(2).substring(1)) : 0;
      this.ox = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
      this.oy = m.group(4) != null ? Integer.parseInt(m.group(4)) : 0;
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return false;
    }
    return true;
  }

  private void doit() {
    if (!quietMode)
      System.out.printf("Cropping %s (%s) -> (%s) \n", listpng.get(0), listpng.get(1), geom);
    crop(listpng.get(0), listpng.get(1), w, h, ox, oy, filter);

  }

  public static void crop(File file1, File file2, int w2, int h2, int ox2, int oy2) {
    crop(file1, file2, w2, h2, ox2, oy2, FilterType.FILTER_PRESERVE);
  }

  public static void crop(File file1, File file2, int w2, int h2, int ox2, int oy2, FilterType ft) {
    PngReaderInt pngr = new PngReaderInt(file1);
    if (w2 == 0)
      w2 = pngr.imgInfo.cols;
    if (h2 == 0)
      h2 = (int) ((pngr.imgInfo.rows * w2) / (double) pngr.imgInfo.cols + 0.5);
    if (ox2 < 0) {
      w2 -= ox2;
      ox2 = 0;
    }
    if (oy2 < 0) {
      h2 -= oy2;
      oy2 = 0;
    }
    if (w2 + ox2 > pngr.imgInfo.cols)
      w2 = pngr.imgInfo.cols - ox2;
    if (h2 + oy2 > pngr.imgInfo.rows)
      h2 = pngr.imgInfo.rows - oy2;
    if (h2 < 1 || w2 < 1) {
      throw new PngjException("Bad dimensions");
    }
    ImageInfo imi2 = pngr.imgInfo.withSize(w2, h2);
    PngWriter pngw = new PngWriter(file2, imi2);
    pngw.copyChunksFrom(pngr.getChunksList(), ChunkCopyBehaviour.COPY_ALL);
    pngw.setFilterType(ft);
    ImageLineInt iline2 = new ImageLineInt(imi2);
    int[] line2 = iline2.getScanline();
    for (int r = oy2, rr = 0; rr < imi2.rows; rr++, r++) {
      ImageLineInt iline1 = (ImageLineInt) pngr.readRow(r);
      iline2.setFilterType(iline1.getFilterType());
      int[] line1 = iline1.getScanline();
      line2[0] = line1[0];
      System.arraycopy(line1, ox2 * imi2.channels, line2, 0, w2 * imi2.channels);
      pngw.writeRow(iline2);
    }
    pngr.end();
    pngw.end();
  }

  public static void main(String[] args) throws Exception {
    run(args);
  }
}
