package vn.huit.dl4j.training;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.spark.api.TrainingMaster;
import org.deeplearning4j.spark.impl.multilayer.SparkDl4jMultiLayer;
import org.deeplearning4j.spark.impl.paramavg.ParameterAveragingTrainingMaster;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

import java.io.File;
import java.util.List;

/**
 * Train MLP tren Spark local[N] de phuc vu kich ban benchmark "Spark Local Scaling".
 *
 * Usage: mvn -pl training exec:java -Dexec.mainClass=vn.huit.dl4j.training.SparkTrainingRunner
 *        -Dexec.args="<csvPath> <numThreads> <epochs> <batchSizePerWorker>"
 */
public final class SparkTrainingRunner {

    public static void main(String[] args) throws Exception {
        File csvFile = args.length > 0 ? new File(args[0]) : new File("data/creditcard.csv");
        int numThreads = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        int epochs = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        int batchSizePerWorker = args.length > 3 ? Integer.parseInt(args[3]) : 32;

        SparkConf sparkConf = new SparkConf();
        sparkConf.setMaster("local[" + numThreads + "]");
        sparkConf.setAppName("DL4J-Spark-Scaling-Benchmark");
        sparkConf.set("spark.executor.memory", "4g");
        sparkConf.set("spark.executor.memoryOverhead", "8g");
        sparkConf.set("spark.executor.extraJavaOptions", "-Dorg.bytedeco.javacpp.maxbytes=8G");

        try (JavaSparkContext sc = new JavaSparkContext(sparkConf)) {
            DataSet fullDataSet = DataPipeline.loadFullDataset(csvFile);
            SplitTestAndTrain split = DataPipeline.splitTrainTest(fullDataSet, 0.8, 42L);
            DataSet trainData = split.getTrain();

            NormalizerStandardize normalizer = DataPipeline.fitNormalizer(trainData);
            normalizer.transform(trainData);

            List<DataSet> trainList = trainData.asList();
            JavaRDD<DataSet> trainRdd = sc.parallelize(trainList);

            TrainingMaster<?, ?> trainingMaster = new ParameterAveragingTrainingMaster.Builder(batchSizePerWorker)
                    .batchSizePerWorker(batchSizePerWorker)
                    .averagingFrequency(5)
                    .workerPrefetchNumBatches(2)
                    .build();

            MultiLayerNetwork model = ModelFactory.buildDefaultModel(42L);
            SparkDl4jMultiLayer sparkModel = new SparkDl4jMultiLayer(sc, model, trainingMaster);

            long start = System.nanoTime();
            for (int i = 0; i < epochs; i++) {
                sparkModel.fit(trainRdd);
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            double throughput = trainList.size() * epochs / (elapsedMs / 1000.0);
            System.out.println("SPARK_SCALING_RESULT,threads=" + numThreads
                    + ",batchSize=" + batchSizePerWorker
                    + ",elapsedMs=" + elapsedMs
                    + ",throughputRecPerSec=" + throughput);
        }
    }
}
