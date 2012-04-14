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

/**
 * 
 * checks that the compiled classes are apt for sandboxed environment
 * 
 * http://code.google.com/appengine/docs/java/jrewhitelist.html
 * 
 * @author Hernan J Gonzalez
 * 
 */
class CheckWhiteListed {

	private static final String WHITELIST_FILENAME = "whitelistedclasses.txt";
	private Set<String> whiteList;

	public void checkDir(File dir, boolean recurse) {
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
		if (classes.isEmpty())
			System.out.println(dir + ": OK! " + nclasses + " classes examined");
		else
			System.out.println(dir + ": ERR! " + classes.size() + "/" + nclasses + " classes with problems ");
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
				if (line.isEmpty() || line.startsWith("#"))
					continue;
				set.add(line);
			}
			bf.close();
			System.out.println("loaded whitelisted classes: " + set.size() + " classes");
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

	public static void main(String[] args) {
		CheckWhiteListed checker = new CheckWhiteListed();
		// this should include every directory except "nosandnbox" and "test"
		checker.checkDir(new File("bin/ar/com/hjg/pngj"), false);
		checker.checkDir(new File("bin/ar/com/hjg/pngj/chunks"), false);
		checker.checkDir(new File("bin/ar/com/hjg/pngj/lossy"), false);
		// checker.checkDir(new File("bin/ar/com/hjg/pngj/test"), false); // This fails, it's ok
	}
}
