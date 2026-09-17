package vn.huit.dl4j.api.dto;

public class PredictRequest {
    private double[] features;

    public double[] getFeatures() {
        return features;
    }

    public void setFeatures(double[] features) {
        this.features = features;
    }
}
