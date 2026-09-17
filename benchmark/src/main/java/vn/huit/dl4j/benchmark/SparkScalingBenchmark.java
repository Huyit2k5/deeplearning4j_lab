package vn.huit.dl4j.benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Kich ban 2: chay vn.huit.dl4j.training.SparkTrainingRunner trong mot JVM RIENG cho
 * moi muc so luong thread (1,2,4,6,8) de tranh nhieu JIT/GC cheo giua cac lan do,
 * roi gom ket qua (SPARK_SCALING_RESULT,... in ra stdout cua tien trinh con) thanh CSV/JSON.
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
    private static final int[] THREAD_LEVELS = {1, 2, 4, 6, 8};

    public static void main(String[] args) throws Exception {
        String csvPath = args.length > 0 ? args[0] : "data/creditcard.csv";
        int epochs = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        int batchSizePerWorker = args.length > 2 ? Integer.parseInt(args[2]) : 32;

        List<Map<String, Object>> results = new ArrayList<>();
        for (int threads : THREAD_LEVELS) {
            System.out.println("=== Running Spark local[" + threads + "] ===");
            Map<String, Object> result = runSparkTrainingSubprocess(csvPath, threads, epochs, batchSizePerWorker);
            if (result != null) {
                results.add(result);
                System.out.println(result);
            } else {
                System.out.println("WARNING: no result parsed for threads=" + threads);
            }
        }

        File outDir = new File("results");
        List<String> header = List.of("threads", "batchSize", "elapsedMs", "throughputRecPerSec");
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> r : results) {
            rows.add(List.of(r.get("threads"), r.get("batchSize"), r.get("elapsedMs"), r.get("throughputRecPerSec")));
        }
        ResultWriter.writeCsv(new File(outDir, "spark_scaling_benchmark.csv"), header, rows);
        ResultWriter.writeJson(new File(outDir, "spark_scaling_benchmark.json"), results);
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
        Map<String, Object> parsedResult = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = RESULT_PATTERN.matcher(line);
                if (matcher.find()) {
                    parsedResult = Map.of(
                            "threads", Integer.parseInt(matcher.group(1)),
                            "batchSize", Integer.parseInt(matcher.group(2)),
                            "elapsedMs", Long.parseLong(matcher.group(3)),
                            "throughputRecPerSec", Double.parseDouble(matcher.group(4))
                    );
                } else if (line.contains("ERROR") || line.contains("Exception")) {
                    System.out.println("  [subprocess] " + line);
                }
            }
        }
        process.waitFor();
        return parsedResult;
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
