package vn.huit.dl4j.api;

import org.springframework.web.bind.annotation.*;
import vn.huit.dl4j.api.dto.BatchPredictRequest;
import vn.huit.dl4j.api.dto.PredictRequest;
import vn.huit.dl4j.api.dto.PredictResponse;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PredictController {

    private final ModelHolder modelHolder;

    public PredictController(ModelHolder modelHolder) {
        this.modelHolder = modelHolder;
    }

    @PostMapping("/predict")
    public PredictResponse predict(@RequestBody PredictRequest request) {
        long start = System.nanoTime();
        ModelHolder.PredictionResult result = modelHolder.predict(request.getFeatures());
        double latencyMs = (System.nanoTime() - start) / 1_000_000.0;
        return new PredictResponse(result.predictedClass(), result.probabilityFraud(), latencyMs);
    }

    @PostMapping("/predict/batch")
    public List<PredictResponse> predictBatch(@RequestBody BatchPredictRequest request) {
        long start = System.nanoTime();
        double[][] rows = request.getRecords().stream()
                .map(PredictRequest::getFeatures)
                .toArray(double[][]::new);
        ModelHolder.PredictionResult[] results = modelHolder.predictBatch(rows);
        double latencyMs = (System.nanoTime() - start) / 1_000_000.0;

        List<PredictResponse> responses = new ArrayList<>();
        for (ModelHolder.PredictionResult r : results) {
            responses.add(new PredictResponse(r.predictedClass(), r.probabilityFraud(), latencyMs));
        }
        return responses;
    }
}
