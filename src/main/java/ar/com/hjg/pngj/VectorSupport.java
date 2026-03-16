package ar.com.hjg.pngj;

public final class VectorSupport {
	private static final boolean AVAILABLE = detectVectorModule();

	private VectorSupport() {
	}

	public static boolean isAvailable() {
		return AVAILABLE;
	}

	private static boolean detectVectorModule() {
		try {
			return ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent();
		} catch (Throwable t) {
			return false;
		}
	}
}
