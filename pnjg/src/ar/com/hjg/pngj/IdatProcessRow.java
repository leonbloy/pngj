package ar.com.hjg.pngj;

public interface IdatProcessRow {
	// return: number of bytes for next row (0 or negative if no more expected)
	int processRow(byte[] rowb, int off, int len, int rown);
}