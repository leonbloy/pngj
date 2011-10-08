package ar.com.hjg.pngj.nosandbox;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjInputException;
import ar.com.hjg.pngj.PngjOutputException;

/**
 * Several static utility methods related with PngReader/PngWriter
 * that uses the filesystem.
 * <p>
 * This is ont essential to the PNGJ library, and will not be 
 * available in sandboxed environments (eg. Google app engine) 
 *
 */
public class FileHelper {

	
	public static InputStream openFileForReading(File file) {
		InputStream isx = null;
		if (file == null || !file.exists() || !file.canRead())
			throw new PngjInputException("Cannot open file for reading (" + file + ")");
		try {
			isx = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new PngjInputException("Error opening file for reading (" + file + ") : "
					+ e.getMessage());
		}
		return isx;
	}

	public static OutputStream openFileForWriting(File file, boolean allowOverwrite) {
		OutputStream osx = null;
		if (file.exists() && !allowOverwrite)
			throw new PngjOutputException("File already exists (" + file
					+ ") and overwrite=false");
		try {
			osx = new FileOutputStream(file);
		} catch (Exception e) {
			throw new PngjOutputException("error opening " + file + " for writing", e);
		}
		return osx;
	}


	public static PngWriter createPngWriter(File file, ImageInfo imgInfo,
			boolean allowOverwrite) {
		return new PngWriter(openFileForWriting(file, allowOverwrite), imgInfo,
				file.getName());
	}

	public static PngReader createPngReader(File file) {
		return new PngReader(openFileForReading(file), file.getName());
	}

	
}
