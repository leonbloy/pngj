package ar.com.hjg.pngj.awt;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;

import ar.com.hjg.pngj.IImageLine;
import ar.com.hjg.pngj.IImageLineSet;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.PngjOutputException;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

public class PngWriterBI extends PngWriter{

  boolean preferCustomInsteadOfBGR=true;
  protected BufferedImage2PngAdapter adapter;
  private ImageLineSetBI ilineset;

  public static PngWriterBI createInstance(BufferedImage bi, File file) {
    return createInstance(bi, bi.getHeight(),0,1,file,true);
  }

  public static PngWriterBI createInstance(BufferedImage bi, OutputStream os) {
    return createInstance(bi, bi.getHeight(),0,1,os);
  }

  public static PngWriterBI createInstance(BufferedImage2PngAdapter adapter,File file) {
    ImageLineSetBI ilineset = new ImageLineSetBI(adapter.image, adapter);
    PngWriterBI pngw = new PngWriterBI(file,ilineset.iminfo);
    pngw.initBi(ilineset);
    return pngw;
  }

  
  public static PngWriterBI createInstance(BufferedImage bi, int nlines, int offset,int step, File file,boolean allowoverwrite) {
    BufferedImage2PngAdapter adapter=new BufferedImage2PngAdapter(bi);
    ImageLineSetBI ilineset = new ImageLineSetBI(bi, adapter,nlines,offset,step);
    PngWriterBI pngw = new PngWriterBI(file,ilineset.iminfo,allowoverwrite);
    pngw.initBi(ilineset);
    return pngw;
  }

  public static PngWriterBI createInstance(BufferedImage bi, int nlines, int offset,int step, OutputStream os) {
    // better to repeat code than to open the stream before being sure that no exception is thrown by the adapter
    BufferedImage2PngAdapter adapter=new BufferedImage2PngAdapter(bi);
    ImageLineSetBI ilineset = new ImageLineSetBI(bi, adapter,nlines,offset,step);
    PngWriterBI pngw = new PngWriterBI(os,ilineset.iminfo);
    pngw.initBi(ilineset);
    return pngw;
  }

  private void initBi(ImageLineSetBI ilineset2) {
    this.ilineset=ilineset2;
    this.adapter=ilineset2.adapter2png;
    if(imgInfo.indexed) {
      PngChunkPLTE pal = adapter.getPlteChunk();
      PngChunkTRNS trns = adapter.getTrnsChunk();
      getChunksList().queue(pal);
      if(trns!=null) getChunksList().queue(trns);
    }
  }

  protected PngWriterBI(File file, ImageInfo imgInfo, boolean allowoverwrite) {
    super(file, imgInfo, allowoverwrite);
  }

  protected PngWriterBI(File file, ImageInfo imgInfo) {
    super(file, imgInfo);
  }

  protected PngWriterBI(OutputStream outputStream, ImageInfo imgInfo) {
    super(outputStream, imgInfo);
  }

  public void writeAll() {
    super.writeRows(ilineset);
    end();
  }


  
 
}
