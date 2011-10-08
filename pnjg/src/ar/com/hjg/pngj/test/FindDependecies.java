package ar.com.hjg.pngj.test;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FindDependecies {
	public static List<File> getClassesFromDir(File dir, boolean recurse) {
		File[] files = dir.listFiles();
		List<File> f = new ArrayList<File>();
		for (File file : files) {
			if (file.getName().endsWith(".class"))
				f.add(file);
			if (file.isDirectory() && recurse)
				f.addAll(getClassesFromDir(file, true));
		}
		return f;
	}

	public static Map<String, List<File>> getReferencedClassesFromFiles(
			Collection<File> files) {
		HashMap<String, List<File>> map = new HashMap<String, List<File>>();
		for (File file : files) {
			Set<String> classes;
			try {
				classes = getReferencedClassesFromFile(file);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			for (String c : classes) {
				if (!map.containsKey(c))
					map.put(c, new ArrayList<File>());
				map.get(c).add(file);
			}
		}
		return map;
	}

	private static Set<String> getReferencedClassesFromFile(File file) throws IOException {
		Map<Integer, String> strings = new HashMap<Integer, String>();
		Set<Integer> classes = new HashSet<Integer>();
		int indexCorrection = 0; // for correcting indexes to constant
		// pools with long and double entries
		DataInputStream stream = new DataInputStream(new BufferedInputStream(
				new FileInputStream(file)));
		try {
			readU4(stream); // magic byte
			readU2(stream); // minor version
			readU2(stream); // major version
			int poolSize = readU2(stream);
			for (int n = 1; n < poolSize; n++) {
				int tag = readU1(stream);
				switch (tag) {
				case 1: // Utf8
					String content = readString(stream);
					strings.put(n, content);
					break;
				case 7: // Class
					int nameIndex = readU2(stream);
					classes.add(nameIndex - indexCorrection);
					break;
				case 8: // String
					readU2(stream);
					break;
				case 3: // Integer
				case 4: // Float
					readU4(stream);
					break;
				case 5: // Long
				case 6: // Double
					readU4(stream);
					readU4(stream);
					indexCorrection++;
					break;
				case 9: // Fieldref
				case 10: // Methodref
				case 11: // InterfaceMethodref
				case 12: // NameAndType
					readU2(stream);
					readU2(stream);
					break;
				}
			}
		} finally {
			stream.close();
		}
		Set<String> allclasses = new HashSet<String>();
		for (Integer index : classes) {
			String c = strings.get(index).replaceAll("/", ".");
			if (c.startsWith("["))
				c = c.substring(1);
			if (c.length() < 2)
				continue;
			allclasses.add(c);
		}
		return allclasses;
	}

	private static String readString(DataInputStream stream) throws IOException {
		return stream.readUTF();
	}

	private static int readU1(DataInputStream stream) throws IOException {
		return stream.readUnsignedByte();
	}

	private static int readU2(DataInputStream stream) throws IOException {
		return stream.readUnsignedShort();
	}

	private static int readU4(DataInputStream stream) throws IOException {
		return stream.readInt();
	}

	public static void printSet(Set<String> set) {
		List<String> setl = new ArrayList<String>(set);
		Collections.sort(setl);
		for (String string : setl) {
			System.out.println(string);
		}
	}

	public static void printMap(Map<String, List<File>> map) {
		List<String> setl = new ArrayList<String>(map.keySet());
		Collections.sort(setl);
		for (String c : setl) {
			System.out.print(c + "\t(");
			for (File f : map.get(c)) {
				System.out.print(f.getName() + " ");
			}
			System.out.print(")\n");
		}
	}

	public static void main(String[] args) throws Exception {
		// String dir=
		// "c:\hjg\workspace\pnjg\bin\ar\com\hjg\pngj\PngReader.classar/com/hjg/png";
		String dir = "bin/ar/com/hjg/pngj/";
		List<File> files = getClassesFromDir(new File(dir), false);
		Map<String, List<File>> classes = getReferencedClassesFromFiles(files);
		printMap(classes);
	}
}