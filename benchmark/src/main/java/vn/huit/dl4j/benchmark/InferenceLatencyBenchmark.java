package vn.huit.dl4j.benchmark;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import vn.huit.dl4j.benchmark.metrics.LatencyStats;
import vn.huit.dl4j.training.DataPipeline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kich ban 3 (phan in-process): do do tre suy luan khi goi model.output(...)
 * truc tiep trong cung tien trinh JVM, o batch size B=1 va B=8.
 *
 * Usage: mvn -pl benchmark exec:java -Dexec.mainClass=vn.huit.dl4j.benchmark.InferenceLatencyBenchmark
 *        -Dexec.args="<modelPath> <warmupIters> <measureIters>"
 */
public final class InferenceLatencyBenchmark {

    public static void main(String[] args) throws Exception {
        File modelFile = args.length > 0 ? new File(args[0]) : new File("training/models/fraud_mlp.zip");
        int warmup = args.length > 1 ? Integer.parseInt(args[1]) : 50;
        int measure = args.length > 2 ? Integer.parseInt(args[2]) : 1000;

        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(modelFile);

        List<Map<String, Object>> results = new ArrayList<>();
        results.add(runBatch(model, 1, warmup, measure));
        results.add(runBatch(model, 8, warmup, measure));

        File outDir = new File("results");
        List<String> header = List.of("mode", "batchSize", "meanMs", "p50Ms", "p99Ms", "minMs", "maxMs", "sampleCount");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : results) {
            rows.add(List.of(r.get("mode"), r.get("batchSize"), r.get("meanMs"), r.get("p50Ms"),
                    r.get("p99Ms"), r.get("minMs"), r.get("maxMs"), r.get("sampleCount")));
        }
        ResultWriter.writeCsv(new File(outDir, "inference_latency_inprocess.csv"), header, rows);
        ResultWriter.writeJson(new File(outDir, "inference_latency_inprocess.json"), results);

        results.forEach(System.out::println);
    }

    private static Map<String, Object> runBatch(MultiLayerNetwork model, int batchSize,
                                                 int warmup, int measure) {
        INDArray input = Nd4j.rand(batchSize, DataPipeline.NUM_FEATURES);

        for (int i = 0; i < warmup; i++) {
            model.output(input);
        }

        long[] samples = new long[measure];
        for (int i = 0; i < measure; i++) {
            long start = System.nanoTime();
            model.output(input);
            samples[i] = System.nanoTime() - start;
        }

        LatencyStats.Result stats = LatencyStats.compute(samples);

        return Map.of(
                "mode", "in-process",
                "batchSize", batchSize,
                "meanMs", round(stats.meanMs()),
                "p50Ms", round(stats.p50Ms()),
                "p99Ms", round(stats.p99Ms()),
                "minMs", round(stats.minMs()),
                "maxMs", round(stats.maxMs()),
                "sampleCount", stats.sampleCount()
        );
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
