package ar.com.hjg.pngj.samples;

import java.io.File;

import ar.com.hjg.pngj.ImageInfo;

public class TestConvToPalette {

	public static void test() {
		String suffix = "crgb";
		ImageInfo imi = new ImageInfo(500, 300, 8, false);
		File f = TestsHelper.createWaves(suffix, 0.5, imi);
		File f2 = TestsHelper.getTmpFile(suffix + "pal");
		SampleConvertToPalette.convertPal(f, f2);
		System.out.println(f + "->" + f2);
	}

	public static void main(String[] args) {
		test();
	}

}
