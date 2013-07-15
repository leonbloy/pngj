package ar.com.hjg.pngj.test;

import org.junit.After;


public class PngjTest {

	/** change to false if you want to inspect the temporary files */
	protected boolean clearTempFiles = true;

	@After
	public void tearDown() {
		if (clearTempFiles) {
			TestSupport.cleanAll();
		}
	}
}
