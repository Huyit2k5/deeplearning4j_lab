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
 * /api/predict/batch (B>1) qua HTTP, quet qua nhieu batch size, so sanh voi
 * in-process trong InferenceLatencyBenchmark. Dung chung cho ca Spring Boot
 * (Java) va FastAPI (Python) - phan biet bang tham so <label>.
 *
 * YEU CAU: server dich (Spring Boot tren :8080 hoac FastAPI tren :8000) phai
 * dang chay san truoc khi goi benchmark nay.
 *
 * Usage: mvn -pl benchmark exec:java -Dexec.mainClass=vn.huit.dl4j.benchmark.RemoteLatencyClient
 *        -Dexec.args="<baseUrl> <warmupIters> <measureIters> <label>"
 *
 * Vi du:
 *   -Dexec.args="http://localhost:8080 20 200 springboot"
 *   -Dexec.args="http://localhost:8000 20 200 fastapi"
 */
public final class RemoteLatencyClient {

    private static final int[] BATCH_SIZES = {1, 8, 16, 32, 64};

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        int warmup = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int measure = args.length > 2 ? Integer.parseInt(args[2]) : 200;
        String label = args.length > 3 ? args[3] : "springboot";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        List<Map<String, Object>> results = new ArrayList<>();
        for (int batchSize : BATCH_SIZES) {
            String path = batchSize == 1 ? "/api/predict" : "/api/predict/batch";
            results.add(runBatch(client, baseUrl + path, batchSize, warmup, measure, label));
        }

        java.io.File outDir = new java.io.File("results");
        List<String> header = List.of("mode", "batchSize", "meanMs", "p50Ms", "p99Ms", "minMs", "maxMs",
                "throughputPerSec", "sampleCount");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : results) {
            rows.add(List.of(r.get("mode"), r.get("batchSize"), r.get("meanMs"), r.get("p50Ms"),
                    r.get("p99Ms"), r.get("minMs"), r.get("maxMs"), r.get("throughputPerSec"), r.get("sampleCount")));
        }
        ResultWriter.writeCsv(new java.io.File(outDir, "inference_latency_remote_" + label + ".csv"), header, rows);
        ResultWriter.writeJson(new java.io.File(outDir, "inference_latency_remote_" + label + ".json"), results);

        results.forEach(System.out::println);
    }

    private static Map<String, Object> runBatch(HttpClient client, String url, int batchSize,
                                                 int warmup, int measure, String label) throws Exception {
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
        double throughputPerSec = batchSize / (stats.meanMs() / 1000.0);

        return Map.of(
                "mode", "remote-" + label,
                "batchSize", batchSize,
                "meanMs", round(stats.meanMs()),
                "p50Ms", round(stats.p50Ms()),
                "p99Ms", round(stats.p99Ms()),
                "minMs", round(stats.minMs()),
                "maxMs", round(stats.maxMs()),
                "throughputPerSec", round(throughputPerSec),
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
