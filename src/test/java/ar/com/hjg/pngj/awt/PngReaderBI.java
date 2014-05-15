package ar.com.hjg.pngj.awt;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineSet;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

public class PngReaderBI extends PngReader{

  boolean preferCustomInsteadOfBGR=true;
  
  public PngReaderBI(File file) {
    super(file);
  }

  public PngReaderBI(InputStream inputStream, boolean shouldCloseStream) {
    super(inputStream, shouldCloseStream);
  }

  public PngReaderBI(InputStream inputStream) {
    super(inputStream);
  }

  @Override
  protected IImageLineSet<? extends IImageLine> createLineSet(boolean singleCursor, int nlines,
      int noffset, int step) {
    PngChunkPLTE pal=(PngChunkPLTE) getChunksList().getById1(PngChunkPLTE.ID);// perhaps null;
    PngChunkTRNS trns = (PngChunkTRNS) getChunksList().getById1(PngChunkTRNS.ID);// perhaps null;
    Png2BufferedImageAdapter adapter=new Png2BufferedImageAdapter(getCurImgInfo(), pal, trns);
    adapter.setPreferCustom(preferCustomInsteadOfBGR);
    return new ImageLineSetBI(getCurImgInfo(),adapter,singleCursor, nlines,noffset, step);
  }

  public void setPreferCustomInsteadOfBGR(boolean b){
    preferCustomInsteadOfBGR=b;
  }
  
  public ImageLineSetBI getImageLineSetBI() {
    if(imlinesSet!=null) return (ImageLineSetBI)imlinesSet;
    else return null;
  }
  
  public BufferedImage readAll() {
    readRows();
    end();
    return getImageLineSetBI().image;
  }

  public BufferedImage readAll(int lines,int offset,int step) {
    readRows(lines,offset,step);
    end();
    return getImageLineSetBI().image;
  }
  
}
