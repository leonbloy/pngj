package ar.com.hjg.pngj.cli;

import java.io.File;
import java.util.List;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngReaderByte;

public class ShowPngInfo {

	static StringBuilder help = new StringBuilder();
	private List<File> listpng;
	private boolean showCompatInfo = false;
	private boolean showFullPixelsDigest;
	private boolean showRawPixelSize;
	private boolean showIdatCompressedSize;
	public static final String NOT_A_PNG = "BAD_OR_NOT_PNG_FILE";

	private ShowPngInfo() {
	}

	private void doit() {
		for (File f : listpng) {
				doitForFile(f);
		}
	}

	
	private String getCompactString(ImageInfo info) {
		return String.format("%s", info.toStringBrief());
	}

	

	@SuppressWarnings("unused")
	private void doitForFile(File f) {
		System.out.printf("%s\t", f.toString());
		try {
			PngReaderByte png = new PngReaderByte(f);
			png.prepareSimpleDigestComputation();
			if(showIdatCompressedSize || showFullPixelsDigest) {
				while(png.hasMoreRows()) {
					IImageLine iline = png.readRow(); // this is not quite correct for 16bpp
				}
			}  
			png.close();//abort
			if (showCompatInfo)
				System.out.printf("%s\t", getCompactString(png.imgInfo));
			if (showRawPixelSize)
				System.out.printf("%d\t", png.imgInfo.getTotalRawBytes());
			if (showIdatCompressedSize)
				System.out.printf("%d\t", png.getChunkseq().getIdatBytes());
			if (showFullPixelsDigest)
				System.out.printf("%s\t", png.getSimpleDigestHex());
		} catch (Exception e) {
			System.out.printf("%s: %s\t", NOT_A_PNG, e.getMessage());
		}
		System.out.println("");

	}

	static String bin2hex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(2 * bytes.length);
		for (byte b : bytes) {
		    sb.append("0123456789ABCDEF".charAt((b & 0xF0) >> 4));
		    sb.append("0123456789ABCDEF".charAt((b & 0x0F)));
		}
		return sb.toString();
	}
	
	public static void run(String[] args) {

		help.append("Shows some info and/or digest for PNG file.\n");
		help.append("  Columns are tab separated, in the order: basic_info raw_pix_size compress_size digest\n");
		help.append("  (if bad or not png image, as message starting with '" + NOT_A_PNG +"' is shown)\n");
		help.append("  Options:\n");
		help.append("    -i: basic image info as compact string \n");
		help.append("    -s: size raw pixels \n");
		help.append("    -c: compressed idat size \n");
		help.append("    -d: digest of full raw pixel data and palette \n");
		help.append("  Accepts paths in the form 'mypath/*' (all pngs in dir) or 'mypath/**' (idem recursive) \n");

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

	public static void main(String[] args) {
		run(args);
	}
}
