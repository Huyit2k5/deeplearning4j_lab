"""
Truc quan hoa so sanh 3 tang do tre suy luan (kich ban 3): In-Process (DL4J),
Remote Spring Boot (Java), Remote FastAPI (Python), quet qua batch size {1,8,16,32,64}.

Doc tu results/latency_comparison.csv (sinh boi build_latency_comparison.py), ve 2 bieu do:
  1. p50 latency theo batch size, 3 duong (log scale vi in-process << remote)
  2. Throughput theo batch size, 3 duong (log scale)

Usage: python scripts/plot_latency_comparison.py
Output: results/charts/latency_p50_by_batch.png
        results/charts/latency_throughput_by_batch.png
"""
import csv
from pathlib import Path

import matplotlib.pyplot as plt

PROJECT_ROOT = Path(__file__).resolve().parent.parent
RESULTS_CSV = PROJECT_ROOT / "results" / "latency_comparison.csv"
OUT_DIR = PROJECT_ROOT / "results" / "charts"

COLOR = {"inProcess": "#2a78d6", "springboot": "#eb6834", "fastapi": "#1baf7a"}
LABEL = {"inProcess": "In-Process (DL4J)", "springboot": "Remote - Spring Boot", "fastapi": "Remote - FastAPI"}


def load_rows():
    with open(RESULTS_CSV, newline="", encoding="utf-8-sig") as f:
        rows = [
            {
                "batchSize": int(r["batchSize"]),
                "inProcessP50": float(r["inProcessP50"]),
                "inProcessThroughput": float(r["inProcessThroughput"]),
                "springbootP50": float(r["springbootP50"]),
                "springbootThroughput": float(r["springbootThroughput"]),
                "fastapiP50": float(r["fastapiP50"]),
                "fastapiThroughput": float(r["fastapiThroughput"]),
            }
            for r in csv.DictReader(f)
        ]
    rows.sort(key=lambda r: r["batchSize"])
    return rows


def plot_metric(rows, field_suffix, ylabel, title, out_path, log_scale=True):
    fig, ax = plt.subplots(figsize=(7.5, 4.8), dpi=150)
    batch_sizes = [r["batchSize"] for r in rows]

    for key in ["inProcess", "springboot", "fastapi"]:
        values = [r[f"{key}{field_suffix}"] for r in rows]
        ax.plot(batch_sizes, values, marker="o", markersize=5, linewidth=2,
                color=COLOR[key], label=LABEL[key])

    ax.set_xlabel("Batch size (B)")
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.set_xscale("log", base=2)
    ax.set_xticks(batch_sizes)
    ax.set_xticklabels([str(b) for b in batch_sizes])
    if log_scale:
        ax.set_yscale("log")
    ax.grid(True, which="both", linewidth=0.5, alpha=0.4)
    ax.legend(frameon=False)
    fig.tight_layout()
    fig.savefig(out_path)
    plt.close(fig)
    print(f"Da luu: {out_path}")


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    rows = load_rows()

    plot_metric(rows, "P50", "p50 latency (ms, thang log)",
                "Do tre p50 theo batch size - In-Process vs Remote (Spring Boot / FastAPI)",
                OUT_DIR / "latency_p50_by_batch.png")

    plot_metric(rows, "Throughput", "Thong luong (mau/s, thang log)",
                "Thong luong theo batch size - In-Process vs Remote (Spring Boot / FastAPI)",
                OUT_DIR / "latency_throughput_by_batch.png")


if __name__ == "__main__":
    main()
