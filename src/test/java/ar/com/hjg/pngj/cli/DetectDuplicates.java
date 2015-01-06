package ar.com.hjg.pngj.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.PngReaderByte;

/** see help */
public class DetectDuplicates {

  static StringBuilder help = new StringBuilder();
  static final String REMOVE__DUP__PNG_ = "REMOVE__DUP__PNG_";

  public static void run(String[] args) {

    help.append("Finds PNG images that are 'almost duplicates'.     \n");
    help.append("  Options:   \n");
    help.append("    -r: rename duplicates preppending '" + REMOVE__DUP__PNG_
        + "' to the filename\n");
    help.append("  They are considered so if they have the same digest: basically, same pixel content \n");
    help.append("     Warning: interlaced/non-interlaced are considered different \n");
    help.append("     Warning: differences in Palette and other metadata are not considered \n");
    help.append("  Accepts list of files and/or paths in the form 'mypath/*' (all pngs in dir) or 'mypath/**' (idem recursive)  \n");

    CliArgs cli = CliArgs.buildFrom(args, help);
    cli.checkAtLeastNargs(1);
    DetectDuplicates me = new DetectDuplicates();
    me.renameDups = cli.hasOpt("r");
    me.listpng = cli.listPngsFromArgs();
    cli.checkNoMoreOpts();
    me.doit();
  }

  private List<File> listpng;
  private boolean renameDups = false;

  private DetectDuplicates() {}

  private void doit() {
    HashMap<String, List<File>> digests = new HashMap<String, List<File>>();
    HashSet<File> checked = new HashSet<File>();
    Collection<File> tocheck = listpng;
    @SuppressWarnings("unused")
    IImageLine iline = null;
    // first pass: group coincidences in imginfo only
    for (File f : tocheck) {
      if (checked.contains(f)) {
        System.err.println("file already included : " + f + " ignoring");
        continue;
      }
      checked.add(f);
      PngReaderByte png = null;
      try {
        png = new PngReaderByte(f);
        png.end();
      } catch (Exception e) {
        System.out.printf("%s : %s\n", ShowPngInfo.BAD_OR_NOT_PNG, f);
        continue;
      } finally {
        if (png != null)
          png.close();
      }
      String digest = png.imgInfo.toStringBrief();
      if (png.interlaced)
        digest += "_i";
      if (!digests.containsKey(digest)) {
        digests.put(digest, new ArrayList<File>());
      }
      digests.get(digest).add(f);
    }
    tocheck = new ArrayList<File>();
    for (List<File> list : digests.values()) {
      if (list.size() > 1)
        tocheck.addAll(list);
    }
    digests.clear();
    // second pass
    for (File f : tocheck) {
      PngReaderByte png = null;
      try {
        png = new PngReaderByte(f);
        png.prepareSimpleDigestComputation();
        while (png.hasMoreRows())
          iline = png.readRow();
        png.end();
      } catch (Exception e) {
        System.out.printf("%s : %s\n", ShowPngInfo.BAD_OR_NOT_PNG, f);
        continue;
      } finally {
        if (png != null)
          png.close();
      }
      String digest = png.getSimpleDigestHex();
      if (!digests.containsKey(digest)) {
        digests.put(digest, new ArrayList<File>());
      }
      digests.get(digest).add(f);
    }

    System.out.printf("# Total checked files: %d\n", checked.size());
    checked.clear();
    int dupsgroups = 0;
    int toberemoved = 0;
    for (List<File> list : digests.values()) {
      if (list.size() == 1)
        continue;
      reportDups(list);
      dupsgroups++;
      toberemoved += list.size() - 1;
    }
    digests.clear();
    System.out.printf("# Total dupgroups: %d. Files that could be removed: %d\n", dupsgroups,
        toberemoved);

  }

  private void reportDups(List<File> listfiles) {
    final HashMap<File, Long> sizeperfile = new HashMap<File, Long>();
    HashSet<Long> sizes = new HashSet<Long>();
    for (File f : listfiles) {
      Long size = f.length();
      sizes.add(size);
      sizeperfile.put(f, size);
    }
    Collections.sort(listfiles, new Comparator<File>() {
      public int compare(File o1, File o2) {
        return sizeperfile.get(o1).compareTo(sizeperfile.get(o2));
      }
    });
    System.out.print("### " + listfiles.size() + " duplicated files. ");
    if (sizes.size() == listfiles.size())
      System.out.print(" Different sizes.\n");
    else if (sizes.size() == 1)
      System.out.print(" Same size.\n");
    else
      System.out.print(" Some have same size.\n");
    for (int i = 0; i < listfiles.size(); i++) {
      boolean renamed = false;
      File f = listfiles.get(i);
      File fn = f;
      if (i > 0 && renameDups) {
        if (!f.getName().startsWith(REMOVE__DUP__PNG_)) {
          fn = new File(f.getParent(), REMOVE__DUP__PNG_ + f.getName());
          renamed = f.renameTo(fn);
        }
      }
      System.out.printf("%s\t(%d)\n", fn.toString(), sizeperfile.get(f));
    }
    if (renameDups) {

    }
  }

  public static void main(String[] args) {
    long t0 = System.currentTimeMillis();
    run(args);
    long t1 = System.currentTimeMillis();
    System.out.println((t1 - t0) + " msecs");
  }
}
