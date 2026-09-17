package vn.huit.dl4j.training;

import org.deeplearning4j.datasets.iterator.utilty.ListDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;

import java.io.File;

/**
 * Train mot MLP thuan DL4J (khong Spark) tren creditcard.csv va luu model + normalizer
 * de dung chung cho benchmark va API serving.
 *
 * Usage: mvn -pl training exec:java -Dexec.mainClass=vn.huit.dl4j.training.LocalTrainingRunner
 *        -Dexec.args="<path-to-creditcard.csv> <epochs> <batchSize>"
 */
public final class LocalTrainingRunner {

    public static void main(String[] args) throws Exception {
        File csvFile = args.length > 0 ? new File(args[0]) : new File("data/creditcard.csv");
        int epochs = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        int batchSize = args.length > 2 ? Integer.parseInt(args[2]) : 32;

        System.out.println("Loading dataset from: " + csvFile.getAbsolutePath());
        DataSet fullDataSet = DataPipeline.loadFullDataset(csvFile);

        SplitTestAndTrain split = DataPipeline.splitTrainTest(fullDataSet, 0.8, 42L);
        DataSet trainData = split.getTrain();
        DataSet testData = split.getTest();

        NormalizerStandardize normalizer = DataPipeline.fitNormalizer(trainData);
        normalizer.transform(trainData);
        normalizer.transform(testData);

        MultiLayerNetwork model = ModelFactory.buildDefaultModel(42L);

        DataSetIterator trainIter = DataPipeline.asyncIterator(
                new ListDataSetIterator<>(trainData.asList(), batchSize));

        System.out.println("Training for " + epochs + " epochs, batchSize=" + batchSize);
        long start = System.nanoTime();
        for (int i = 0; i < epochs; i++) {
            trainIter.reset();
            model.fit(trainIter);
            System.out.println("Epoch " + (i + 1) + "/" + epochs + " done");
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("Training completed in " + elapsedMs + " ms");

        Evaluation eval = model.evaluate(
                new ListDataSetIterator<>(testData.asList(), batchSize));
        System.out.println(eval.stats());

        File modelsDir = new File("training/models");
        modelsDir.mkdirs();
        File modelFile = new File(modelsDir, "fraud_mlp.zip");
        ModelSerializer.writeModel(model, modelFile, true, normalizer);
        System.out.println("Model + normalizer saved to: " + modelFile.getAbsolutePath());
    }
}
