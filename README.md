# DL4J CPU Experiment & Fraud Detection Web Demo

Dự án thực nghiệm hiệu năng Deeplearning4j (DL4J) trên CPU thuần (tích hợp Apache Spark),
kèm một web demo trực quan, phục vụ bài thuyết trình về DL4J. Toàn bộ được thiết kế để
chạy hoàn toàn trên **Windows** (không cần WSL2/Linux).

Đặc tả gốc của thực nghiệm nằm ở [`Thiet_Lap_Thuc_Nghiem_DL4J_CPU.md`](Thiet_Lap_Thuc_Nghiem_DL4J_CPU.md).

## Mục lục

- [Tổng quan](#tổng-quan)
- [Kiến trúc & cấu trúc project](#kiến-trúc--cấu-trúc-project)
- [Dataset](#dataset)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Cài đặt lần đầu](#cài-đặt-lần-đầu)
- [Chạy từng phần](#chạy-từng-phần)
- [3 kịch bản thực nghiệm](#3-kịch-bản-thực-nghiệm)
- [Web demo](#web-demo)
- [Kết quả đã đo](#kết-quả-đã-đo)
- [Sự cố đã gặp & cách khắc phục (Windows)](#sự-cố-đã-gặp--cách-khắc-phục-windows)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)

## Tổng quan

Dự án gồm 2 phần:

1. **Thực nghiệm hiệu năng** — đo và so sánh hành vi của DL4J trên CPU theo 3 kịch bản
   quy định trong đặc tả gốc: `WorkspaceMode`, khả năng mở rộng của Spark local, và độ trễ
   suy luận in-process so với remote (qua REST API).
2. **Web demo** — một ứng dụng Spring Boot vừa serve model (REST API `/predict`) vừa hiển
   thị một trang HTML đơn giản để nhập/dự đoán giao dịch gian lận trực tiếp, cùng biểu đồ
   trực quan hoá kết quả benchmark.

Bài toán được chọn: **phát hiện gian lận thẻ tín dụng** (Kaggle Credit Card Fraud
Detection), dữ liệu dạng bảng (tabular), khớp với `CSVRecordReader` + `NormalizerStandardize`
mà đặc tả gốc yêu cầu.

## Kiến trúc & cấu trúc project

Maven multi-module:

- **`training`** — pipeline dữ liệu (DataVec) + huấn luyện model DL4J (thuần Java và qua
  Spark local). Biên dịch xuống bytecode **Java 11** (xem [lý do](#sự-cố-đã-gặp--cách-khắc-phục-windows)).
- **`benchmark`** — 3 class benchmark tương ứng 3 kịch bản, cộng các tiện ích đo latency/bộ nhớ.
- **`api`** — Spring Boot, serve model qua REST + phục vụ trang web demo tĩnh
  (`api/src/main/resources/static`).

```
D:\deeplearning4j\
├── pom.xml                  # parent POM (version DL4J/Spark/JDK)
├── data\creditcard.csv       # dataset (Kaggle Credit Card Fraud)
├── training\                 # data pipeline + training (Java 11 bytecode)
├── benchmark\                 # 3 kịch bản benchmark
├── api\                       # Spring Boot: REST API + web demo tĩnh
├── results\                   # output benchmark (CSV + JSON + summary.md)
└── scripts\                   # PowerShell tiện ích
```

## Dataset

`data/creditcard.csv` — Kaggle Credit Card Fraud Detection: 284.807 giao dịch, 30 feature
số (`Time`, `V1`..`V28` đã PCA-transform, `Amount`), nhãn `Class` (0 = bình thường,
1 = gian lận, chỉ ~0.17% dữ liệu là fraud).

## Yêu cầu môi trường

Đã cài và verify hoạt động trên máy này:

| Thành phần | Version | Ghi chú |
| --- | --- | --- |
| JDK 17 | Temurin 17.0.20 | Dùng cho `benchmark`, `api`, và chạy Maven |
| JDK 11 | Temurin 11.0.32 | **Chỉ dùng để chạy `SparkTrainingRunner`** (xem phần sự cố) |
| Apache Maven | 3.9.16 | Cài thủ công tại `C:\devtools\apache-maven-3.9.16` (không có trên winget) |
| Hadoop winutils | 3.3.6 | Tại `C:\hadoop\bin` — bắt buộc để Spark chạy trên Windows |

Không cần cài Python/PyTorch để chạy các script hiện có (đặc tả gốc có nhắc tới để đối
chứng nhưng nằm ngoài phạm vi các script đã dựng).

## Cài đặt lần đầu

Nếu clone project sang máy khác, cần thiết lập lại các thành phần dưới đây (đã có sẵn trên
máy hiện tại):

1. **JDK 17**: cài Eclipse Temurin 17, cập nhật đường dẫn trong
   [`scripts/setup-env.ps1`](scripts/setup-env.ps1) nếu khác `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`.
2. **JDK 11**: cần cho kịch bản 2 (Spark scaling). Cài bằng:
   ```powershell
   winget install --id EclipseAdoptium.Temurin.11.JDK -e
   ```
   Cập nhật hằng số `DEFAULT_JDK11_HOME` trong
   [`benchmark/.../SparkScalingBenchmark.java`](benchmark/src/main/java/vn/huit/dl4j/benchmark/SparkScalingBenchmark.java)
   nếu đường dẫn cài đặt khác.
3. **Maven**: tải bản zip từ https://maven.apache.org/download.cgi, giải nén vào
   `C:\devtools\apache-maven-3.9.16` (hoặc cập nhật `setup-env.ps1` theo đường dẫn thực tế).
4. **winutils cho Spark trên Windows**: tải `winutils.exe` + `hadoop.dll` (Hadoop 3.3.6) từ
   repo cộng đồng `cdarlint/winutils`, đặt vào `C:\hadoop\bin`.
5. Chạy `. .\scripts\setup-env.ps1` để nạp toàn bộ biến môi trường cần thiết cho phiên làm
   việc hiện tại (không set global).

## Chạy từng phần

Tất cả script PowerShell trong `scripts/` tự gọi `setup-env.ps1` và tự `Set-Location` về
project root, nên có thể chạy trực tiếp từ bất kỳ đâu:

```powershell
# 1. Build toàn bộ project
mvn install -q -DskipTests

# 2. Train model (MLP, DL4J thuần, không Spark)
.\scripts\run-training.ps1

# 3. Kịch bản 1: WorkspaceMode NONE vs ENABLED
.\scripts\run-benchmark-1-workspace.ps1

# 4. Kịch bản 2: Spark Local Scaling (threads 1,2,4,6,8)
.\scripts\run-benchmark-2-spark-scaling.ps1

# 5. Chạy web demo + API (giữ cửa sổ này chạy)
.\scripts\run-api.ps1

# 6. (Ở cửa sổ khác, trong lúc API đang chạy) Kịch bản 3: In-process vs Remote
.\scripts\run-benchmark-3-latency.ps1

# 7. Tổng hợp toàn bộ kết quả thành bảng markdown
.\scripts\summarize-results.ps1
```

Hoặc chạy `.\scripts\run-all.ps1` để tự động hoá bước 1–4 và 7 (bước 5–6 cần 2 cửa sổ
song song nên phải chạy thủ công).

## 3 kịch bản thực nghiệm

| # | Class | Đo gì | Output |
| --- | --- | --- | --- |
| 1 | `WorkspaceBenchmark` | Thời gian train + bộ nhớ (heap/non-heap/native) giữa `WorkspaceMode.NONE` và `ENABLED` | `results/workspace_benchmark.{csv,json}` |
| 2 | `SparkScalingBenchmark` | Thời gian train + throughput khi tăng số Spark executor thread (1→8) | `results/spark_scaling_benchmark.{csv,json}` |
| 3a | `InferenceLatencyBenchmark` | Độ trễ p50/p99 khi gọi `model.output()` trực tiếp trong JVM (in-process), batch B=1 và B=8 | `results/inference_latency_inprocess.{csv,json}` |
| 3b | `RemoteLatencyClient` | Độ trễ p50/p99 khi gọi qua REST API (`/api/predict`, `/api/predict/batch`) | `results/inference_latency_remote.{csv,json}` |

**Windows không có `VmRSS`** (chỉ số bộ nhớ chuẩn của Linux) — kịch bản 1 thay thế bằng tổ
hợp: JVM heap qua `ManagementFactory.getMemoryMXBean()`, native/off-heap qua JavaCPP
`Pointer.totalBytes()`, và (khi chạy qua `run-benchmark-1-workspace.ps1`) đối chiếu độc lập
bằng `Get-Process | Select WorkingSet64, PrivateMemorySize64` lấy mẫu mỗi 500ms, ghi ra
`results/workspace_benchmark_os_samples.csv`. Đây là ánh xạ tương đương, không phải số đo
giống hệt VmRSS của Linux.

## Web demo

Chạy `.\scripts\run-api.ps1`, mở `http://localhost:8080`:

- **Tab "Demo dự đoán"**: lấy một giao dịch mẫu (Normal hoặc Fraud thật từ dataset) hoặc
  dán 30 giá trị feature, gửi tới model, xem kết quả + xác suất + độ trễ.
- **Tab "Kết quả Benchmark"**: đọc `results/*.json` qua `/api/benchmark/results`, vẽ biểu
  đồ (Chart.js) cho cả 3 kịch bản — dùng để trình chiếu trực tiếp thay vì chụp ảnh dán slide.

Các endpoint chính:

| Method | Path | Mô tả |
| --- | --- | --- |
| POST | `/api/predict` | Dự đoán 1 giao dịch (`{"features": [30 số]}`) |
| POST | `/api/predict/batch` | Dự đoán nhiều giao dịch (`{"records": [{"features": [...]}, ...]}`) |
| GET | `/api/sample?classLabel=0\|1` | Lấy 1 ví dụ ngẫu nhiên (Normal hoặc Fraud thật) từ dataset |
| GET | `/api/benchmark/results` | Toàn bộ kết quả benchmark dạng JSON |

## Kết quả đã đo

Model (`training/models/fraud_mlp.zip`): **Accuracy 99.95%**, **F1 84.57%** (lớp fraud).

| Kịch bản | Kết quả |
| --- | --- |
| 1. Workspace | `ENABLED` nhanh hơn (32.87s vs 33.63s /2 epoch) và tiết kiệm bộ nhớ rõ rệt (heap 1.05GB vs 1.48GB, native 9.8MB vs 30.5MB) |
| 2. Spark Scaling | Throughput tăng 1391 → 3184 rec/s khi threads 1→8; thấy rõ điểm bão hòa (6→8 threads chỉ tăng nhẹ) |
| 3. In-process vs Remote | In-process p50 ≈1.4–1.8ms; Remote (qua HTTP) p50 ≈5–7ms — chênh lệch phản ánh chi phí serialize + network round-trip |

Xem `results/summary.md` để có bảng đầy đủ (copy trực tiếp vào slide).

## Sự cố đã gặp & cách khắc phục (Windows)

Ghi lại để tránh lặp lại khi setup trên máy khác, hoặc để giải thích trong phần "khó
khăn/giải pháp" khi thuyết trình:

1. **`exec:java` không đổi thư mục làm việc** — plugin `exec-maven-plugin` chạy in-process
   với Maven, không chdir vào basedir của module con. → Toàn bộ path mặc định trong code
   (`data/creditcard.csv`, `results/`, `training/models/...`) được viết tương đối theo
   **project root**, không theo module.
2. **Spark 3.3.0 + JDK 17**: lỗi `IllegalAccessError`/module access khi Spark dùng reflection
   nội bộ → cần `--add-opens` cho nhiều package `java.base`. Vì `exec:java` chạy in-process,
   các cờ này phải set qua **`MAVEN_OPTS`** (JVM của chính tiến trình `mvn`), không phải qua
   `-Dexec.jvmArgs` (chỉ có tác dụng khi exec fork JVM riêng).
3. **Spark local[N>1] + JDK 17**: sau khi fix (2), gặp tiếp
   `SerializedLambda.readResolve: too many arguments` khi deserialize closure trong
   `local[N]` mode — lỗi tương thích Spark 3.3.0/Scala 2.12 với JDK17. **Giải pháp**: biên
   dịch riêng module `training` xuống bytecode **Java 11** (`maven.compiler.source/target=11`
   trong `training/pom.xml`) và chạy `SparkTrainingRunner` bằng một JVM JDK 11 riêng
   (`SparkScalingBenchmark` tự spawn subprocess JDK11). Các module khác (`benchmark`, `api`)
   vẫn chạy JDK17 bình thường.
4. **Thiếu winutils.exe/HADOOP_HOME**: Spark trên Windows cần `winutils.exe` + `hadoop.dll`
   (khớp version Hadoop mà Spark bundle) để thao tác file hệ thống local. Thiếu sẽ gây lỗi
   ngay từ `WARN Shell: Did not find winutils.exe`.
5. **`exec:java` không set đúng `java.class.path`**: khi `SparkScalingBenchmark` spawn
   subprocess JDK11 bằng `ProcessBuilder`, dùng `System.getProperty("java.class.path")` cho
   ra classpath rỗng/sai (vì `exec-maven-plugin` nạp class qua `URLClassLoader` riêng, không
   cập nhật system property này). **Giải pháp**: lấy classpath trực tiếp từ
   `URLClassLoader.getURLs()` của class đang chạy.
6. **`spring-boot:run` fail với `CreateProcess error=206` (command line quá dài)**: do
   classpath đầy đủ của DL4J+Spark vượt giới hạn dòng lệnh Windows. **Giải pháp**: build
   fat-jar (`mvn package`, đã cấu hình `spring-boot-maven-plugin` goal `repackage`) rồi chạy
   trực tiếp bằng `java -jar api/target/api-1.0.0.jar`.
7. **Xung đột `log4j-slf4j-impl` vs `logback`**: DL4J/Spark mang theo `log4j-slf4j-impl`,
   xung đột với logging mặc định (`logback`) của Spring Boot. **Giải pháp**: exclude
   `log4j-slf4j-impl` khỏi dependency `training` trong `api/pom.xml`.
8. **Xung đột version Jackson**: DL4J/Spark mang theo Jackson cũ hơn bản Spring Boot 2.7.18
   cần (thiếu class `StreamWriteException` từ Jackson 2.12+). **Giải pháp**: import
   `jackson-bom` phiên bản 2.13.5 vào `dependencyManagement` của `api/pom.xml` để ghim version
   thống nhất.

## Cấu trúc thư mục

```
training/src/main/java/vn/huit/dl4j/training/
├── DataPipeline.java          # CSVRecordReader + NormalizerStandardize + split + AsyncDataSetIterator
├── ModelFactory.java          # MLP: 30 in -> 32 -> 16 -> 2 out (softmax)
├── LocalTrainingRunner.java   # Train thuần DL4J, lưu model .zip
└── SparkTrainingRunner.java   # Train qua Spark local[N] (chạy bằng JDK 11)

benchmark/src/main/java/vn/huit/dl4j/benchmark/
├── WorkspaceBenchmark.java        # Kịch bản 1
├── SparkScalingBenchmark.java     # Kịch bản 2 (điều phối, spawn JDK11 subprocess)
├── InferenceLatencyBenchmark.java # Kịch bản 3a (in-process)
├── RemoteLatencyClient.java       # Kịch bản 3b (remote, gọi API)
├── ResultWriter.java               # Ghi CSV + JSON
└── metrics/
    ├── MemoryProbe.java    # Thay thế VmRSS trên Windows
    └── LatencyStats.java   # Tính mean/p50/p99

api/src/main/java/vn/huit/dl4j/api/
├── ApiApplication.java
├── ModelHolder.java        # Load model + normalizer 1 lần lúc startup
├── PredictController.java  # POST /api/predict, /api/predict/batch
├── BenchmarkController.java # GET /api/benchmark/results
├── SampleController.java   # GET /api/sample
└── dto/...

api/src/main/resources/static/  # Web demo (HTML/CSS/JS thuần + Chart.js qua CDN)
scripts/                        # Script PowerShell cho từng bước
results/                        # Output benchmark + summary.md
```
