package ar.com.hjg.pngj;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;

import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

/**
 * A few static utility methods related with PngReader/PngWriter that
 * read/writes to files.
 * <p>
 * This is not essential to the PNGJ library, and the writer will not work in
 * sandboxed environments (eg. Google App Engine)
 */
public class FileHelper {

	/**
	 * Creates a PngWriter from a file. The object is ready to start writing
	 * chunks or image rows. This is essentially equivalent to
	 * 
	 * <code>
	 *         new PngWriter(new FileOutputStream(file), imgInfo, file.getName) 
	 *  </code>
	 * 
	 * WARNING: This will throw exception if run in a sandboxed environment (as
	 * Google App Engine) that does not permit to use Java class
	 * java.io.FileOutputStream. You can always use the PngWriter constructor
	 * with an arbitrary OutputStream
	 * 
	 * @param file
	 *            File to be writen
	 * @param imgInfo
	 *            Target image basic info
	 * @param allowOverwrite
	 *            if true, file will be overwriten if it already exists.
	 * 
	 * @return a new PngWriter - see constructor doc
	 */
	public static PngWriter createPngWriter(File file, ImageInfo imgInfo, boolean allowOverwrite) {
		return new PngWriter(openFileForWriting(file, allowOverwrite), imgInfo, file.getName());
	}

	/**
	 * Creates a PngReader from a File. This is esentially the same as
	 * 
	 * <code>
	 *         new PngReader(new FileinputStream(file), imgInfo, file.getName) 
	 *  </code>
	 * 
	 * @param file
	 * @return A new PngReader object, ready for starting reading image rows
	 */
	public static PngReader createPngReader(File file) {
		return new PngReader(openFileForReading(file), file.getName());
	}

	/**
	 * Utility method to open a file for reading, with buffering and some
	 * checks.
	 */
	public static InputStream openFileForReading(File file) {
		InputStream isx = null;
		if (file == null || !file.exists() || !file.canRead())
			throw new PngjInputException("Cannot open file for reading (" + file + ")");
		try {
			isx = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new PngjInputException("Error opening file for reading (" + file + ") : " + e.getMessage());
		}
		return isx;
	}

	/***
	 * Utility method to open a file for writing.
	 * <p>
	 * WARNING: This method will throw exception if run in a sandboxed
	 * environment (as Google App Engine) that does not permit to use Java class
	 * java.io.FileOutputStream <br>
	 * We use reflection to be sure that this just throw run time exception in
	 * that case, but that the class is loadable
	 * 
	 * @param file
	 * @param allowOverwrite
	 *            if true, and the file exists, it will be overwriten; elswhere,
	 *            an exception is thrown
	 * @return outputStream
	 */
	public static OutputStream openFileForWriting(File file, boolean allowOverwrite) {
		if (file.exists() && !allowOverwrite)
			throw new PngjOutputException("File already exists (" + file + ") and overwrite=false");
		OutputStream os = null;
		Constructor<?> constructorOs = null;
		try {
			constructorOs = Class.forName("java.io.FileOutputStream").getConstructor(File.class);
		} catch (Exception e) {
			throw new PngjOutputException("Error opening file for write. "
					+ "Perhaps running in a sandboxed environment? If so, you can't use this method", e);
		}
		try {
			// osx = new FileOutputStream(file);
			os = (OutputStream) constructorOs.newInstance(file);
		} catch (Exception e) {
			throw new PngjOutputException("error opening " + file + " for write. "
					+ "Check that you have permission to write and that this is not a sandboxed environment", e);
		}
		return os;
	}
	
	public static int[][] readAsARGB32(PngReader pngr,	int[][] img) {
		pngr.setUnpackedMode(false); // we unpack in the conversion method
		if(img==null) img=new int[pngr.imgInfo.rows][pngr.imgInfo.cols];
		PngChunkPLTE pal = pngr.getMetadata().getPLTE();
		PngChunkTRNS trns = pngr.getMetadata().getTRNS();
		for(int r=0;r<pngr.imgInfo.rows;r++) {
			ImageLine line=pngr.readRowByte(r);
			ImageLineHelper.lineToARGB32(line, pal, trns, img[r]);
		}
		pngr.end();
		return img;
	}

}
