package ar.com.hjg.pngj;


/**
 * ImageLine and ImageBye (most common IImageLine) also implement this 
 */
public interface IImageLineArray {
	public ImageInfo getImageInfo();
	public FilterType getFilterType();
	public int getSize();
	public int getElem(int i);
}
