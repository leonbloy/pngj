package ar.com.hjg.pngj.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * 
 * checks that the compiled classes are apt for sandboxed environment
 * 
 * http://code.google.com/appengine/docs/java/jrewhitelist.html
 * 
 * @author Hernan J Gonzalez
 * 
 */
public class WhiteListedCheck {

	private static final String WHITELIST_FILENAME = "whitelistedclasses.txt";
	private Set<String> whiteList;
	private boolean VERBOSE = false;

	/* returns ERR TOTAL */
	public int[] checkDir(File dir, boolean recurse) {
		//System.out.println("checking " + dir);
		List<File> files = FindDependecies.getClassesFromDir(dir, recurse);
		Map<String, List<File>> classes = FindDependecies.getReferencedClassesFromFiles(files);
		List<String> cs = new ArrayList<String>(classes.keySet());
		int nclasses = classes.size();
		for (String c : cs) {
			if (isWhiteListed(c) || ignoreClassName(c)) {
				classes.remove(c);
			}
		}
		FindDependecies.printMap(classes);
		int errors = classes.size();
		if (VERBOSE) {
			if (errors == 0)
				System.out.println(dir + ": OK! " + nclasses + " classes examined in " + dir);
			else
				System.out.println(dir + ": ERR! " + errors + "/" + nclasses + " classes with problems in " + dir);
		}
		return new int[] { errors, nclasses };
	}

	public boolean isWhiteListed(String name) {
		if (whiteList == null)
			whiteList = loadWhiteListed();
		return whiteList.contains(name);
	}

	private Set<String> loadWhiteListed() {
		try {
			InputStream is = this.getClass().getResourceAsStream(WHITELIST_FILENAME);
			BufferedReader bf = new BufferedReader(new InputStreamReader(is));
			String line;
			HashSet<String> set = new HashSet<String>();
			while ((line = bf.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#"))
					continue;
				set.add(line);
			}
			bf.close();
			//System.out.println("loaded whitelisted classes: " + set.size() + " classes");
			return set;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean ignoreClassName(String name) {
		if (name.startsWith("ar.com.hjg.png"))
			return true; // those correspond to this package
		return false;
	}

	public void maintest() {
		// this should include every directory except "nosandnbox" and "test"
		int[] res = checkDir(new File("target/classes/ar/com/hjg/pngj"), false);
		System.out.println("=== The above should only report class PngHelperInternal2 (that's ok)");
		TestCase.assertEquals("Only opne class with errors", 1, res[0]);
		TestCase.assertTrue("More than 10 classes", res[1] > 10);
		res = checkDir(new File("target/classes/ar/com/hjg/pngj/chunks"), false);
		TestCase.assertEquals("No class with errors", 0, res[0]);
		TestCase.assertTrue("More than 10 classes", res[1] > 10);
		// checker.checkDir(new File("bin/ar/com/hjg/pngj/test"), false); // This fails, it's ok
	}

	public static void main(String[] args) {
		new WhiteListedCheck().maintest();
	}
}
