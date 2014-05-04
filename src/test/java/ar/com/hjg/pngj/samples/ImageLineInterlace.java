package ar.com.hjg.pngj.samples;

public class ImageLineInterlace {

  /*
   * public static void testPackInt() { Random r = new Random(); for (int n = 0; n < 50000; n++) {
   * int bitdepth = r.nextInt(3) + 1; if (bitdepth == 3) bitdepth = 4; boolean scale =
   * r.nextBoolean(); ImageLine im = new ImageLine(new ImageInfo(r.nextInt(37) + 1, 1, bitdepth,
   * false, true, false), SampleType.INT, true); int m =
   * ImageLineHelper.getMaskForPackedFormatsLs(bitdepth); for (int i = 0; i < im.elementsPerRow;
   * i++) im.scanline[i] = scale ? ((r.nextInt(256) & m) << (8 - bitdepth)) : (r.nextInt(256) & m);
   * int[] c = Arrays.copyOf(im.scanline, im.scanline.length); //
   * System.out.println(Arrays.toString(im.scanline)); ImageLine.packInplaceInt(im.imgInfo,
   * im.scanline, im.scanline, scale); // System.out.println(Arrays.toString(im.scanline));
   * ImageLine.unpackInplaceInt(im.imgInfo, im.scanline, im.scanline, scale); //
   * System.out.println(Arrays.toString(im.scanline)); if (!Arrays.equals(c, im.scanline)) { throw
   * new RuntimeException(Arrays.toString(c)); }
   * 
   * } System.out.println("Done"); }
   * 
   * public static void testPackByte() { Random r = new Random(); for (int n = 0; n < 50000; n++) {
   * int bitdepth = r.nextInt(3) + 1; if (bitdepth == 3) bitdepth = 4; boolean scale =
   * r.nextBoolean(); ImageLine im = new ImageLine(new ImageInfo(r.nextInt(37) + 1, 1, bitdepth,
   * false, true, false), SampleType.BYTE, true); int m =
   * ImageLineHelper.getMaskForPackedFormatsLs(bitdepth); for (int i = 0; i < im.elementsPerRow;
   * i++) im.scanlineb[i] = (byte) (scale ? ((r.nextInt(256) & m) << (8 - bitdepth)) :
   * (r.nextInt(256) & m)); byte[] c = Arrays.copyOf(im.scanlineb, im.scanlineb.length); //
   * System.out.println(Arrays.toString(im.scanlineb)); ImageLine.packInplaceByte(im.imgInfo,
   * im.scanlineb, im.scanlineb, scale); // System.out.println(Arrays.toString(im.scanlineb));
   * ImageLine.unpackInplaceByte(im.imgInfo, im.scanlineb, im.scanlineb, scale); //
   * System.out.println(Arrays.toString(im.scanlineb)); if (!Arrays.equals(c, im.scanlineb)) { throw
   * new RuntimeException(Arrays.toString(c)); }
   * 
   * } System.out.println("Done"); }
   */
}
