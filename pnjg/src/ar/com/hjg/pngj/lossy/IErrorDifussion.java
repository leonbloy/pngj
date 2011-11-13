package ar.com.hjg.pngj.lossy;


public interface IErrorDifussion {

	/** warning: here (and in the methods) 'col' is measure not in pixels but in samples !  = cols x channels x bytespsample*/ 
	public int getTotalErr(int row, int col);
			/** you must respect the order! leftoright or righttoleft 
	 *  err = exact - writen
	 * 	warning: here (and in the methods) 'col' is measure not in pixels but in samples !  = cols x channels x bytespsample 
	 **/
	public void addErr(int row, int col, int err); 
	public boolean isLeftToright();
	public void setLeftToright(boolean leftToright);
	public void reset();

	
}
