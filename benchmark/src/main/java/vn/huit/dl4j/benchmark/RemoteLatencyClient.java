package vn.huit.dl4j.benchmark;

import vn.huit.dl4j.benchmark.metrics.LatencyStats;
import vn.huit.dl4j.training.DataPipeline;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Kich ban 3 (phan remote): do do tre khi goi API /api/predict (B=1) va
 * /api/predict/batch (B=8) qua HTTP, so sanh voi in-process trong InferenceLatencyBenchmark.
 *
 * YEU CAU: module `api` phai dang chay san (vd http://localhost:8080) truoc khi chay benchmark nay.
 *
 * Usage: mvn -pl benchmark exec:java -Dexec.mainClass=vn.huit.dl4j.benchmark.RemoteLatencyClient
 *        -Dexec.args="<baseUrl> <warmupIters> <measureIters>"
 */
public final class RemoteLatencyClient {

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        int warmup = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int measure = args.length > 2 ? Integer.parseInt(args[2]) : 200;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        List<Map<String, Object>> results = new ArrayList<>();
        results.add(runBatch(client, baseUrl + "/api/predict", 1, warmup, measure));
        results.add(runBatch(client, baseUrl + "/api/predict/batch", 8, warmup, measure));

        java.io.File outDir = new java.io.File("results");
        List<String> header = List.of("mode", "batchSize", "meanMs", "p50Ms", "p99Ms", "minMs", "maxMs", "sampleCount");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : results) {
            rows.add(List.of(r.get("mode"), r.get("batchSize"), r.get("meanMs"), r.get("p50Ms"),
                    r.get("p99Ms"), r.get("minMs"), r.get("maxMs"), r.get("sampleCount")));
        }
        ResultWriter.writeCsv(new java.io.File(outDir, "inference_latency_remote.csv"), header, rows);
        ResultWriter.writeJson(new java.io.File(outDir, "inference_latency_remote.json"), results);

        results.forEach(System.out::println);
    }

    private static Map<String, Object> runBatch(HttpClient client, String url, int batchSize,
                                                 int warmup, int measure) throws Exception {
        String body = randomFeaturesJson(batchSize);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        for (int i = 0; i < warmup; i++) {
            client.send(request, HttpResponse.BodyHandlers.discarding());
        }

        long[] samples = new long[measure];
        for (int i = 0; i < measure; i++) {
            long start = System.nanoTime();
            client.send(request, HttpResponse.BodyHandlers.discarding());
            samples[i] = System.nanoTime() - start;
        }

        LatencyStats.Result stats = LatencyStats.compute(samples);

        return Map.of(
                "mode", "remote",
                "batchSize", batchSize,
                "meanMs", round(stats.meanMs()),
                "p50Ms", round(stats.p50Ms()),
                "p99Ms", round(stats.p99Ms()),
                "minMs", round(stats.minMs()),
                "maxMs", round(stats.maxMs()),
                "sampleCount", stats.sampleCount()
        );
    }

    private static String randomFeaturesJson(int batchSize) {
        Random random = new Random(42);
        StringBuilder sb = new StringBuilder();
        if (batchSize == 1) {
            sb.append("{\"features\": [");
            appendRandomFeatures(sb, random);
            sb.append("]}");
        } else {
            sb.append("{\"records\": [");
            for (int i = 0; i < batchSize; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("{\"features\": [");
                appendRandomFeatures(sb, random);
                sb.append("]}");
            }
            sb.append("]}");
        }
        return sb.toString();
    }

    private static void appendRandomFeatures(StringBuilder sb, Random random) {
        for (int i = 0; i < DataPipeline.NUM_FEATURES; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(random.nextDouble() * 10 - 5);
        }
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
