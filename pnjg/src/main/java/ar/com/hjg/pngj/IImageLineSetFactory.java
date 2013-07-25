package ar.com.hjg.pngj;

public interface IImageLineSetFactory<T extends IImageLine> {
	public IImageLineSet<T> create(ImageInfo imgInfo, boolean singleCursor, int nlines, int noffset, int step);
}
