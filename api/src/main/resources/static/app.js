const FEATURE_NAMES = ["Time", ...Array.from({length: 28}, (_, i) => "V" + (i + 1)), "Amount"];

document.querySelectorAll(".tab-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
        document.querySelectorAll(".tab-panel").forEach(p => p.classList.remove("active"));
        btn.classList.add("active");
        document.getElementById(btn.dataset.tab).classList.add("active");
        if (btn.dataset.tab === "benchmark") {
            loadCharts();
        }
    });
});

async function fetchSample(classLabel) {
    const res = await fetch(`/api/sample?classLabel=${classLabel}`);
    const features = await res.json();
    document.getElementById("featuresBox").value = features.join(", ");
}

document.getElementById("btnSampleNormal").addEventListener("click", () => fetchSample(0));
document.getElementById("btnSampleFraud").addEventListener("click", () => fetchSample(1));

document.getElementById("btnPredict").addEventListener("click", async () => {
    const raw = document.getElementById("featuresBox").value.trim();
    const features = raw.split(",").map(s => parseFloat(s.trim()));
    const resultBox = document.getElementById("result");

    if (features.length !== 30 || features.some(Number.isNaN)) {
        resultBox.textContent = "Vui lòng nhập đúng 30 giá trị số, cách nhau bởi dấu phẩy.";
        resultBox.className = "result";
        return;
    }

    resultBox.textContent = "Đang dự đoán...";
    resultBox.className = "result";

    try {
        const res = await fetch("/api/predict", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({features})
        });
        const data = await res.json();
        const isFraud = data.predictedClass === 1;
        resultBox.className = "result " + (isFraud ? "fraud" : "normal");
        resultBox.innerHTML = `Kết quả: <b>${isFraud ? "FRAUD (gian lận)" : "NORMAL (bình thường)"}</b><br>`
            + `Xác suất fraud: ${(data.probabilityFraud * 100).toFixed(2)}%<br>`
            + `Thời gian phản hồi: ${data.latencyMs.toFixed(2)} ms`;
    } catch (e) {
        resultBox.textContent = "Lỗi gọi API: " + e;
        resultBox.className = "result";
    }
});

let charts = {};

async function loadCharts() {
    const res = await fetch("/api/benchmark/results");
    const data = await res.json();

    renderWorkspaceChart(data.workspace_benchmark || []);
    renderSparkChart(data.spark_scaling_benchmark || []);
    renderLatencyChart(
        (data.inference_latency_inprocess || []),
        (data.inference_latency_remote_springboot || []),
        (data.inference_latency_remote_fastapi || [])
    );
}

function upsertChart(key, ctx, config) {
    if (charts[key]) {
        charts[key].destroy();
    }
    charts[key] = new Chart(ctx, config);
}

function renderWorkspaceChart(rows) {
    const ctx = document.getElementById("chartWorkspace");
    upsertChart("workspace", ctx, {
        type: "bar",
        data: {
            labels: rows.map(r => r.mode),
            datasets: [{
                label: "Thời gian train (ms)",
                data: rows.map(r => r.elapsedMs),
                backgroundColor: "#5b8cff"
            }]
        },
        options: {responsive: true}
    });
}

function renderSparkChart(rows) {
    const ctx = document.getElementById("chartSpark");
    const sorted = [...rows].sort((a, b) => a.threads - b.threads);
    upsertChart("spark", ctx, {
        type: "line",
        data: {
            labels: sorted.map(r => r.threads + " threads"),
            datasets: [{
                label: "Thời gian train (ms)",
                data: sorted.map(r => r.elapsedMs),
                borderColor: "#5b8cff",
                fill: false
            }, {
                label: "Throughput (rec/s)",
                data: sorted.map(r => r.throughputRecPerSec),
                borderColor: "#4cd97b",
                fill: false,
                yAxisID: "y1"
            }]
        },
        options: {
            responsive: true,
            scales: {y1: {position: "right"}}
        }
    });
}

function renderLatencyChart(inProcessRows, springbootRows, fastapiRows) {
    const ctx = document.getElementById("chartLatency");
    const all = [...inProcessRows, ...springbootRows, ...fastapiRows];
    const labels = [...new Set(all.map(r => "B=" + r.batchSize))]
        .sort((a, b) => parseInt(a.replace("B=", "")) - parseInt(b.replace("B=", "")));

    function seriesFor(rows, metric) {
        return labels.map(label => {
            const b = parseInt(label.replace("B=", ""));
            const row = rows.find(r => r.batchSize === b);
            return row ? row[metric] : null;
        });
    }

    upsertChart("latency", ctx, {
        type: "bar",
        data: {
            labels,
            datasets: [
                {label: "In-process p50 (ms)", data: seriesFor(inProcessRows, "p50Ms"), backgroundColor: "#5b8cff"},
                {label: "Spring Boot p50 (ms)", data: seriesFor(springbootRows, "p50Ms"), backgroundColor: "#ff6b6b"},
                {label: "FastAPI p50 (ms)", data: seriesFor(fastapiRows, "p50Ms"), backgroundColor: "#4cd97b"}
            ]
        },
        options: {responsive: true, scales: {y: {type: "logarithmic"}}}
    });
}
