package ar.com.hjg.pngj.test;

import org.junit.After;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PngjTest {

	/** change to false if you want to inspect the temporary files */
	protected boolean clearTempFiles = true;

	@BeforeClass
	public static void setup() {
		//testLogger();
	}

	private static void testLogger() {
		Logger log = LoggerFactory.getLogger(PngjTest.class.getName());
		log.error("Testing log ERROR");
		log.warn("Testing log WARN");
		log.info("Testing log INFO");
		log.debug("Testing log DEBUG");
		log.trace("Testing log TRACE");
	}

	@After
	public void tearDown() {
		if (clearTempFiles) {
			TestSupport.cleanAll();
		}
	}
}
