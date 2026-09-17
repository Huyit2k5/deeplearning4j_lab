package vn.huit.dl4j.benchmark.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Windows khong co /proc/pid/status (VmRSS) nhu Linux. Lop nay lay cac chi so
 * tuong duong tren Windows de xap xi muc dung bo nho khi benchmark:
 * - JVM heap usage qua MemoryMXBean.
 * - Native/off-heap (do libnd4j cap phat qua JavaCPP) qua Pointer.physicalBytes()/totalBytes().
 * Cac chi so nay KHONG giong het VmRSS cua Linux, chi la anh xa tuong duong
 * de so sanh tuong doi giua cac che do (VD: WorkspaceMode NONE vs ENABLED).
 */
public final class MemoryProbe {

    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();

    private MemoryProbe() {
    }

    public static long jvmHeapUsedBytes() {
        return MEMORY_BEAN.getHeapMemoryUsage().getUsed();
    }

    public static long jvmNonHeapUsedBytes() {
        return MEMORY_BEAN.getNonHeapMemoryUsage().getUsed();
    }

    /** Tong so byte native/off-heap ma JavaCPP (libnd4j) da cap phat va con giu (chua GC). */
    public static long nativeBytesUsed() {
        return org.bytedeco.javacpp.Pointer.totalBytes();
    }

    /** Gioi han toi da cho native bytes (cau hinh qua -Dorg.bytedeco.javacpp.maxbytes). */
    public static long nativeBytesLimit() {
        return org.bytedeco.javacpp.Pointer.maxBytes();
    }

    public static MemorySnapshot snapshot() {
        return new MemorySnapshot(jvmHeapUsedBytes(), jvmNonHeapUsedBytes(), nativeBytesUsed());
    }

    public record MemorySnapshot(long heapUsed, long nonHeapUsed, long nativeUsed) {
        public long totalApproxUsed() {
            return heapUsed + nonHeapUsed + nativeUsed;
        }
    }
}
