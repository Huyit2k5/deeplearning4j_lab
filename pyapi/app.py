"""
FastAPI service phuc vu du doan fraud detection bang PyTorch (CPU), dung de do do tre
remote serving qua Python stack, doi chieu voi Spring Boot (Java) va DL4J in-process.

Usage:
    cd pyapi
    pip install -r requirements.txt
    uvicorn app:app --host 0.0.0.0 --port 8000
"""
import time
from typing import List

import torch
from fastapi import FastAPI
from pydantic import BaseModel

from model import load_model

app = FastAPI(title="Fraud Detection FastAPI")
MODEL = load_model()


class PredictRequest(BaseModel):
    features: List[float]


class BatchPredictRequest(BaseModel):
    records: List[PredictRequest]


class PredictResponse(BaseModel):
    predictedClass: int
    probabilityFraud: float
    latencyMs: float


def _predict_batch(feature_rows: List[List[float]]) -> List[PredictResponse]:
    start = time.perf_counter()
    with torch.no_grad():
        x = torch.tensor(feature_rows, dtype=torch.float32)
        output = MODEL(x)
    latency_ms = (time.perf_counter() - start) * 1000.0

    responses = []
    for row in output:
        prob_fraud = row[1].item()
        predicted_class = 1 if prob_fraud >= 0.5 else 0
        responses.append(PredictResponse(
            predictedClass=predicted_class,
            probabilityFraud=prob_fraud,
            latencyMs=latency_ms,
        ))
    return responses


@app.post("/api/predict", response_model=PredictResponse)
def predict(request: PredictRequest):
    return _predict_batch([request.features])[0]


@app.post("/api/predict/batch", response_model=List[PredictResponse])
def predict_batch(request: BatchPredictRequest):
    feature_rows = [r.features for r in request.records]
    return _predict_batch(feature_rows)


@app.get("/api/health")
def health():
    return {"status": "ok"}
