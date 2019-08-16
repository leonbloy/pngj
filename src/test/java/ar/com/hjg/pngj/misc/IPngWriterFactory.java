package ar.com.hjg.pngj.misc;

import java.io.OutputStream;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngWriter;

public interface IPngWriterFactory {
    public PngWriter createPngWriter(OutputStream outputStream, ImageInfo imgInfo);
}
