package ar.com.hjg.pngj.pixels;

import java.io.OutputStream;
import java.util.zip.Deflater;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngHelperInternal;
import ar.com.hjg.pngj.PngjOutputException;

/**
 * Writes a set of rows (pixels) as a continuous deflated stream (does not know about IDAT chunk
 * segmentation)
 * <p>
 * This includes the filter selection strategy
 * <p>
 * This can (could, we hope) be used for APGN
 */
public abstract class PixelsWriter {

  protected final ImageInfo imgInfo;

  protected final int buflen; // including filter byte = imgInfo.bytesPerRow + 1
  protected final int bytesPixel;
  protected final int bytesRow;

  private CompressorStream compressorStream;

  private int deflaterCompLevel = 6;
  private int deflaterStrategy = Deflater.DEFAULT_STRATEGY;
  protected boolean initdone = false;

  protected FilterType filterType = FilterType.FILTER_DEFAULT;
  // counts the filters used - just for stats
  private int[] filtersUsed = new int[5];

  private OutputStream os;

  protected int currentRow;

  public PixelsWriter(ImageInfo imgInfo) {
    this.imgInfo = imgInfo;
    bytesRow = imgInfo.bytesPerRow;
    buflen = bytesRow + 1;
    bytesPixel = imgInfo.bytesPixel;
    currentRow = -1;
  }

  public final void processRow(final byte[] rowb) {
    if (!initdone)
      init();
    currentRow++;
    filterAndWrite(rowb);
  }

  protected void sendToCompressedStream(byte[] rowf) {
    compressorStream.write(rowf, 0, rowf.length);
    filtersUsed[rowf[0]]++;
  }

  /**
   * This does the filtering and send to stream. Typically should decide the filtering, call
   * {@link #filterRowWithFilterType(FilterType, byte[], byte[], byte[])} and and
   * {@link #sendToCompressedStream(byte[])}
   * 
   * @param rowb
   * @param currentRow
   */
  protected abstract void filterAndWrite(final byte[] rowb);

  /**
   * Does the real filtering. This must be called with the real (standard) filterType
   * 
   * @param _filterType
   * @param _rowb current row (the first byte might be modified)
   * @param _rowbprev previous row (should be all zero the first time)
   * @param _rowf tentative buffer to store the filtered bytes. might not be used!
   * @return normally _rowf, but eventually _rowb. This MUST NOT BE MODIFIED nor reused by caller
   */
  final protected byte[] filterRowWithFilterType(FilterType _filterType, byte[] _rowb,
      byte[] _rowbprev, byte[] _rowf) {
    // warning: some filters rely on: "previous row" (rowbprev) it must be initialized to 0 the
    // first time
    if (_filterType == FilterType.FILTER_NONE)
      _rowf = _rowb;
    _rowf[0] = (byte) _filterType.val;
    int i, j;
    switch (_filterType) {
      case FILTER_NONE:
        // we return the same original (be careful!)
        break;
      case FILTER_PAETH:
        for (i = 1; i <= bytesPixel; i++)
          _rowf[i] = (byte) PngHelperInternal.filterRowPaeth(_rowb[i], 0, _rowbprev[i] & 0xFF, 0);
        for (j = 1, i = bytesPixel + 1; i <= bytesRow; i++, j++)
          _rowf[i] =
              (byte) PngHelperInternal.filterRowPaeth(_rowb[i], _rowb[j] & 0xFF,
                  _rowbprev[i] & 0xFF, _rowbprev[j] & 0xFF);
        break;
      case FILTER_SUB:
        for (i = 1; i <= bytesPixel; i++)
          _rowf[i] = (byte) _rowb[i];
        for (j = 1, i = bytesPixel + 1; i <= bytesRow; i++, j++)
          _rowf[i] = (byte) (_rowb[i] - _rowb[j]);
        break;
      case FILTER_AVERAGE:
        for (i = 1; i <= bytesPixel; i++)
          _rowf[i] = (byte) (_rowb[i] - (_rowbprev[i] & 0xFF) / 2);
        for (j = 1, i = bytesPixel + 1; i <= bytesRow; i++, j++)
          _rowf[i] = (byte) (_rowb[i] - ((_rowbprev[i] & 0xFF) + (_rowb[j] & 0xFF)) / 2);
        break;
      case FILTER_UP:
        for (i = 1; i <= bytesRow; i++)
          _rowf[i] = (byte) (_rowb[i] - _rowbprev[i]);
        break;
      default:
        throw new PngjOutputException("Filter type not recognized: " + _filterType);
    }
    return _rowf;
  }

  /**
   * This will be called by the PngWrite to store the raw pixels for each row. This can change from
   * call to call. Warning: this can be called before the object is init, call init() o be sure
   */
  public abstract byte[] getRowb();

  /**
   * This will be called lazily just before writing row 0
   */
  protected void init() {
    if (!initdone) {
      initParams();
      if (compressorStream == null) { // if not set, use the deflater
        compressorStream =
            new CompressorStreamDeflater(os, buflen, imgInfo.getTotalRawBytes(),
                getDeflaterCompLevel(), getDeflaterStrategy());
      }
      initdone = true;
    }
  }

  /** called by init(); if override, call this first */
  protected void initParams() {
    // if adaptative but two few rows or columns, use default
    if (imgInfo.cols < 3 && !FilterType.isValidStandard(filterType))
      filterType = FilterType.FILTER_DEFAULT;
    if (imgInfo.rows < 3 && !FilterType.isValidStandard(filterType))
      filterType = FilterType.FILTER_DEFAULT;
  }

  /** cleanup. This should be called explicitly. This must be idempotent and not throw exceptions */
  public void close() {
    if (compressorStream != null) {
      compressorStream.close();
    }
  }

  public Integer getDeflaterStrategy() {
    return deflaterStrategy;
  }

  /**
   * Deflater (ZLIB) strategy. You should rarely change this from the default
   * (Deflater.DEFAULT_STRATEGY) to Deflater.FILTERED (Deflater.HUFFMAN_ONLY is fast but compress
   * poorly)
   */
  public void setDeflaterStrategy(Integer deflaterStrategy) {
    this.deflaterStrategy = deflaterStrategy;
  }

  public Integer getDeflaterCompLevel() {
    return deflaterCompLevel;
  }

  /**
   * Deflater (ZLIB) compression level, between 0 (no compression) and 9
   */
  public void setDeflaterCompLevel(Integer deflaterCompLevel) {
    this.deflaterCompLevel = deflaterCompLevel;
  }

  public final void setOs(OutputStream datStream) {
    this.os = datStream;
  }

  public OutputStream getOs() {
    return os;
  }

  final public FilterType getFilterType() {
    return filterType;
  }

  /**
   * this is a hint, for all the image; it can be a concrete filter type or a strategy
   */
  final public void setFilterType(FilterType filterType) {
    this.filterType = filterType;
  }

  final public String getFiltersUsed() {
    return String.format("%d,%d,%d,%d,%d", (int) (filtersUsed[0] * 100.0 / imgInfo.rows + 0.5),
        (int) (filtersUsed[1] * 100.0 / imgInfo.rows + 0.5), (int) (filtersUsed[2] * 100.0
            / imgInfo.rows + 0.5), (int) (filtersUsed[3] * 100.0 / imgInfo.rows + 0.5),
        (int) (filtersUsed[4] * 100.0 / imgInfo.rows + 0.5));
  }

  /* out/in This should be called only after end() to get reliable results */
  public double getCompression() {
    return compressorStream.isDone() ? compressorStream.getCompressionRatio() : 1.0;
  }

  public void setCompressorStream(CompressorStream compressorStream) {
    this.compressorStream = compressorStream;
  }

  public long getTotalBytesToWrite() {
    return imgInfo.getTotalRawBytes();
  }

  protected FilterType getDefaultFilter() {
    if (imgInfo.indexed || imgInfo.bitDepth < 8)
      return FilterType.FILTER_NONE;
    else if (imgInfo.getTotalPixels() < 1024)
      return FilterType.FILTER_NONE;
    else if (imgInfo.rows == 1)
      return FilterType.FILTER_SUB;
    else if (imgInfo.cols == 1)
      return FilterType.FILTER_UP;
    else
      return FilterType.FILTER_PAETH;
  }

}
