package ar.com.hjg.pngj.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// very simple cli parser - because we all know the wheel has not yet been invented -
// http://jewelcli.lexicalscope.com/related.html */
// it supports special pseudo args : 'path/*' (all png files in directory) and 'path/**' idem recursive
public class CliArgs {
  private Map<String, String> opts = new LinkedHashMap<String, String>(); // key: opt (single char no dash)
  private Set<String> unCheckedOpts = new HashSet<String>();
  private List<String> argslist = new ArrayList<String>();
  private boolean allfiles = false;
  private boolean allfilesRecursive = false;
  private CharSequence help;

  public static CliArgs buildFrom(String[] args, CharSequence help) {
    CliArgs c = new CliArgs();
    c.help = help;
    try {
      c.parse(args);
    } catch (Exception e) {
      System.err.println("Bad args - " + e.getMessage());
      System.err.println(help);
      System.exit(1);
    }
    if (c.isHelp()) {
      System.err.println(help);
      System.exit(0);
    }
    return c;
  }

  public void checkNoMoreOpts() {
    if (!unCheckedOpts.isEmpty()) {
      System.err.print("Unexpected option passed: (");
      for (String o : unCheckedOpts)
        System.err.print(" -" + o);
      System.err.print(" )\n");
      System.err.println(help);
      System.exit(1);
    }
  }

  public void checkAtLeastNargs(int n) {
    if (argslist.size() < n) {
      System.err.println("Not enough arguments (" + n + " expected)");
      System.err.println(help);
      System.exit(1);
    }
  }

  public void badUsageAbort(String msg) {
    System.err.printf("Error in invocation: %s\n", msg);
    System.err.println(help);
    System.exit(1);
  }

  public String getOpt(String k) {
    return getOpt(k, null);
  }

  public String getOpt(String k, String def) {
    unCheckedOpts.remove(k);
    return opts.containsKey(k) ? opts.get(k) : def;

  }

  public boolean hasOpt(String k) {
    unCheckedOpts.remove(k);
    return opts.containsKey(k);
  }

  private static List<File> listPng(File path) {
    String name = path.getName();
    if (path.isDirectory())
      return listPngFromDir(path, false);
    else if (name.equals("*"))
      return listPngFromDir(path.getParentFile(), false);
    else if (name.equals("**"))
      return listPngFromDir(path.getParentFile(), true);
    ArrayList<File> l = new ArrayList<File>();
    l.add(path);
    return l;
  }

  private static List<File> listPngFromDir(File dir, boolean recurse) {
    ArrayList<File> pngs = new ArrayList<File>();
    for (File f : dir.listFiles()) {
      if (f.isFile() && f.getName().toLowerCase().endsWith(".png"))
        pngs.add(f);
      if (f.isDirectory() && recurse) {
        pngs.addAll(listPngFromDir(f, true));
      }
    }
    return pngs;
  }

  protected void parse(String[] args) {
    boolean parsingOpts = true;
    for (String arg : args) {
      if (arg.equals("--") && parsingOpts) {
        parsingOpts = false;
        continue;
      }
      if (arg.startsWith("-") && parsingOpts) {
        String opt = arg.substring(1, 2);
        if (opt.equals("-"))
          throw new RuntimeException("double dash not accepted, use single dash");
        String val = arg.length() > 2 ? arg.substring(2) : "";
        opts.put(opt, val);
      } else {
        parsingOpts = false;
        if (arg.equals("*")) {
          if (allfiles || allfilesRecursive || argslist.size() > 0)
            throw new RuntimeException("bad syntax");
          allfiles = true;
        } else if (arg.equals("**/*")) {
          if (allfiles || allfilesRecursive || argslist.size() > 0)
            throw new RuntimeException("bad syntax");
          allfiles = true;
          allfilesRecursive = true;
        } else {
          if (allfiles || allfilesRecursive)
            throw new RuntimeException("bad syntax");
          argslist.add(arg);
        }
      }
    }
    unCheckedOpts.addAll(opts.keySet());
  }

  public List<String> getArgslist() {
    return argslist;
  }

  public int nArgs() {
    return argslist.size();
  }

  public boolean isHelp() {
    return opts.containsKey("h") || opts.containsKey("?");
  }

  /** only for the cases where the args are a flat list of png files */
  public List<File> listPngsFromArgs() {
    ArrayList<File> l = new ArrayList<File>();
    for (String arg : argslist)
      l.addAll(listPng(new File(arg)));
    return l;
  }

}
