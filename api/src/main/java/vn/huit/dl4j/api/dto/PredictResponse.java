package vn.huit.dl4j.api.dto;

public class PredictResponse {
    private int predictedClass;
    private double probabilityFraud;
    private double latencyMs;

    public PredictResponse(int predictedClass, double probabilityFraud, double latencyMs) {
        this.predictedClass = predictedClass;
        this.probabilityFraud = probabilityFraud;
        this.latencyMs = latencyMs;
    }

    public int getPredictedClass() {
        return predictedClass;
    }

    public double getProbabilityFraud() {
        return probabilityFraud;
    }

    public double getLatencyMs() {
        return latencyMs;
    }
}
