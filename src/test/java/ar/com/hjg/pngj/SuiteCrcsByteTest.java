package ar.com.hjg.pngj;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.zip.CRC32;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import ar.com.hjg.pngj.test.PngjTest;
import ar.com.hjg.pngj.test.TestSupport;

/**
 * Reads all (valid) PNG images from the test suite, loads as INT (unpacked) and computes a CRC of
 * all lines (bytes 0 and 1), comparing with precomputed
 */
public class SuiteCrcsByteTest extends PngjTest {

  LinkedHashMap<String, Long> crcs;// these were computed with old PNJG

  public SuiteCrcsByteTest() {
    init();
  }

  @Test
  public void testcrcs() {
    LinkedHashMap<String, Long> bad = new LinkedHashMap<String, Long>();
    int errs = 0;
    int ok = 0;
    for (String file : crcs.keySet()) {
      try {
        long res = calcCrc(file);
        long correct = crcs.get(file);
        if (res != correct) {
          errs++;
          bad.put(file, res);
          if (errs > 10)
            break;
        } else {
          ok++;
        }
      } catch (Exception e) {
        PngHelperInternal.LOGGER.severe("exception with " + file + ": " + e.getMessage());
      }
    }
    TestCase.assertEquals("bad crcs:" + bad.toString(), 0, errs);
  }

  protected long calcCrc(String file) {
    File f = new File(TestSupport.getPngTestSuiteDir(), file);
    PngReaderByte png = new PngReaderByte(f);
    CRC32 crc = new CRC32();
    for (int i = 0; i < png.imgInfo.rows; i++) {
      ImageLineByte line = (ImageLineByte) png.readRow(i);
      for (int j = 0; j < line.getSize(); j++) {
        int x = line.getScanline()[j];
        crc.update(x);
      }
    }
    png.end();
    return crc.getValue();
  }

  protected void init() {
    crcs = new LinkedHashMap<String, Long>();
    crcs.put("basi0g01.png", Long.valueOf(638439777L));
    crcs.put("basi0g02.png", Long.valueOf(657548584L));
    crcs.put("basi0g04.png", Long.valueOf(1464587703L));
    crcs.put("basi0g08.png", Long.valueOf(2018200142L));
    crcs.put("basi0g16.png", Long.valueOf(101827364L));
    crcs.put("basi2c08.png", Long.valueOf(2018884031L));
    crcs.put("basi2c16.png", Long.valueOf(4291567313L));
    crcs.put("basi3p01.png", Long.valueOf(4221820031L));
    crcs.put("basi3p02.png", Long.valueOf(2367188966L));
    crcs.put("basi3p04.png", Long.valueOf(672045553L));
    crcs.put("basi3p08.png", Long.valueOf(4142762662L));
    crcs.put("basi4a08.png", Long.valueOf(2960547948L));
    crcs.put("basi4a16.png", Long.valueOf(1352835972L));
    crcs.put("basi6a08.png", Long.valueOf(2806903596L));
    crcs.put("basi6a16.png", Long.valueOf(677111136L));
    crcs.put("basn0g01.png", Long.valueOf(638439777L));
    crcs.put("basn0g02.png", Long.valueOf(657548584L));
    crcs.put("basn0g04.png", Long.valueOf(1464587703L));
    crcs.put("basn0g08.png", Long.valueOf(2018200142L));
    crcs.put("basn0g16.png", Long.valueOf(101827364L));
    crcs.put("basn2c08.png", Long.valueOf(2018884031L));
    crcs.put("basn2c16.png", Long.valueOf(4291567313L));
    crcs.put("basn3p01.png", Long.valueOf(4221820031L));
    crcs.put("basn3p02.png", Long.valueOf(2367188966L));
    crcs.put("basn3p04.png", Long.valueOf(672045553L));
    crcs.put("basn3p08.png", Long.valueOf(4142762662L));
    crcs.put("basn4a08.png", Long.valueOf(2960547948L));
    crcs.put("basn4a16.png", Long.valueOf(1352835972L));
    crcs.put("basn6a08.png", Long.valueOf(2806903596L));
    crcs.put("basn6a16.png", Long.valueOf(677111136L));
    crcs.put("bgai4a08.png", Long.valueOf(2960547948L));
    crcs.put("bgai4a16.png", Long.valueOf(1352835972L));
    crcs.put("bgan6a08.png", Long.valueOf(2806903596L));
    crcs.put("bgan6a16.png", Long.valueOf(677111136L));
    crcs.put("bgbn4a08.png", Long.valueOf(2960547948L));
    crcs.put("bggn4a16.png", Long.valueOf(1352835972L));
    crcs.put("bgwn6a08.png", Long.valueOf(2806903596L));
    crcs.put("bgyn6a16.png", Long.valueOf(677111136L));
    crcs.put("ccwn2c08.png", Long.valueOf(1639358094L));
    crcs.put("ccwn3p08.png", Long.valueOf(519007828L));
    crcs.put("cdfn2c08.png", Long.valueOf(2578399395L));
    crcs.put("cdhn2c08.png", Long.valueOf(2225401664L));
    crcs.put("cdsn2c08.png", Long.valueOf(2192731567L));
    crcs.put("cdun2c08.png", Long.valueOf(3998278602L));
    crcs.put("ch1n3p04.png", Long.valueOf(672045553L));
    crcs.put("ch2n3p08.png", Long.valueOf(4142762662L));
    crcs.put("cm0n0g04.png", Long.valueOf(1865618197L));
    crcs.put("cm7n0g04.png", Long.valueOf(1865618197L));
    crcs.put("cm9n0g04.png", Long.valueOf(1865618197L));
    crcs.put("cs3n2c16.png", Long.valueOf(1929786652L));
    crcs.put("cs3n3p08.png", Long.valueOf(1361754892L));
    crcs.put("cs5n2c08.png", Long.valueOf(454480233L));
    crcs.put("cs5n3p08.png", Long.valueOf(2896231495L));
    crcs.put("cs8n2c08.png", Long.valueOf(1929786652L));
    crcs.put("cs8n3p08.png", Long.valueOf(18831189L));
    crcs.put("ct0n0g04.png", Long.valueOf(1865618197L));
    crcs.put("ct1n0g04.png", Long.valueOf(1865618197L));
    crcs.put("cten0g04.png", Long.valueOf(47982921L));
    crcs.put("ctfn0g04.png", Long.valueOf(263585808L));
    crcs.put("ctgn0g04.png", Long.valueOf(404114289L));
    crcs.put("cthn0g04.png", Long.valueOf(2657543270L));
    crcs.put("ctjn0g04.png", Long.valueOf(2185308513L));
    crcs.put("ctzn0g04.png", Long.valueOf(1865618197L));
    crcs.put("f00n0g08.png", Long.valueOf(521676383L));
    crcs.put("f00n2c08.png", Long.valueOf(1058891437L));
    crcs.put("f01n0g08.png", Long.valueOf(409477503L));
    crcs.put("f01n2c08.png", Long.valueOf(297906814L));
    crcs.put("f02n0g08.png", Long.valueOf(2042218974L));
    crcs.put("f02n2c08.png", Long.valueOf(2132584325L));
    crcs.put("f03n0g08.png", Long.valueOf(2742273604L));
    crcs.put("f03n2c08.png", Long.valueOf(828661129L));
    crcs.put("f04n0g08.png", Long.valueOf(3087032872L));
    crcs.put("f04n2c08.png", Long.valueOf(1996843631L));
    crcs.put("f99n0g04.png", Long.valueOf(31400461L));
    crcs.put("g03n0g16.png", Long.valueOf(786225710L));
    crcs.put("g03n2c08.png", Long.valueOf(909312543L));
    crcs.put("g03n3p04.png", Long.valueOf(1885837076L));
    crcs.put("g04n0g16.png", Long.valueOf(1657700307L));
    crcs.put("g04n2c08.png", Long.valueOf(2927775506L));
    crcs.put("g04n3p04.png", Long.valueOf(3087039771L));
    crcs.put("g05n0g16.png", Long.valueOf(4267405292L));
    crcs.put("g05n2c08.png", Long.valueOf(1008110461L));
    crcs.put("g05n3p04.png", Long.valueOf(971297531L));
    crcs.put("g07n0g16.png", Long.valueOf(3805335016L));
    crcs.put("g07n2c08.png", Long.valueOf(1838909555L));
    crcs.put("g07n3p04.png", Long.valueOf(531282976L));
    crcs.put("g10n0g16.png", Long.valueOf(1679108422L));
    crcs.put("g10n2c08.png", Long.valueOf(4195624999L));
    crcs.put("g10n3p04.png", Long.valueOf(2600520802L));
    crcs.put("g25n0g16.png", Long.valueOf(925915624L));
    crcs.put("g25n2c08.png", Long.valueOf(3851501873L));
    crcs.put("g25n3p04.png", Long.valueOf(1079986293L));
    crcs.put("oi1n0g16.png", Long.valueOf(101827364L));
    crcs.put("oi1n2c16.png", Long.valueOf(4291567313L));
    crcs.put("oi2n0g16.png", Long.valueOf(101827364L));
    crcs.put("oi2n2c16.png", Long.valueOf(4291567313L));
    crcs.put("oi4n0g16.png", Long.valueOf(101827364L));
    crcs.put("oi4n2c16.png", Long.valueOf(4291567313L));
    crcs.put("oi9n0g16.png", Long.valueOf(101827364L));
    crcs.put("oi9n2c16.png", Long.valueOf(4291567313L));
    crcs.put("PngSuite.png", Long.valueOf(4071145679L));
    crcs.put("pp0n2c16.png", Long.valueOf(4291567313L));
    crcs.put("pp0n6a08.png", Long.valueOf(249584737L));
    crcs.put("ps1n0g08.png", Long.valueOf(2018200142L));
    crcs.put("ps1n2c16.png", Long.valueOf(4291567313L));
    crcs.put("ps2n0g08.png", Long.valueOf(2018200142L));
    crcs.put("ps2n2c16.png", Long.valueOf(4291567313L));
    crcs.put("s01i3p01.png", Long.valueOf(3523407757L));
    crcs.put("s01n3p01.png", Long.valueOf(3523407757L));
    crcs.put("s02i3p01.png", Long.valueOf(558161692L));
    crcs.put("s02n3p01.png", Long.valueOf(558161692L));
    crcs.put("s03i3p01.png", Long.valueOf(3681107230L));
    crcs.put("s03n3p01.png", Long.valueOf(3681107230L));
    crcs.put("s04i3p01.png", Long.valueOf(298271832L));
    crcs.put("s04n3p01.png", Long.valueOf(298271832L));
    crcs.put("s05i3p02.png", Long.valueOf(3091650273L));
    crcs.put("s05n3p02.png", Long.valueOf(3091650273L));
    crcs.put("s06i3p02.png", Long.valueOf(1027227604L));
    crcs.put("s06n3p02.png", Long.valueOf(1027227604L));
    crcs.put("s07i3p02.png", Long.valueOf(316580556L));
    crcs.put("s07n3p02.png", Long.valueOf(316580556L));
    crcs.put("s08i3p02.png", Long.valueOf(1723406241L));
    crcs.put("s08n3p02.png", Long.valueOf(1723406241L));
    crcs.put("s09i3p02.png", Long.valueOf(947415988L));
    crcs.put("s09n3p02.png", Long.valueOf(947415988L));
    crcs.put("s32i3p04.png", Long.valueOf(1290239135L));
    crcs.put("s32n3p04.png", Long.valueOf(1290239135L));
    crcs.put("s33i3p04.png", Long.valueOf(1896936080L));
    crcs.put("s33n3p04.png", Long.valueOf(1896936080L));
    crcs.put("s34i3p04.png", Long.valueOf(3517918178L));
    crcs.put("s34n3p04.png", Long.valueOf(3517918178L));
    crcs.put("s35i3p04.png", Long.valueOf(3585191218L));
    crcs.put("s35n3p04.png", Long.valueOf(3585191218L));
    crcs.put("s36i3p04.png", Long.valueOf(3482711170L));
    crcs.put("s36n3p04.png", Long.valueOf(3482711170L));
    crcs.put("s37i3p04.png", Long.valueOf(198056743L));
    crcs.put("s37n3p04.png", Long.valueOf(198056743L));
    crcs.put("s38i3p04.png", Long.valueOf(2729214834L));
    crcs.put("s38n3p04.png", Long.valueOf(2729214834L));
    crcs.put("s39i3p04.png", Long.valueOf(2270821299L));
    crcs.put("s39n3p04.png", Long.valueOf(2270821299L));
    crcs.put("s40i3p04.png", Long.valueOf(1834410701L));
    crcs.put("s40n3p04.png", Long.valueOf(1834410701L));
    crcs.put("tbbn0g04.png", Long.valueOf(3759719923L));
    crcs.put("tbbn2c16.png", Long.valueOf(3843674096L));
    crcs.put("tbbn3p08.png", Long.valueOf(554813364L));
    crcs.put("tbgn2c16.png", Long.valueOf(3843674096L));
    crcs.put("tbgn3p08.png", Long.valueOf(554813364L));
    crcs.put("tbrn2c08.png", Long.valueOf(3843674096L));
    crcs.put("tbwn0g16.png", Long.valueOf(1535967669L));
    crcs.put("tbwn3p08.png", Long.valueOf(554813364L));
    crcs.put("tbyn3p08.png", Long.valueOf(554813364L));
    crcs.put("tp0n0g08.png", Long.valueOf(3286082606L));
    crcs.put("tp0n2c08.png", Long.valueOf(3022435152L));
    crcs.put("tp0n3p08.png", Long.valueOf(3796036490L));
    crcs.put("tp1n3p08.png", Long.valueOf(554813364L));
    crcs.put("z00n2c08.png", Long.valueOf(4176991825L));
    crcs.put("z03n2c08.png", Long.valueOf(4176991825L));
    crcs.put("z06n2c08.png", Long.valueOf(4176991825L));
    crcs.put("z09n2c08.png", Long.valueOf(4176991825L));

  }

  @Before
  public void setUp() {}

  public static void main(String[] args) {
    String filename = "basi0g01.png";
    SuiteCrcsByteTest tc = new SuiteCrcsByteTest();
    long res = tc.calcCrc(filename);
    long crc0 = tc.crcs.get(filename);
    TestCase.assertEquals("bad crc for " + filename, res, crc0);
    System.out.println("ok");
  }
}
