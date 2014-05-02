package ar.com.hjg.pngj.cli;

import java.io.File;
import java.util.List;

import ar.com.hjg.pngj.test.TestSupport;

/**
 * See help
 */
public class ShowFilterInfo {
	static StringBuilder help = new StringBuilder();

	public static void run(String[] args) {

		help.append(" Shows row filtering info for PNG files.   \n");
		help.append("  Options:   \n");
		help.append("    -n: use newlines \n");
		help.append("    -s: summary \n");
		help.append("    -sp: summary with percentajes \n");
		help.append("    -g[num]: maximum number of groups to show (0:none -1:all)   \n");
		help.append(" Accepts paths in the form 'mypath/*' (all pngs in dir) or 'mypath/**' (idem recursive)  \n");

		CliArgs cli = CliArgs.buildFrom(args, help);
		cli.checkAtLeastNargs(1);
		ShowFilterInfo me = new ShowFilterInfo();
		me.useNewlines = cli.hasOpt("n");
		me.withSummary = cli.hasOpt("s");
		me.withSummaryPercent = cli.getOpt("s", "").startsWith("p");
		me.maxGroupsToShow = Integer.parseInt(cli.getOpt("g", "5"));
		me.listpng = cli.listPngsFromArgs();
		cli.checkNoMoreOpts();
		if (!(me.withSummary || me.withSummaryPercent || me.maxGroupsToShow != 0))
			cli.badUsageAbort("You must either specify a summary or a non-zero group size");
		me.doit();
	}

	private List<File> listpng;
	private boolean useNewlines = false;
	private boolean withSummaryPercent = false;
	private boolean withSummary = false;
	private int maxGroupsToShow;

	private void doit() {
		for (File f : listpng)
			doitForFile(f);
	}

	public void doitForFile(File file) {
		System.out.printf("%s\t", file.toString());
		try {
			String info = TestSupport.showFilters(file, maxGroupsToShow, useNewlines, withSummary, withSummaryPercent);
			System.out.printf("%s", info);
		} catch (Exception e) {
			System.out.printf("%s : %s", ShowPngInfo.BAD_OR_NOT_PNG, e.getMessage());
		}
		System.out.println("");
	}

	public static void main(String[] args) {
		run(args);
	}

}
