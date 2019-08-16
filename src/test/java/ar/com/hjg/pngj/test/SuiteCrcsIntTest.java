package ar.com.hjg.pngj.test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngReader;

/**
 * Reads all (valid) PNG images from the test suite, loads as INT (unpacked) and
 * computes a CRC of all lines (bytes 0 and 1), comparing with precomputed
 */
public class SuiteCrcsIntTest extends PngjTest {
    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    LinkedHashMap<String, Long> crcs;// these were computed with old PNJG

    public SuiteCrcsIntTest() {
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
		LOGGER.severe("exception with " + file + ": " + e.getMessage());
	    }
	}
	TestCase.assertEquals("bad crcs:" + bad.toString(), 0, errs);

    }

    protected long calcCrc(String file) {
	File f = new File(TestSupport.getPngTestSuiteDir(), file);
	PngReader png = new PngReader(f);
	CRC32 crc = new CRC32();
	for (int i = 0; i < png.imgInfo.rows; i++) {
	    ImageLineInt line = (ImageLineInt) png.readRow(i);
	    for (int j = 0; j < line.getSize(); j++) {
		int x = line.getElem(j);
		crc.update(x);
		x >>= 8;
		crc.update(x);
	    }
	}
	png.end();
	return crc.getValue();
    }

    protected void init() {
	crcs = new LinkedHashMap<String, Long>();
	// these were computed with old PNJG
	crcs.put("basi0g01.png", Long.valueOf(385240647L));
	crcs.put("basi0g02.png", Long.valueOf(117051011L));
	crcs.put("basi0g04.png", Long.valueOf(3411899179L));
	crcs.put("basi0g08.png", Long.valueOf(1558158563L));
	crcs.put("basi0g16.png", Long.valueOf(716263731L));
	crcs.put("basi2c08.png", Long.valueOf(2378745398L));
	crcs.put("basi2c16.png", Long.valueOf(2395397753L));
	crcs.put("basi3p01.png", Long.valueOf(685967487L));
	crcs.put("basi3p02.png", Long.valueOf(2980053571L));
	crcs.put("basi3p04.png", Long.valueOf(42091252L));
	crcs.put("basi3p08.png", Long.valueOf(4147469058L));
	crcs.put("basi4a08.png", Long.valueOf(3749889517L));
	crcs.put("basi4a16.png", Long.valueOf(1310642363L));
	crcs.put("basi6a08.png", Long.valueOf(2530735596L));
	crcs.put("basi6a16.png", Long.valueOf(142337329L));
	crcs.put("basn0g01.png", Long.valueOf(385240647L));
	crcs.put("basn0g02.png", Long.valueOf(117051011L));
	crcs.put("basn0g04.png", Long.valueOf(3411899179L));
	crcs.put("basn0g08.png", Long.valueOf(1558158563L));
	crcs.put("basn0g16.png", Long.valueOf(716263731L));
	crcs.put("basn2c08.png", Long.valueOf(2378745398L));
	crcs.put("basn2c16.png", Long.valueOf(2395397753L));
	crcs.put("basn3p01.png", Long.valueOf(685967487L));
	crcs.put("basn3p02.png", Long.valueOf(2980053571L));
	crcs.put("basn3p04.png", Long.valueOf(42091252L));
	crcs.put("basn3p08.png", Long.valueOf(4147469058L));
	crcs.put("basn4a08.png", Long.valueOf(3749889517L));
	crcs.put("basn4a16.png", Long.valueOf(1310642363L));
	crcs.put("basn6a08.png", Long.valueOf(2530735596L));
	crcs.put("basn6a16.png", Long.valueOf(142337329L));
	crcs.put("bgai4a08.png", Long.valueOf(3749889517L));
	crcs.put("bgai4a16.png", Long.valueOf(1310642363L));
	crcs.put("bgan6a08.png", Long.valueOf(2530735596L));
	crcs.put("bgan6a16.png", Long.valueOf(142337329L));
	crcs.put("bgbn4a08.png", Long.valueOf(3749889517L));
	crcs.put("bggn4a16.png", Long.valueOf(1310642363L));
	crcs.put("bgwn6a08.png", Long.valueOf(2530735596L));
	crcs.put("bgyn6a16.png", Long.valueOf(142337329L));
	crcs.put("ccwn2c08.png", Long.valueOf(2749952221L));
	crcs.put("ccwn3p08.png", Long.valueOf(3232422439L));
	crcs.put("cdfn2c08.png", Long.valueOf(1004784475L));
	crcs.put("cdhn2c08.png", Long.valueOf(3767596225L));
	crcs.put("cdsn2c08.png", Long.valueOf(1165166485L));
	crcs.put("cdun2c08.png", Long.valueOf(1358040891L));
	crcs.put("ch1n3p04.png", Long.valueOf(42091252L));
	crcs.put("ch2n3p08.png", Long.valueOf(4147469058L));
	crcs.put("cm0n0g04.png", Long.valueOf(239744072L));
	crcs.put("cm7n0g04.png", Long.valueOf(239744072L));
	crcs.put("cm9n0g04.png", Long.valueOf(239744072L));
	crcs.put("cs3n2c16.png", Long.valueOf(2031730372L));
	crcs.put("cs3n3p08.png", Long.valueOf(2603498434L));
	crcs.put("cs5n2c08.png", Long.valueOf(3591461903L));
	crcs.put("cs5n3p08.png", Long.valueOf(4185835007L));
	crcs.put("cs8n2c08.png", Long.valueOf(127361955L));
	crcs.put("cs8n3p08.png", Long.valueOf(1899206989L));
	crcs.put("ct0n0g04.png", Long.valueOf(239744072L));
	crcs.put("ct1n0g04.png", Long.valueOf(239744072L));
	crcs.put("cten0g04.png", Long.valueOf(2485959390L));
	crcs.put("ctfn0g04.png", Long.valueOf(2144806951L));
	crcs.put("ctgn0g04.png", Long.valueOf(4145660843L));
	crcs.put("cthn0g04.png", Long.valueOf(735065130L));
	crcs.put("ctjn0g04.png", Long.valueOf(4127411432L));
	crcs.put("ctzn0g04.png", Long.valueOf(239744072L));
	crcs.put("f00n0g08.png", Long.valueOf(2120354897L));
	crcs.put("f00n2c08.png", Long.valueOf(388769213L));
	crcs.put("f01n0g08.png", Long.valueOf(4095017386L));
	crcs.put("f01n2c08.png", Long.valueOf(3908657445L));
	crcs.put("f02n0g08.png", Long.valueOf(3465606629L));
	crcs.put("f02n2c08.png", Long.valueOf(2103042764L));
	crcs.put("f03n0g08.png", Long.valueOf(2044373410L));
	crcs.put("f03n2c08.png", Long.valueOf(2127122668L));
	crcs.put("f04n0g08.png", Long.valueOf(1878881775L));
	crcs.put("f04n2c08.png", Long.valueOf(789320001L));
	crcs.put("f99n0g04.png", Long.valueOf(149645259L));
	crcs.put("g03n0g16.png", Long.valueOf(473707466L));
	crcs.put("g03n2c08.png", Long.valueOf(4133284128L));
	crcs.put("g03n3p04.png", Long.valueOf(1158497212L));
	crcs.put("g04n0g16.png", Long.valueOf(1061496161L));
	crcs.put("g04n2c08.png", Long.valueOf(3887021429L));
	crcs.put("g04n3p04.png", Long.valueOf(1576095991L));
	crcs.put("g05n0g16.png", Long.valueOf(3735522154L));
	crcs.put("g05n2c08.png", Long.valueOf(3152630461L));
	crcs.put("g05n3p04.png", Long.valueOf(907804064L));
	crcs.put("g07n0g16.png", Long.valueOf(528485053L));
	crcs.put("g07n2c08.png", Long.valueOf(1176251103L));
	crcs.put("g07n3p04.png", Long.valueOf(800464339L));
	crcs.put("g10n0g16.png", Long.valueOf(3347570312L));
	crcs.put("g10n2c08.png", Long.valueOf(115288574L));
	crcs.put("g10n3p04.png", Long.valueOf(3822507246L));
	crcs.put("g25n0g16.png", Long.valueOf(1442058880L));
	crcs.put("g25n2c08.png", Long.valueOf(3404091835L));
	crcs.put("g25n3p04.png", Long.valueOf(3766988542L));
	crcs.put("oi1n0g16.png", Long.valueOf(716263731L));
	crcs.put("oi1n2c16.png", Long.valueOf(2395397753L));
	crcs.put("oi2n0g16.png", Long.valueOf(716263731L));
	crcs.put("oi2n2c16.png", Long.valueOf(2395397753L));
	crcs.put("oi4n0g16.png", Long.valueOf(716263731L));
	crcs.put("oi4n2c16.png", Long.valueOf(2395397753L));
	crcs.put("oi9n0g16.png", Long.valueOf(716263731L));
	crcs.put("oi9n2c16.png", Long.valueOf(2395397753L));
	crcs.put("PngSuite.png", Long.valueOf(3046406988L));
	crcs.put("pp0n2c16.png", Long.valueOf(2395397753L));
	crcs.put("pp0n6a08.png", Long.valueOf(29814635L));
	crcs.put("ps1n0g08.png", Long.valueOf(1558158563L));
	crcs.put("ps1n2c16.png", Long.valueOf(2395397753L));
	crcs.put("ps2n0g08.png", Long.valueOf(1558158563L));
	crcs.put("ps2n2c16.png", Long.valueOf(2395397753L));
	crcs.put("s01i3p01.png", Long.valueOf(1104745215L));
	crcs.put("s01n3p01.png", Long.valueOf(1104745215L));
	crcs.put("s02i3p01.png", Long.valueOf(1696784233L));
	crcs.put("s02n3p01.png", Long.valueOf(1696784233L));
	crcs.put("s03i3p01.png", Long.valueOf(2295964787L));
	crcs.put("s03n3p01.png", Long.valueOf(2295964787L));
	crcs.put("s04i3p01.png", Long.valueOf(3147056371L));
	crcs.put("s04n3p01.png", Long.valueOf(3147056371L));
	crcs.put("s05i3p02.png", Long.valueOf(3147295260L));
	crcs.put("s05n3p02.png", Long.valueOf(3147295260L));
	crcs.put("s06i3p02.png", Long.valueOf(1832758033L));
	crcs.put("s06n3p02.png", Long.valueOf(1832758033L));
	crcs.put("s07i3p02.png", Long.valueOf(2391917463L));
	crcs.put("s07n3p02.png", Long.valueOf(2391917463L));
	crcs.put("s08i3p02.png", Long.valueOf(1940116363L));
	crcs.put("s08n3p02.png", Long.valueOf(1940116363L));
	crcs.put("s09i3p02.png", Long.valueOf(641071288L));
	crcs.put("s09n3p02.png", Long.valueOf(641071288L));
	crcs.put("s32i3p04.png", Long.valueOf(1178170563L));
	crcs.put("s32n3p04.png", Long.valueOf(1178170563L));
	crcs.put("s33i3p04.png", Long.valueOf(2797147378L));
	crcs.put("s33n3p04.png", Long.valueOf(2797147378L));
	crcs.put("s34i3p04.png", Long.valueOf(549270401L));
	crcs.put("s34n3p04.png", Long.valueOf(549270401L));
	crcs.put("s35i3p04.png", Long.valueOf(2708301864L));
	crcs.put("s35n3p04.png", Long.valueOf(2708301864L));
	crcs.put("s36i3p04.png", Long.valueOf(3803983580L));
	crcs.put("s36n3p04.png", Long.valueOf(3803983580L));
	crcs.put("s37i3p04.png", Long.valueOf(2384076453L));
	crcs.put("s37n3p04.png", Long.valueOf(2384076453L));
	crcs.put("s38i3p04.png", Long.valueOf(2104405411L));
	crcs.put("s38n3p04.png", Long.valueOf(2104405411L));
	crcs.put("s39i3p04.png", Long.valueOf(3397979578L));
	crcs.put("s39n3p04.png", Long.valueOf(3397979578L));
	crcs.put("s40i3p04.png", Long.valueOf(1512187790L));
	crcs.put("s40n3p04.png", Long.valueOf(1512187790L));
	crcs.put("tbbn0g04.png", Long.valueOf(2380849062L));
	crcs.put("tbbn2c16.png", Long.valueOf(675079740L));
	crcs.put("tbbn3p08.png", Long.valueOf(3558460409L));
	crcs.put("tbgn2c16.png", Long.valueOf(675079740L));
	crcs.put("tbgn3p08.png", Long.valueOf(3558460409L));
	crcs.put("tbrn2c08.png", Long.valueOf(762241843L));
	crcs.put("tbwn0g16.png", Long.valueOf(1577244699L));
	crcs.put("tbwn3p08.png", Long.valueOf(3558460409L));
	crcs.put("tbyn3p08.png", Long.valueOf(3558460409L));
	crcs.put("tp0n0g08.png", Long.valueOf(3617951865L));
	crcs.put("tp0n2c08.png", Long.valueOf(1385374186L));
	crcs.put("tp0n3p08.png", Long.valueOf(3791776703L));
	crcs.put("tp1n3p08.png", Long.valueOf(3558460409L));
	crcs.put("z00n2c08.png", Long.valueOf(2166536248L));
	crcs.put("z03n2c08.png", Long.valueOf(2166536248L));
	crcs.put("z06n2c08.png", Long.valueOf(2166536248L));
	crcs.put("z09n2c08.png", Long.valueOf(2166536248L));

    }

    @Before
    public void setUp() {
	init();
    }

    public static void main(String[] args) {
	String filename = "basi0g01.png";
	SuiteCrcsIntTest tc = new SuiteCrcsIntTest();
	long res = tc.calcCrc(filename);
	long crc0 = tc.crcs.get(filename);
	TestCase.assertEquals("bad crc for " + filename, res, crc0);
	System.out.println("ok");
    }

}
