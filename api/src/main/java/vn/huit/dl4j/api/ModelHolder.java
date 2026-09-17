package vn.huit.dl4j.api;

import javax.annotation.PostConstruct;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Load model + normalizer MOT LAN duy nhat luc Spring Boot khoi dong, giu singleton
 * trong bo nho de cac request /predict khong phai load lai (dam bao do do tre cong bang
 * khi so sanh voi benchmark in-process trong module benchmark).
 */
@Component
public class ModelHolder {

    @Value("${dl4j.model.path:training/models/fraud_mlp.zip}")
    private String modelPath;

    private MultiLayerNetwork model;
    private NormalizerStandardize normalizer;

    @PostConstruct
    public void load() throws Exception {
        File modelFile = new File(modelPath);
        this.model = ModelSerializer.restoreMultiLayerNetwork(modelFile);
        this.normalizer = ModelSerializer.restoreNormalizerFromFile(modelFile);
    }

    public PredictionResult predict(double[] features) {
        INDArray input = Nd4j.create(features).reshape(1, features.length);
        return predictBatch(input, 1)[0];
    }

    public PredictionResult[] predictBatch(double[][] featureRows) {
        int rows = featureRows.length;
        int cols = featureRows[0].length;
        INDArray input = Nd4j.create(rows, cols);
        for (int i = 0; i < rows; i++) {
            input.putRow(i, Nd4j.create(featureRows[i]));
        }
        return predictBatch(input, rows);
    }

    private PredictionResult[] predictBatch(INDArray rawInput, int rows) {
        if (normalizer != null) {
            normalizer.transform(rawInput);
        }
        INDArray output = model.output(rawInput);
        PredictionResult[] results = new PredictionResult[rows];
        for (int i = 0; i < rows; i++) {
            double probFraud = output.getDouble(i, 1);
            int predictedClass = probFraud >= 0.5 ? 1 : 0;
            results[i] = new PredictionResult(predictedClass, probFraud);
        }
        return results;
    }

    public record PredictionResult(int predictedClass, double probabilityFraud) {
    }
}
