package ar.com.hjg.pngj.test;

import org.junit.After;
import org.junit.BeforeClass;

public class PngjTest {

    /** change to false if you want to inspect the temporary files */
    protected boolean clearTempFiles = true;

    @BeforeClass
    public static void setup() {
        System.setProperty("java.util.logging.config.file", ClassLoader.getSystemResource("logging.properties").getPath());
    }
    
    @After
    public void tearDown() {
	if (clearTempFiles) {
	    TestSupport.cleanAll();
	}
    }
}
