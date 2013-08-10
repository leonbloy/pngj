package ar.com.hjg.pngj;

/**
 * This interface is just for the sake of unifying some methods of
 * {@link ImageLineHelper} that can use both {@link ImageLineInt} or
 * {@link ImageLineByte}. It's not very useful outside that, and the user should
 * not rely much on this.
 */
public interface IImageLineArray {
	public ImageInfo getImageInfo();

	public FilterType getFilterType();

	public int getSize();

	public int getElem(int i);
}
