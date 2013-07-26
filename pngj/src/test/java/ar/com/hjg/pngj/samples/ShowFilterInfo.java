package ar.com.hjg.pngj.samples;

import java.io.File;

import ar.com.hjg.pngj.test.TestSupport;

/**
 * 
 */
public class ShowFilterInfo {

	public static void show(File file) {
		System.out.println(TestSupport.showFilters(file, 5, false));
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1)
			System.err.println("arg: PNG file");
		else
			show(new File(args[0]));
	}

}
