package vn.huit.dl4j.benchmark.metrics;

import java.util.Arrays;

/** Tinh cac chi so thong ke tre (mean, p50, p99) tu mang do latency (nano hoac milli giay). */
public final class LatencyStats {

    private LatencyStats() {
    }

    public static Result compute(long[] samplesNanos) {
        long[] sorted = samplesNanos.clone();
        Arrays.sort(sorted);

        double meanMs = Arrays.stream(sorted).average().orElse(0) / 1_000_000.0;
        double p50Ms = percentile(sorted, 0.50) / 1_000_000.0;
        double p99Ms = percentile(sorted, 0.99) / 1_000_000.0;
        double minMs = sorted[0] / 1_000_000.0;
        double maxMs = sorted[sorted.length - 1] / 1_000_000.0;

        return new Result(meanMs, p50Ms, p99Ms, minMs, maxMs, sorted.length);
    }

    private static long percentile(long[] sortedAsc, double p) {
        int index = (int) Math.ceil(p * sortedAsc.length) - 1;
        index = Math.max(0, Math.min(sortedAsc.length - 1, index));
        return sortedAsc[index];
    }

    public record Result(double meanMs, double p50Ms, double p99Ms, double minMs, double maxMs, int sampleCount) {
    }
}
