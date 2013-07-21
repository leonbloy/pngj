package ar.com.hjg.pngj;

/**
 * Set of ImageLines. This can be implemented in several ways:
 * 
 * 1. Cursor: stores only one line, which is implicitly moved when requested (typically only forward should be allowed)
 * 
 * 2. All lines: all lines stored as a IImageLine[]
 * 
 * 3. Subset of lines: eg, only first 3 lines, or odd numbered lines. Or a band of neighbours lines that is moved like a cursor.  
 * 
 * @param <T>
 */
public interface IImageLineSet<T extends IImageLine>  {

	/** 
	 * Asks for imageline corresponding to row <tt>n</tt> in the original image (zero based).
	 * This can trigger side effects in this object (eg, advance a cursor).
	 * This should be consider as alias to <tt>positionAtLine(n); getCurrentLine();</tt>
	 * 
	 * Throws exception if not available. The caller is supposed to know what he/she is doing  
	 **/
	public T getImageLine(int n);
	
	/** 
	 * Returns true if the set contain row <tt>n</tt> (in the original image,zero based) currently allocated.

	 * If it's a single-cursor, this should return true only if it's positioned there.
	 * (notice that hasImageLine(n) can return false, but getImageLine(n) can be ok)
	 *  
	 **/
	public boolean hasImageLine(int n);

	/** 
	 * Internal size of allocated rows 
	 * This is informational, it should rarely be important for the caller. 
	 **/
	public int size();

}
