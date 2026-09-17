package vn.huit.dl4j.training;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * MLP nho cho bai toan fraud detection tren dataset creditcard.csv
 * (30 input -> 32 -> 16 -> 2 output, softmax).
 */
public final class ModelFactory {

    private ModelFactory() {
    }

    public static MultiLayerNetwork buildModel(long seed, WorkspaceMode trainingWorkspaceMode,
                                                WorkspaceMode inferenceWorkspaceMode) {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(1e-3))
                .trainingWorkspaceMode(trainingWorkspaceMode)
                .inferenceWorkspaceMode(inferenceWorkspaceMode)
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(DataPipeline.NUM_FEATURES)
                        .nOut(32)
                        .activation(Activation.RELU)
                        .build())
                .layer(new DenseLayer.Builder()
                        .nIn(32)
                        .nOut(16)
                        .activation(Activation.RELU)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nIn(16)
                        .nOut(DataPipeline.NUM_CLASSES)
                        .activation(Activation.SOFTMAX)
                        .build())
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        return model;
    }

    public static MultiLayerNetwork buildDefaultModel(long seed) {
        return buildModel(seed, WorkspaceMode.ENABLED, WorkspaceMode.ENABLED);
    }
}
