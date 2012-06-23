package ar.com.hjg.pngj.lossy;

public interface IErrorDifussion {

	public int getTotalErr(int row, int col,int channel);

	/**
	 * you must respect the order! leftoright or righttoleft err = exact - writen warning: here (and in the methods)
	 **/
	public void addErr(int row, int col, int channel, int err);

	public boolean isLeftToright();

	public void setLeftToright(boolean leftToright);

	public void reset();

}
