"""
Truc quan hoa ket qua WorkspaceBenchmark (kich ban 1: NONE vs ENABLED) bang matplotlib.

Doc du lieu tho tu results/workspace_runs/epoch_detail_run{1,2,3}.csv (thoi gian tung
epoch) + os_samples_run{1,2,3}.csv (Working Set - tuong duong VmRSS tren Windows, lay
mau moi 200ms), khop timestamp de lay VmRSS dung tai thoi diem ket thuc moi epoch, roi
ve 2 bieu do duong (mean +/- std qua 3 lan chay):
  1. Thoi gian train moi epoch (ms)
  2. VmRSS moi epoch (MB)

Usage:
    python scripts/plot_workspace_results.py
    (chay tu thu muc goc project, hoac tu dau cung duoc vi dung duong dan tuyet doi)

Output: results/charts/workspace_time_per_epoch.png
        results/charts/workspace_vmrss_per_epoch.png
"""
import csv
import statistics
from pathlib import Path

import matplotlib.pyplot as plt

PROJECT_ROOT = Path(__file__).resolve().parent.parent
RUNS_DIR = PROJECT_ROOT / "results" / "workspace_runs"
OUT_DIR = PROJECT_ROOT / "results" / "charts"

MODES = ["NONE", "ENABLED"]
RUN_IDS = [1, 2, 3]

COLOR = {"NONE": "#2a78d6", "ENABLED": "#eb6834"}


def load_epoch_detail(run_id):
    path = RUNS_DIR / f"epoch_detail_run{run_id}.csv"
    with open(path, newline="", encoding="utf-8-sig") as f:
        return [
            {
                "mode": row["mode"],
                "epoch": int(row["epoch"]),
                "elapsedMs": float(row["elapsedMs"]),
                "timestampMs": int(row["timestampMs"]),
            }
            for row in csv.DictReader(f)
        ]


def load_os_samples(run_id):
    path = RUNS_DIR / f"os_samples_run{run_id}.csv"
    with open(path, newline="", encoding="utf-8-sig") as f:
        return [
            {"timestampMs": int(row["TimestampMs"]), "workingSet64": float(row["WorkingSet64"])}
            for row in csv.DictReader(f)
        ]


def nearest_working_set_mb(samples, target_ms):
    best = min(samples, key=lambda s: abs(s["timestampMs"] - target_ms))
    return best["workingSet64"] / (1024 * 1024)


def collect_per_epoch(mode):
    """Tra ve dict: epoch -> (list thoi gian ms qua 3 run, list VmRSS MB qua 3 run)."""
    per_epoch_time = {}
    per_epoch_vmrss = {}
    for run_id in RUN_IDS:
        epoch_rows = [r for r in load_epoch_detail(run_id) if r["mode"] == mode]
        epoch_rows.sort(key=lambda r: r["epoch"])
        os_samples = load_os_samples(run_id)
        for r in epoch_rows:
            per_epoch_time.setdefault(r["epoch"], []).append(r["elapsedMs"])
            vmrss_mb = nearest_working_set_mb(os_samples, r["timestampMs"])
            per_epoch_vmrss.setdefault(r["epoch"], []).append(vmrss_mb)
    return per_epoch_time, per_epoch_vmrss


def mean_std(values):
    m = statistics.mean(values)
    s = statistics.stdev(values) if len(values) > 1 else 0.0
    return m, s


def plot_metric(data_by_mode, ylabel, title, out_path):
    fig, ax = plt.subplots(figsize=(8, 4.5), dpi=150)

    for mode in MODES:
        epochs = sorted(data_by_mode[mode].keys())
        means = []
        stds = []
        for e in epochs:
            m, s = mean_std(data_by_mode[mode][e])
            means.append(m)
            stds.append(s)

        color = COLOR[mode]
        ax.plot(epochs, means, marker="o", markersize=4, linewidth=2, color=color, label=mode)
        lower = [m - s for m, s in zip(means, stds)]
        upper = [m + s for m, s in zip(means, stds)]
        ax.fill_between(epochs, lower, upper, color=color, alpha=0.15, linewidth=0)

    ax.set_xlabel("Epoch")
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.set_xticks(range(1, 16))
    ax.grid(True, axis="y", linewidth=0.5, alpha=0.5)
    ax.legend(frameon=False)
    fig.tight_layout()
    fig.savefig(out_path)
    plt.close(fig)
    print(f"Da luu: {out_path}")


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    time_by_mode = {}
    vmrss_by_mode = {}
    for mode in MODES:
        per_epoch_time, per_epoch_vmrss = collect_per_epoch(mode)
        time_by_mode[mode] = per_epoch_time
        vmrss_by_mode[mode] = per_epoch_vmrss

    plot_metric(
        time_by_mode,
        ylabel="Thoi gian (ms)",
        title="Thoi gian train moi epoch - WorkspaceMode NONE vs ENABLED\n(mean +/- std, 3 lan chay x 15 epoch)",
        out_path=OUT_DIR / "workspace_time_per_epoch.png",
    )
    plot_metric(
        vmrss_by_mode,
        ylabel="VmRSS / Working Set (MB)",
        title="VmRSS moi epoch - WorkspaceMode NONE vs ENABLED\n(mean +/- std, 3 lan chay x 15 epoch)",
        out_path=OUT_DIR / "workspace_vmrss_per_epoch.png",
    )


if __name__ == "__main__":
    main()
