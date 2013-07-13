package ar.com.hjg.pngj;

public interface IImageLineFactory<T extends IImageLine> {
	public T createImageLine(ImageInfo iminfo);
}
