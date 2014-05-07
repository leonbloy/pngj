package ar.com.hjg.pngj;

/**
 * Factory of {@link IImageLineSet}, used by {@link PngReader}.
 * <p>
 * 
 * @param <T> Generic type of IImageLine
 */
public interface IImageLineSetFactory<T extends IImageLine> {
  /**
   * Creates a new {@link IImageLineSet}
   * 
   * @param imgInfo Image info
   * @param singleCursor : we intend to read one row at a time, if possible, and we are not interested in keeping the line in memory
   * @param nlines : how many lines we want the ImageLineSet to store
   * @param noffset : how many lines we want to skip from the original image (normally 0)
   * @param step : row step (normally 1)
   */
  public IImageLineSet<T> create(ImageInfo imgInfo, boolean singleCursor, int nlines, int noffset,
      int step);
}
