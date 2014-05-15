package ar.com.hjg.pngj.awt;

import java.awt.image.BufferedImage;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineSet;
import ar.com.hjg.pngj.ImageInfo;

// backed by a BufferedImage
public class ImageLineSetBI implements IImageLineSet<ImageLineBI> {

  final BufferedImage image;
  final Png2BufferedImageAdapter adapter2bi;
  final BufferedImage2PngAdapter adapter2png;
  final ImageInfo iminfo;

  // this is the number of lines of the "target" (BI if reading PNG, PNG elsewhere), less or equal than the rows of the source
  private int nlines;  
  // how many lines to skip from the source 
  private int offset;
  //steps is in the source; eg. if step=2, and isForWritePng==false, we are creating a BI with half the lines ofthe PNG; 
  // if step=2 and isForWritePng==false, we are creating a PNG with half the lines of the BI 
  private int step; 

  /** this constructor is for reading a PNG to a BufferedImage */
  public ImageLineSetBI(ImageInfo imgInfo, Png2BufferedImageAdapter adapter, boolean singleCursor,
      int nlines, int noffset, int step) {
    this.iminfo = imgInfo;
    this.adapter2bi = adapter;
    this.adapter2png = null;
    this.nlines = nlines;
    this.offset = noffset;
    this.step = step;
    image = adapter.createBufferedImage(imgInfo.cols, nlines);
  }

  public ImageLineSetBI(ImageInfo imgInfo, Png2BufferedImageAdapter adapter) {
    this(imgInfo, adapter, false, imgInfo.rows, 0, 1);
  }

  /** this constructor is for writing a PNG from an existing BufferedImage */
  public ImageLineSetBI(BufferedImage bi, BufferedImage2PngAdapter adapter, int nlines,
      int noffset, int step) {
    this.image = bi;
    this.adapter2png = adapter;
    this.adapter2bi = null;
    iminfo = adapter2png.createImgInfo(nlines, noffset, step);
  }

  public ImageLineSetBI(BufferedImage bi, BufferedImage2PngAdapter adapter) {
    this(bi, adapter, bi.getHeight(), 0, 1);
  }

  /** is this for write PNG or for write BufferredImage? */
  public boolean isForWritePng() {
    return adapter2png != null;
  }

  /**
   * The row number here referers to the PNG, not the BI
   * See {@link #getImageLineRawNum(int)}
   */
  public ImageLineBI getImageLine(int row) {
    int rowInBI = imageRowToMatrixRowStrict(row);
    if (rowInBI >= 0)
      return new ImageLineBI(this, rowInBI);
    return null;
  }
  
  
  /**
   * The row number here is that of the BufferedImage
   */
  public IImageLine getImageLineRawNum(int n) {
    return new ImageLineBI(this, n);
  }


  public boolean hasImageLine(int n) {
    return imageRowToMatrixRowStrict(n) >= 0;
  }

  public int size() {
    return nlines;
  }

  /**
   * Same as {@link #imageRowToMatrixRow(int)}, but returns negative if invalid
   */
  public int imageRowToMatrixRowStrict(int imrow) {
    imrow -= offset;
    int mrow = imrow >= 0 && (step == 1 || imrow % step == 0) ? imrow / step : -1;
    return mrow < nlines ? mrow : -1;
  }

  /**
   * Converts from matrix row number (0 : nRows-1) to image row number
   * 
   * @param mrow Matrix row number
   * @return Image row number. Returns trash if mrow is invalid
   */
  public int matrixRowToImageRow(int mrow) {
    return mrow * step + offset;
  }

  /**
   * Converts from real image row to this object row number.
   * <p>
   * Warning: this always returns a valid matrix row (clamping on 0 : nrows-1, and rounding down)
   * <p>
   * Eg: rowOffset=4,rowStep=2 imageRowToMatrixRow(17) returns 6 , imageRowToMatrixRow(1) returns 0
   */
  public int imageRowToMatrixRow(int imrow) {
    int r = (imrow - offset) / step;
    return r < 0 ? 0 : (r < nlines ? r : nlines - 1);
  }




}
