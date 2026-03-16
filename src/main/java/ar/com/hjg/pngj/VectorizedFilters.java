package ar.com.hjg.pngj;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public final class VectorizedFilters {
	private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;
	private static final VectorSpecies<Integer> ISPECIES = IntVector.SPECIES_PREFERRED;
	// Number of B2I parts: how many IntVectors fit in one ByteVector
	private static final int B2I_PARTS = SPECIES.length() / ISPECIES.length();

	private VectorizedFilters() {
	}

	public static void unfilterUp(byte[] row, byte[] rowUnfilteredPrev, byte[] rowUnfiltered, int nbytes) {
		int i = 1;
		int limit = nbytes - SPECIES.length() + 1;
		for (; i <= limit; i += SPECIES.length()) {
			ByteVector vRow = ByteVector.fromArray(SPECIES, row, i);
			ByteVector vPrev = ByteVector.fromArray(SPECIES, rowUnfilteredPrev, i);
			vRow.add(vPrev).intoArray(rowUnfiltered, i);
		}
		for (; i <= nbytes; i++) {
			rowUnfiltered[i] = (byte) (row[i] + rowUnfilteredPrev[i]);
		}
	}

	/**
	 * Zero-extends each byte in src[srcOff..srcOff+len) into dst[dstOff..dstOff+len) as unsigned ints.
	 * Equivalent to: dst[dstOff+i] = src[srcOff+i] & 0xFF
	 */
	public static void widenBytesToInts(byte[] src, int srcOff, int[] dst, int dstOff, int len) {
		int bLen = SPECIES.length();
		int iLen = ISPECIES.length();
		int i = 0;
		int upperBound = len - bLen;
		for (; i <= upperBound; i += bLen) {
			ByteVector bv = ByteVector.fromArray(SPECIES, src, srcOff + i);
			for (int p = 0; p < B2I_PARTS; p++) {
				// B2I is sign-extending; mask with 0xFF to get unsigned byte value
				((IntVector) bv.convert(VectorOperators.B2I, p)).and(0xFF)
						.intoArray(dst, dstOff + i + p * iLen);
			}
		}
		for (; i < len; i++) {
			dst[dstOff + i] = src[srcOff + i] & 0xFF;
		}
	}

	/**
	 * Truncates each int in src[srcOff..srcOff+len) to a byte in dst[dstOff..dstOff+len).
	 * Equivalent to: dst[dstOff+i] = (byte) src[srcOff+i]
	 */
	public static void narrowIntsToBytes(int[] src, int srcOff, byte[] dst, int dstOff, int len) {
		// Scalar: I2B via Vector API requires SPECIES_32 which is not a named constant in JDK 17.
		// The encode path is not the hot path, so scalar is fine here.
		for (int i = 0; i < len; i++) {
			dst[dstOff + i] = (byte) src[srcOff + i];
		}
	}

	public static void filterUp(byte[] rowb, byte[] rowbprev, byte[] rowf, int bytesRow) {
		int i = 1;
		int limit = bytesRow - SPECIES.length() + 1;
		for (; i <= limit; i += SPECIES.length()) {
			ByteVector vRow = ByteVector.fromArray(SPECIES, rowb, i);
			ByteVector vPrev = ByteVector.fromArray(SPECIES, rowbprev, i);
			vRow.sub(vPrev).intoArray(rowf, i);
		}
		for (; i <= bytesRow; i++) {
			rowf[i] = (byte) (rowb[i] - rowbprev[i]);
		}
	}
}
