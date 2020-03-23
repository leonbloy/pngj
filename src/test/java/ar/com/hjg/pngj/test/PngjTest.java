package ar.com.hjg.pngj.test;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.BeforeClass;

public class PngjTest {

	/** change to false if you want to inspect the temporary files */
	protected boolean clearTempFiles = true;

	@BeforeClass
	public static void setup() {
		System.setProperty("java.util.logging.config.file",
				ClassLoader.getSystemResource("logging.properties").getPath());
		//testLogger();
	}

	private static void testLogger() {
		Logger log = Logger.getLogger(PngjTest.class.getName());
		log.severe("Testing log SEVERE");
		log.warning("Testing log WARN");
		log.info("Testing log INFO");
		log.fine("Testing log FINE");
	}

	@After
	public void tearDown() {
		if (clearTempFiles) {
			TestSupport.cleanAll();
		}
	}
}
