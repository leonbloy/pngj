# PNGJ: Java library for PNG encoding

PNGJ is a pure Java library for high performance reading and writing of PNG images 

---

## Downloads

You can download the latest release from here http://hjg.com.ar/pngj/ or use the [Maven Central repository](https://search.maven.org/search?q=pngj)

## Main features

* Very efficient in memory usage and speed
* Pure Java (8 or greater)
* Small and self contained. No dependencies on third party libraries, nor even on `java.awt.*` or `javax.imageio.*`
* Runs on  __Android__ and __GAE__ (Google App Engine) 
* Allows to read and write progressively, row by row. This means that you can process huge images without needing to load them fully in memory.
* Reads and writes all PNG color models. Interlaced PNG is supported (though not welcomed) for reading.
* Full support for __metadata__ handling ("chunks").
* The format of the pixel data (for read and write) is extensible and efficient (no double copies).
* Supports asyncronous reading and low level tweaking and extension in the reader.
* Basic support for APNG reading
* Open source (Apache licence). Available in Maven Central repository.

## What is this for?

This is a relatively low level library, its goal is to code and decode PNG images from/to raw pixels, optimizing memory usage and speed.
It does not provide any high-level image processing (eg, resizing, colour conversions), it does not try to abstract the concrete PNG color model (as `BufferedImage` does, for example). 
In particular, the default format of the scanlines (as wrapped in `ImageLineInt` or `ImageLineByte`) is not abstract, the meaning of the values depends on the image color model and bitdepth.

## More documentation

 * [Javadocs](http://hjg.com.ar/pngj/apidocs/)
 * [Wiki](https://github.com/leonbloy/pngj/wiki)
