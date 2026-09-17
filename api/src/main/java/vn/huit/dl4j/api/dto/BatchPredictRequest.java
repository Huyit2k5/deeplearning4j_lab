package vn.huit.dl4j.api.dto;

import java.util.List;

public class BatchPredictRequest {
    private List<PredictRequest> records;

    public List<PredictRequest> getRecords() {
        return records;
    }

    public void setRecords(List<PredictRequest> records) {
        this.records = records;
    }
}
