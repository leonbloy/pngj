package ar.com.hjg.pngj.cli;

import java.io.File;
import java.util.List;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngReaderByte;

/** see help */
public class ShowPngInfo {

	static StringBuilder help = new StringBuilder();

	public static void run(String[] args) {

		help.append("Shows some info and/or digest for PNG file.     \n");
		help.append("  Columns are tab separated, in the order: basic_info raw_pix_size compress_size digest   \n");
		help.append("  (if bad or not png image, as message starting with '" + BAD_OR_NOT_PNG + "' is shown)   \n");
		help.append("  Options:   \n");
		help.append("    -i: basic image info as compact string \n");
		help.append("    -s: size raw pixels \n");
		help.append("    -c: compressed idat size \n");
		help.append("    -d: digest of full raw pixel data and palette    \n");
		help.append("  Accepts paths in the form 'mypath/*' (all pngs in dir) or 'mypath/**' (idem recursive)  \n");

		CliArgs cli = CliArgs.buildFrom(args, help);
		cli.checkAtLeastNargs(1);
		ShowPngInfo me = new ShowPngInfo();
		me.showCompatInfo = cli.hasOpt("i");
		me.showFullPixelsDigest = cli.hasOpt("d");
		me.showRawPixelSize = cli.hasOpt("s");
		me.showIdatCompressedSize = cli.hasOpt("c");
		me.listpng = cli.listPngsFromArgs();
		cli.checkNoMoreOpts();
		me.doit();
	}

	public static final String BAD_OR_NOT_PNG = "BAD_OR_NOT_PNG_FILE";

	private List<File> listpng;
	private boolean showCompatInfo = false;
	private boolean showFullPixelsDigest;
	private boolean showRawPixelSize;
	private boolean showIdatCompressedSize;

	private ShowPngInfo() {
	}

	private void doit() {
		for (File f : listpng)
			doitForFile(f);
	}

	@SuppressWarnings("unused")
	private void doitForFile(File f) {
		System.out.printf("%s\t", f.toString());
		try {
			PngReaderByte png = new PngReaderByte(f);
			png.prepareSimpleDigestComputation();
			if (showIdatCompressedSize || showFullPixelsDigest) {
				IImageLine iline;
				while (png.hasMoreRows()) 
					iline = png.readRow(); 
			}
			png.close();//abort
			if (showCompatInfo)
				System.out.printf("%s\t", png.toStringCompact());
			if (showRawPixelSize)
				System.out.printf("%d\t", png.imgInfo.getTotalRawBytes());
			if (showIdatCompressedSize)
				System.out.printf("%d\t", png.getChunkseq().getIdatBytes());
			if (showFullPixelsDigest)
				System.out.printf("%s\t", png.getSimpleDigestHex());
		} catch (Exception e) {
			System.out.printf("%s: %s\t", BAD_OR_NOT_PNG, e.getMessage());
		}
		System.out.println("");

	}

	
	public static void main(String[] args) {
		run(args);
	}
}
