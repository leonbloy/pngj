package ar.com.hjg.pngj;

import java.util.zip.Inflater;

public class InflaterInputStreamNb {
	
	protected Inflater inf;
    protected byte[] bufc; // compressed
    protected int lenc;
    protected byte[] buf; // uncompressed
    protected int len;
    private boolean closed = false;
    private boolean reachEOF = false;
    
    
    private void init() {
    	bufc=new byte[8192]; //TODO: parametrize this
    	buf=new byte[8192]; //TODO: parametrize this
    	len=0;
    	lenc=0;
    	if(inf==null) inf = new Inflater();
    }
    
    public void feed(byte[] x,int offf,int l) {
    	
    }

}
