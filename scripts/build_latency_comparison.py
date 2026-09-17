"""
Gop 3 tang do do tre suy luan (kich ban 3) theo tung batch size:
  - In-process (DL4J, cung JVM)
  - Remote qua Spring Boot (Java)
  - Remote qua FastAPI (Python)

Doc tu:
  results/inference_latency_inprocess.csv
  results/inference_latency_remote_springboot.csv
  results/inference_latency_remote_fastapi.csv

Tinh ty so chenh lech (Remote/In-Process) cho ca p50 va mean, roi xuat bang
markdown giong dinh dang "Bang 4.x" trong bao cao.

Usage: python scripts/build_latency_comparison.py
Output: results/latency_comparison.csv
        results/latency_comparison_table.md
"""
import csv
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
RESULTS_DIR = PROJECT_ROOT / "results"


def load_csv(filename):
    path = RESULTS_DIR / filename
    with open(path, newline="", encoding="utf-8-sig") as f:
        return {int(r["batchSize"]): r for r in csv.DictReader(f)}


def format_vn(value, decimals):
    """Doi dinh dang so tu kieu US (1,234.56) sang kieu VN (1.234,56)."""
    s = f"{value:,.{decimals}f}"
    chars = []
    for ch in s:
        if ch == ",":
            chars.append(".")
        elif ch == ".":
            chars.append(",")
        else:
            chars.append(ch)
    return "".join(chars)


def main():
    in_process = load_csv("inference_latency_inprocess.csv")
    springboot = load_csv("inference_latency_remote_springboot.csv")
    fastapi = load_csv("inference_latency_remote_fastapi.csv")

    batch_sizes = sorted(in_process.keys())

    rows = []
    for b in batch_sizes:
        ip = in_process[b]
        sb = springboot.get(b)
        fa = fastapi.get(b)

        ip_p50 = float(ip["p50Ms"])
        ip_mean = float(ip["meanMs"])
        ip_thr = float(ip["throughputPerSec"])

        row = {
            "batchSize": b,
            "inProcessP50": ip_p50,
            "inProcessMean": ip_mean,
            "inProcessThroughput": ip_thr,
        }

        for label, data in [("springboot", sb), ("fastapi", fa)]:
            if data is None:
                continue
            p50 = float(data["p50Ms"])
            mean = float(data["meanMs"])
            thr = float(data["throughputPerSec"])
            row[f"{label}P50"] = p50
            row[f"{label}Mean"] = mean
            row[f"{label}Throughput"] = thr
            row[f"{label}RatioP50"] = p50 / ip_p50
            row[f"{label}RatioMean"] = mean / ip_mean

        rows.append(row)

    csv_header = ["batchSize",
                  "inProcessP50", "inProcessMean", "inProcessThroughput",
                  "springbootP50", "springbootMean", "springbootThroughput",
                  "springbootRatioP50", "springbootRatioMean",
                  "fastapiP50", "fastapiMean", "fastapiThroughput",
                  "fastapiRatioP50", "fastapiRatioMean"]
    with open(RESULTS_DIR / "latency_comparison.csv", "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=csv_header, extrasaction="ignore")
        writer.writeheader()
        for r in rows:
            writer.writerow(r)

    md = ["# So sanh do tre In-Process vs Remote (Spring Boot & FastAPI)\n"]

    for label, title in [("springboot", "Remote: Spring Boot (Java)"), ("fastapi", "Remote: FastAPI (Python)")]:
        md.append(f"## {title}\n")
        md.append("| Batch (B) | In-Process p50 (ms) | Remote p50 (ms) | Ty so chenh lech p50 | Chenh lech mean | Thong luong In-Process (mau/s) | Thong luong Remote (mau/s) |")
        md.append("| --- | --- | --- | --- | --- | --- | --- |")
        for r in rows:
            if f"{label}P50" not in r:
                continue
            md.append(
                "| B=" + str(r["batchSize"]) + " | " + format_vn(r["inProcessP50"], 3) + " | " +
                format_vn(r[label + "P50"], 3) + " | " + format_vn(r[label + "RatioP50"], 2) + "x | " +
                format_vn(r[label + "RatioMean"], 2) + "x | " + format_vn(r["inProcessThroughput"], 1) + " | " +
                format_vn(r[label + "Throughput"], 1) + " |"
            )
        md.append("")

    md.append("## So sanh truc tiep 3 tang theo p50 (ms)\n")
    md.append("| Batch (B) | In-Process | Spring Boot (Java) | FastAPI (Python) |")
    md.append("| --- | --- | --- | --- |")
    for r in rows:
        sb_p50 = format_vn(r["springbootP50"], 3) if "springbootP50" in r else "-"
        fa_p50 = format_vn(r["fastapiP50"], 3) if "fastapiP50" in r else "-"
        md.append("| B=" + str(r["batchSize"]) + " | " + format_vn(r["inProcessP50"], 3) + " | " + sb_p50 + " | " + fa_p50 + " |")

    out_text = "\n".join(md) + "\n"
    (RESULTS_DIR / "latency_comparison_table.md").write_text(out_text, encoding="utf-8")
    print(out_text)
    print("Da luu: results/latency_comparison.csv va results/latency_comparison_table.md")


if __name__ == "__main__":
    main()
