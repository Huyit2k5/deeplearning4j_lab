"""
Truc quan hoa ket qua SparkScalingBenchmark (kich ban 2: local[1,2,4,8]) bang matplotlib.

Doc truc tiep tu results/spark_scaling_benchmark.csv (bang mean +/- std da tong hop san
boi SparkScalingBenchmark.java qua 3 lan chay/cau hinh), ve 4 bieu do:
  1. Thoi gian thuc thi theo cau hinh Worker (bar chart, error bar = std)
  2. Toc do gia tang thuc te so voi toc do gia tang ly tuong (speedup = so luong)
  3. Hieu suat co gian (%) theo cau hinh Worker
  4. Dinh bo nho VmHWM theo cau hinh Worker (bar chart, error bar = std)

Usage:
    python scripts/plot_spark_scaling_results.py

Output: results/charts/spark_time_by_config.png
        results/charts/spark_speedup_vs_ideal.png
        results/charts/spark_efficiency.png
        results/charts/spark_peak_memory.png
"""
import csv
from pathlib import Path

import matplotlib.pyplot as plt

PROJECT_ROOT = Path(__file__).resolve().parent.parent
RESULTS_CSV = PROJECT_ROOT / "results" / "spark_scaling_benchmark.csv"
OUT_DIR = PROJECT_ROOT / "results" / "charts"

BAR_COLOR = "#2a78d6"
IDEAL_COLOR = "#898781"
ACTUAL_COLOR = "#eb6834"


def load_rows():
    with open(RESULTS_CSV, newline="", encoding="utf-8-sig") as f:
        rows = [
            {
                "threads": int(r["threads"]),
                "timeMeanMs": float(r["timeMeanMs"]),
                "timeStdMs": float(r["timeStdMs"]),
                "throughputMean": float(r["throughputMean"]),
                "throughputStd": float(r["throughputStd"]),
                "speedup": float(r["speedup"]),
                "efficiencyPct": float(r["efficiencyPct"]),
                "peakMemMeanMB": float(r["peakMemMeanMB"]),
                "peakMemStdMB": float(r["peakMemStdMB"]),
            }
            for r in csv.DictReader(f)
        ]
    rows.sort(key=lambda r: r["threads"])
    return rows


def plot_time_by_config(rows, out_path):
    labels = [f"local[{r['threads']}]" for r in rows]
    means = [r["timeMeanMs"] / 1000.0 for r in rows]
    stds = [r["timeStdMs"] / 1000.0 for r in rows]

    fig, ax = plt.subplots(figsize=(7, 4.5), dpi=150)
    bars = ax.bar(labels, means, yerr=stds, capsize=5, color=BAR_COLOR)
    for bar, mean, std in zip(bars, means, stds):
        ax.text(bar.get_x() + bar.get_width() / 2, mean + std + max(means) * 0.03,
                f"{mean:.1f}s", ha="center", va="bottom", fontsize=9)

    ax.set_ylabel("Thoi gian thuc thi (s)")
    ax.set_ylim(0, max(m + s for m, s in zip(means, stds)) * 1.18)
    ax.set_title("Thoi gian thuc thi theo cau hinh Worker\n(mean +/- std, 3 lan chay x 1 epoch)")
    ax.grid(True, axis="y", linewidth=0.5, alpha=0.5)
    fig.tight_layout()
    fig.savefig(out_path)
    plt.close(fig)
    print(f"Da luu: {out_path}")


def plot_speedup_vs_ideal(rows, out_path):
    threads = [r["threads"] for r in rows]
    actual = [r["speedup"] for r in rows]
    ideal = threads  # speedup ly tuong = so luong

    fig, ax = plt.subplots(figsize=(7, 4.5), dpi=150)
    ax.plot(threads, ideal, linestyle="--", color=IDEAL_COLOR, marker="o", markersize=4,
            label="Ly tuong (speedup = so luong)")
    ax.plot(threads, actual, color=ACTUAL_COLOR, marker="o", markersize=5, linewidth=2,
            label="Thuc te")

    for x, y in zip(threads, actual):
        ax.annotate(f"{y:.2f}x", (x, y), textcoords="offset points", xytext=(0, 8),
                    ha="center", fontsize=9)

    ax.set_xlabel("So luong Worker (N)")
    ax.set_ylabel("Toc do gia tang (speedup)")
    ax.set_title("Toc do gia tang: thuc te vs ly tuong")
    ax.set_xticks(threads)
    ax.grid(True, linewidth=0.5, alpha=0.5)
    ax.legend(frameon=False)
    fig.tight_layout()
    fig.savefig(out_path)
    plt.close(fig)
    print(f"Da luu: {out_path}")


def plot_efficiency(rows, out_path):
    labels = [f"local[{r['threads']}]" for r in rows]
    values = [r["efficiencyPct"] for r in rows]

    fig, ax = plt.subplots(figsize=(7, 4.5), dpi=150)
    bars = ax.bar(labels, values, color=ACTUAL_COLOR)
    for bar, v in zip(bars, values):
        ax.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 1.5,
                f"{v:.1f}%", ha="center", va="bottom", fontsize=9)

    ax.axhline(100, color=IDEAL_COLOR, linestyle="--", linewidth=1)
    ax.set_ylabel("Hieu suat co gian (%)")
    ax.set_ylim(0, 110)
    ax.set_title("Hieu suat co gian theo cau hinh Worker")
    ax.grid(True, axis="y", linewidth=0.5, alpha=0.5)
    fig.tight_layout()
    fig.savefig(out_path)
    plt.close(fig)
    print(f"Da luu: {out_path}")


def plot_peak_memory(rows, out_path):
    labels = [f"local[{r['threads']}]" for r in rows]
    means = [r["peakMemMeanMB"] for r in rows]
    stds = [r["peakMemStdMB"] for r in rows]

    fig, ax = plt.subplots(figsize=(7, 4.5), dpi=150)
    bars = ax.bar(labels, means, yerr=stds, capsize=5, color=BAR_COLOR)
    for bar, mean, std in zip(bars, means, stds):
        ax.text(bar.get_x() + bar.get_width() / 2, mean + std + max(means) * 0.02,
                f"{mean:.0f}MB", ha="center", va="bottom", fontsize=9)

    ax.set_ylabel("Dinh bo nho VmHWM (MB)")
    ax.set_ylim(0, max(m + s for m, s in zip(means, stds)) * 1.15)
    ax.set_title("Dinh bo nho theo cau hinh Worker\n(mean +/- std, 3 lan chay)")
    ax.grid(True, axis="y", linewidth=0.5, alpha=0.5)
    fig.tight_layout()
    fig.savefig(out_path)
    plt.close(fig)
    print(f"Da luu: {out_path}")


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    rows = load_rows()

    plot_time_by_config(rows, OUT_DIR / "spark_time_by_config.png")
    plot_speedup_vs_ideal(rows, OUT_DIR / "spark_speedup_vs_ideal.png")
    plot_efficiency(rows, OUT_DIR / "spark_efficiency.png")
    plot_peak_memory(rows, OUT_DIR / "spark_peak_memory.png")


if __name__ == "__main__":
    main()
