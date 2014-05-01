package ar.com.hjg.pngj.cli;

import java.io.File;
import java.util.List;

import ar.com.hjg.pngj.test.TestSupport;

/**
 * 
 */
public class ShowFilterInfo {
	static StringBuilder help = new StringBuilder();
	private boolean usenewlines=false;
	private List<File> listpng;
	private boolean withSummaryPercent=false;
	private boolean withSummary=false;
	private int groups;

	private void doit() {
		for (File f : listpng) {
				doitForFile(f);
		}
	}

	
	public void doitForFile(File file) {
		System.out.printf("%s\t",file.toString());
		try {
			String info=TestSupport.showFilters(file, groups,usenewlines,withSummary,withSummaryPercent);
			System.out.printf("%s",info);
		} catch(Exception e) {
			System.out.printf("%s : %s",ShowPngInfo.NOT_A_PNG,e.getMessage());
		}
		System.out.println("");
		
	}

	
	public static void run(String[] args) {

		help.append("Shows row filtering info for PNG files.\n");
		help.append("  Options:\n");
		help.append("    -n: use newlines \n");
		help.append("    -s: summary \n");
		help.append("    -sp: summary with percentajes\n");
		help.append("    -g[num]: maximum number of groups to show (0:none -1:all)\n");
		help.append("  Accepts paths in the form 'mypath/*' (all pngs in dir) or 'mypath/**' (idem recursive) \n");

		CliArgs cli = CliArgs.buildFrom(args, help);
		cli.checkAtLeastNargs(1);
		ShowFilterInfo me = new ShowFilterInfo();
		me.usenewlines = cli.hasOpt("n");
		me.withSummary = cli.hasOpt("s");
		me.withSummaryPercent = cli.getOpt("s", "").startsWith("p");
		me.groups = Integer.parseInt(cli.getOpt("g", "5"));
		me.listpng = cli.listPngsFromArgs();
		cli.checkNoMoreOpts();
		if(!(me.withSummary || me.withSummaryPercent || me.groups!=0))
			cli.badUsageAbort("You must either specify a summary or a non-zero group size");
		me.doit();
	}

	public static void main(String[] args) {
		run(args);
	}

}
