package vn.huit.dl4j.benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Kich ban 2: chay vn.huit.dl4j.training.SparkTrainingRunner trong mot JVM RIENG cho
 * moi cau hinh Worker (local[1,2,4,8]), MOI CAU HINH LAP LAI 3 LAN (1 epoch/lan) de
 * tinh mean +/- std, roi suy ra Toc do gia tang (speedup) va Hieu suat co gian
 * (efficiency) so voi local[1] lam co so.
 *
 * Dinh bo nho VmHWM (peak Working Set tren Windows) duoc theo doi bang mot thread rieng
 * polling `Get-Process -Id <pid>` moi 200ms trong luc tien trinh con dang chay.
 *
 * YEU CAU: chay tu thu muc goc project, sau khi da `mvn install` module training
 * (de benchmark module thay duoc jar cua training qua classpath khi spawn tien trinh con).
 *
 * Usage: mvn -pl benchmark exec:java -Dexec.mainClass=vn.huit.dl4j.benchmark.SparkScalingBenchmark
 *        -Dexec.args="<csvPath> <epochs> <batchSizePerWorker>"
 */
public final class SparkScalingBenchmark {

    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "SPARK_SCALING_RESULT,threads=(\\d+),batchSize=(\\d+),elapsedMs=(\\d+),throughputRecPerSec=([\\d.]+)");
    private static final int[] THREAD_LEVELS = {1, 2, 4, 8};
    private static final int REPEATS = 3;

    public static void main(String[] args) throws Exception {
        String csvPath = args.length > 0 ? args[0] : "data/creditcard.csv";
        int epochs = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        int batchSizePerWorker = args.length > 2 ? Integer.parseInt(args[2]) : 32;

        Map<Integer, List<Map<String, Object>>> runsByThreads = new LinkedHashMap<>();
        for (int threads : THREAD_LEVELS) {
            List<Map<String, Object>> runs = new ArrayList<>();
            for (int run = 1; run <= REPEATS; run++) {
                System.out.println("=== Running Spark local[" + threads + "] (run " + run + "/" + REPEATS + ") ===");
                Map<String, Object> result = runSparkTrainingSubprocess(csvPath, threads, epochs, batchSizePerWorker);
                if (result != null) {
                    runs.add(result);
                    System.out.println(result);
                } else {
                    System.out.println("WARNING: no result parsed for threads=" + threads + " run=" + run);
                }
            }
            runsByThreads.put(threads, runs);
        }

        List<Map<String, Object>> rawRows = new ArrayList<>();
        for (Map.Entry<Integer, List<Map<String, Object>>> e : runsByThreads.entrySet()) {
            for (Map<String, Object> r : e.getValue()) {
                rawRows.add(r);
            }
        }

        List<Map<String, Object>> aggregated = aggregate(runsByThreads);

        File outDir = new File("results");
        List<String> rawHeader = List.of("threads", "batchSize", "elapsedMs", "throughputRecPerSec", "peakWorkingSetBytes");
        List<List<Object>> rawCsvRows = new ArrayList<>();
        for (Map<String, Object> r : rawRows) {
            rawCsvRows.add(List.of(r.get("threads"), r.get("batchSize"), r.get("elapsedMs"),
                    r.get("throughputRecPerSec"), r.get("peakWorkingSetBytes")));
        }
        ResultWriter.writeCsv(new File(outDir, "spark_scaling_raw.csv"), rawHeader, rawCsvRows);

        List<String> aggHeader = List.of("threads", "timeMeanMs", "timeStdMs", "throughputMean", "throughputStd",
                "speedup", "efficiencyPct", "peakMemMeanMB", "peakMemStdMB");
        List<List<Object>> aggCsvRows = new ArrayList<>();
        for (Map<String, Object> r : aggregated) {
            aggCsvRows.add(List.of(r.get("threads"), r.get("timeMeanMs"), r.get("timeStdMs"),
                    r.get("throughputMean"), r.get("throughputStd"), r.get("speedup"), r.get("efficiencyPct"),
                    r.get("peakMemMeanMB"), r.get("peakMemStdMB")));
        }
        ResultWriter.writeCsv(new File(outDir, "spark_scaling_benchmark.csv"), aggHeader, aggCsvRows);
        ResultWriter.writeJson(new File(outDir, "spark_scaling_benchmark.json"), aggregated);

        writeMarkdownTable(aggregated, new File(outDir, "spark_scaling_report_table.md"));
    }

    private static List<Map<String, Object>> aggregate(Map<Integer, List<Map<String, Object>>> runsByThreads) {
        List<Map<String, Object>> aggregated = new ArrayList<>();
        Double baselineTimeMean = null;

        for (int threads : THREAD_LEVELS) {
            List<Map<String, Object>> runs = runsByThreads.get(threads);
            if (runs == null || runs.isEmpty()) {
                continue;
            }
            double[] times = runs.stream().mapToDouble(r -> ((Number) r.get("elapsedMs")).doubleValue()).toArray();
            double[] throughputs = runs.stream().mapToDouble(r -> ((Number) r.get("throughputRecPerSec")).doubleValue()).toArray();
            double[] peaksMB = runs.stream()
                    .mapToDouble(r -> ((Number) r.get("peakWorkingSetBytes")).doubleValue() / (1024.0 * 1024.0))
                    .toArray();

            double timeMean = mean(times);
            double timeStd = std(times, timeMean);
            double thrMean = mean(throughputs);
            double thrStd = std(throughputs, thrMean);
            double peakMean = mean(peaksMB);
            double peakStd = std(peaksMB, peakMean);

            if (baselineTimeMean == null) {
                baselineTimeMean = timeMean;
            }
            double speedup = baselineTimeMean / timeMean;
            double efficiency = speedup / threads * 100.0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("threads", threads);
            row.put("timeMeanMs", round(timeMean));
            row.put("timeStdMs", round(timeStd));
            row.put("throughputMean", round(thrMean));
            row.put("throughputStd", round(thrStd));
            row.put("speedup", round(speedup));
            row.put("efficiencyPct", round(efficiency));
            row.put("peakMemMeanMB", round(peakMean));
            row.put("peakMemStdMB", round(peakStd));
            aggregated.add(row);
        }
        return aggregated;
    }

    private static void writeMarkdownTable(List<Map<String, Object>> aggregated, File outFile) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# Ket qua Spark Local Scaling (").append(REPEATS).append(" lan chay x 1 epoch)\n\n");
        sb.append("| Cau hinh Worker | Thoi gian thuc thi TN (ms) | Thong luong (rec/s) | Toc do gia tang | Hieu suat co gian | Dinh bo nho VmHWM (MB) |\n");
        sb.append("| --- | --- | --- | --- | --- | --- |\n");
        for (Map<String, Object> r : aggregated) {
            int threads = (int) r.get("threads");
            String speedupStr = threads == THREAD_LEVELS[0]
                    ? formatVN((double) r.get("speedup"), 2) + " x (Co so)"
                    : formatVN((double) r.get("speedup"), 2) + " x";
            sb.append("| local[").append(threads).append("] | ")
                    .append(formatVN((double) r.get("timeMeanMs"), 1)).append(" +/- ").append(formatVN((double) r.get("timeStdMs"), 1)).append(" | ")
                    .append(formatVN((double) r.get("throughputMean"), 0)).append(" +/- ").append(formatVN((double) r.get("throughputStd"), 0)).append(" | ")
                    .append(speedupStr).append(" | ")
                    .append(formatVN((double) r.get("efficiencyPct"), 1)).append("% | ")
                    .append(formatVN((double) r.get("peakMemMeanMB"), 1)).append(" +/- ").append(formatVN((double) r.get("peakMemStdMB"), 1)).append(" |\n");
        }
        try (var writer = new java.io.FileWriter(outFile, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(sb.toString());
        }
        System.out.println(sb);
    }

    private static String formatVN(double value, int decimals) {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimals > 0) {
            pattern.append(".");
            for (int i = 0; i < decimals; i++) pattern.append("0");
        }
        java.text.DecimalFormat df = new java.text.DecimalFormat(pattern.toString(), symbols);
        return df.format(value);
    }

    private static double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return values.length == 0 ? 0 : sum / values.length;
    }

    private static double std(double[] values, double mean) {
        if (values.length < 2) return 0.0;
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / (values.length - 1));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Spark 3.3.0 (Scala 2.12) + JDK17 gap loi "SerializedLambda.readResolve: too many arguments"
     * khi deserialize closure trong local[N>1] mode. Phuong an du phong (xem plan/README): chay
     * SparkTrainingRunner (module `training`, bien dich rieng xuong bytecode Java 11) bang mot
     * JVM JDK11 tach biet, thay vi JVM JDK17 dang chay benchmark module nay.
     */
    private static final String JDK11_HOME_PROPERTY = "jdk11.home";
    private static final String DEFAULT_JDK11_HOME =
            "C:\\Program Files\\Eclipse Adoptium\\jdk-11.0.32.101-hotspot";

    private static Map<String, Object> runSparkTrainingSubprocess(String csvPath, int threads,
                                                                   int epochs, int batchSizePerWorker) throws Exception {
        String jdk11Home = System.getProperty(JDK11_HOME_PROPERTY, DEFAULT_JDK11_HOME);
        String javaBin = jdk11Home + File.separator + "bin" + File.separator + "java";
        String classpath = resolveRuntimeClasspath();

        ProcessBuilder pb = new ProcessBuilder(
                javaBin, "-cp", classpath,
                "-Dorg.bytedeco.javacpp.maxbytes=8G",
                "vn.huit.dl4j.training.SparkTrainingRunner",
                csvPath, String.valueOf(threads), String.valueOf(epochs), String.valueOf(batchSizePerWorker)
        );
        pb.environment().put("OMP_NUM_THREADS", "1");
        pb.environment().put("OPENBLAS_NUM_THREADS", "1");
        pb.environment().put("MKL_NUM_THREADS", "1");
        pb.redirectErrorStream(true);

        Process process = pb.start();
        long pid = process.pid();

        AtomicLong peakWorkingSet = new AtomicLong(0);
        Thread monitor = new Thread(() -> {
            while (process.isAlive()) {
                try {
                    long ws = queryWorkingSet(pid);
                    peakWorkingSet.updateAndGet(cur -> Math.max(cur, ws));
                } catch (Exception ignored) {
                    // process co the vua thoat giua luc query, bo qua mau nay
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        monitor.setDaemon(true);
        monitor.start();

        Map<String, Object> parsedResult = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = RESULT_PATTERN.matcher(line);
                if (matcher.find()) {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("threads", Integer.parseInt(matcher.group(1)));
                    r.put("batchSize", Integer.parseInt(matcher.group(2)));
                    r.put("elapsedMs", Long.parseLong(matcher.group(3)));
                    r.put("throughputRecPerSec", Double.parseDouble(matcher.group(4)));
                    parsedResult = r;
                } else if (line.contains("ERROR") || line.contains("Exception")) {
                    System.out.println("  [subprocess] " + line);
                }
            }
        }
        process.waitFor();
        monitor.join(1000);

        if (parsedResult != null) {
            parsedResult.put("peakWorkingSetBytes", peakWorkingSet.get());
        }
        return parsedResult;
    }

    private static long queryWorkingSet(long pid) throws Exception {
        ProcessBuilder psb = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                "(Get-Process -Id " + pid + " -ErrorAction SilentlyContinue).WorkingSet64");
        psb.redirectErrorStream(true);
        Process p = psb.start();
        String out;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            out = r.readLine();
        }
        p.waitFor();
        if (out == null || out.isBlank()) {
            return 0;
        }
        return Long.parseLong(out.trim());
    }

    /**
     * exec-maven-plugin's `exec:java` goal chay class trong mot URLClassLoader rieng va
     * KHONG cap nhat System.getProperty("java.class.path") cho khop voi dependency classpath
     * thuc te (no van chi tro ve classpath goc cua tien trinh Maven). Vi vay phai lay danh
     * sach URL truc tiep tu ClassLoader dang nap SparkScalingBenchmark de dung lam classpath
     * cho tien trinh con JDK11.
     */
    private static String resolveRuntimeClasspath() {
        ClassLoader cl = SparkScalingBenchmark.class.getClassLoader();
        if (cl instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) cl).getURLs();
            String fromClassLoader = java.util.Arrays.stream(urls)
                    .map(url -> new File(url.getPath()).getAbsolutePath())
                    .collect(Collectors.joining(File.pathSeparator));
            if (!fromClassLoader.isBlank()) {
                return fromClassLoader;
            }
        }
        return System.getProperty("java.class.path");
    }
}
