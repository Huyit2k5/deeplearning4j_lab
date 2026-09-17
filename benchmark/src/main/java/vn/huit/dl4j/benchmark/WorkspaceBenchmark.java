package vn.huit.dl4j.benchmark;

import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import vn.huit.dl4j.benchmark.metrics.MemoryProbe;
import vn.huit.dl4j.training.DataPipeline;
import vn.huit.dl4j.training.ModelFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kich ban 1: so sanh thoi gian train + muc dung bo nho giua
 * WorkspaceMode.NONE va WorkspaceMode.ENABLED, tren cung mot du lieu/model.
 *
 * Ghi them chi tiet TUNG epoch (thoi gian + JVM heap + timestamp) ra
 * results/workspace_epoch_detail.csv, va PID cua chinh JVM nay ra
 * results/workspace_pid.txt, de mot sampler ben ngoai (PowerShell Get-Process)
 * co the tuong quan Working Set (tuong duong VmRSS tren Windows) theo dung
 * timestamp cua tung epoch.
 *
 * Usage: mvn -pl benchmark exec:java -Dexec.mainClass=vn.huit.dl4j.benchmark.WorkspaceBenchmark
 *        -Dexec.args="<csvPath> <epochs> <batchSize>"
 */
public final class WorkspaceBenchmark {

    public static void main(String[] args) throws Exception {
        File csvFile = args.length > 0 ? new File(args[0]) : new File("data/creditcard.csv");
        int epochs = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        int batchSize = args.length > 2 ? Integer.parseInt(args[2]) : 32;

        File outDir = new File("results");
        outDir.mkdirs();
        try (FileWriter pidWriter = new FileWriter(new File(outDir, "workspace_pid.txt"))) {
            pidWriter.write(String.valueOf(ProcessHandle.current().pid()));
        }

        DataSet fullDataSet = DataPipeline.loadFullDataset(csvFile);
        SplitTestAndTrain split = DataPipeline.splitTrainTest(fullDataSet, 0.8, 42L);
        DataSet trainData = split.getTrain();

        NormalizerStandardize normalizer = DataPipeline.fitNormalizer(trainData);
        normalizer.transform(trainData);

        List<Map<String, Object>> epochDetails = new ArrayList<>();
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(runMode(WorkspaceMode.NONE, trainData, epochs, batchSize, epochDetails));
        System.gc();
        Thread.sleep(500);
        results.add(runMode(WorkspaceMode.ENABLED, trainData, epochs, batchSize, epochDetails));

        List<String> header = List.of("mode", "epochs", "batchSize", "elapsedMs",
                "heapUsedBytes", "nonHeapUsedBytes", "nativeUsedBytes");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : results) {
            rows.add(List.of(r.get("mode"), r.get("epochs"), r.get("batchSize"), r.get("elapsedMs"),
                    r.get("heapUsedBytes"), r.get("nonHeapUsedBytes"), r.get("nativeUsedBytes")));
        }
        ResultWriter.writeCsv(new File(outDir, "workspace_benchmark.csv"), header, rows);
        ResultWriter.writeJson(new File(outDir, "workspace_benchmark.json"), results);

        List<String> epochHeader = List.of("mode", "epoch", "elapsedMs", "heapUsedBytes", "timestampMs");
        List<List<Object>> epochRows = new ArrayList<>();
        for (Map<String, Object> r : epochDetails) {
            epochRows.add(List.of(r.get("mode"), r.get("epoch"), r.get("elapsedMs"),
                    r.get("heapUsedBytes"), r.get("timestampMs")));
        }
        ResultWriter.writeCsv(new File(outDir, "workspace_epoch_detail.csv"), epochHeader, epochRows);

        results.forEach(System.out::println);
    }

    private static Map<String, Object> runMode(WorkspaceMode mode, DataSet trainData,
                                                int epochs, int batchSize,
                                                List<Map<String, Object>> epochDetails) {
        MultiLayerNetwork model = ModelFactory.buildModel(42L, mode, mode);

        var trainIter = new org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator<>(
                trainData.asList(), batchSize);

        long start = System.nanoTime();
        for (int epoch = 1; epoch <= epochs; epoch++) {
            long epochStart = System.nanoTime();
            trainIter.reset();
            model.fit(trainIter);
            long epochElapsedMs = (System.nanoTime() - epochStart) / 1_000_000;
            long heapUsedBytes = MemoryProbe.jvmHeapUsedBytes();
            long timestampMs = System.currentTimeMillis();

            epochDetails.add(Map.of(
                    "mode", mode.toString(),
                    "epoch", epoch,
                    "elapsedMs", epochElapsedMs,
                    "heapUsedBytes", heapUsedBytes,
                    "timestampMs", timestampMs
            ));
            System.out.println("EPOCH_CHECKPOINT,mode=" + mode + ",epoch=" + epoch
                    + ",elapsedMs=" + epochElapsedMs + ",heapUsedBytes=" + heapUsedBytes
                    + ",timestampMs=" + timestampMs);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        MemoryProbe.MemorySnapshot snapshot = MemoryProbe.snapshot();

        return Map.of(
                "mode", mode.toString(),
                "epochs", epochs,
                "batchSize", batchSize,
                "elapsedMs", elapsedMs,
                "heapUsedBytes", snapshot.heapUsed(),
                "nonHeapUsedBytes", snapshot.nonHeapUsed(),
                "nativeUsedBytes", snapshot.nativeUsed()
        );
    }
}
