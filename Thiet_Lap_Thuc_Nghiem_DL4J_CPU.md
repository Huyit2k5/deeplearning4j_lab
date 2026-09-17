# **CẤU HÌNH THỰC NGHIỆM DEEPLEARNING4J TRÊN CPU (DỰA THEO BÁO CÁO)**

Báo cáo này chi tiết các thiết lập kỹ thuật và thông số môi trường cần thiết để triển khai và đo lường hiệu năng của thư viện DeepLearning4J (DL4J) trong môi trường tính toán CPU thuần túy, tối ưu hóa cho hệ sinh thái Apache Spark.

## **1. Yêu cầu Phần cứng & Hệ điều hành**
Hệ thống thực nghiệm đòi hỏi cấu hình tài nguyên đủ lớn để xử lý các phép toán tensor song song mà không bị nghẽn cổ chai tại CPU hoặc băng thông bộ nhớ.
- **Vi xử lý:** Kiến trúc x86_64, tối thiểu 16 luồng logic (Khuyến nghị xung nhịp cơ bản từ 2.30 GHz trở lên).
- **Bộ nhớ RAM:** Tối thiểu 16.0 GB khả dụng.
- **Hệ điều hành:** Ubuntu 22.04 LTS (Hỗ trợ chạy trực tiếp hoặc thông qua WSL2 với Linux Kernel 6.6.87+).

## **2. Nền tảng Phần mềm & Thư viện**
Sự tương thích giữa các phiên bản phần mềm đóng vai trò quan trọng trong việc đảm bảo tính ổn định của môi trường JVM và Native.

| Thành phần | Phiên bản | Ghi chú |
| :--- | :--- | :--- |
| **Java** | OpenJDK 17.0.20 | Môi trường thực thi chính |
| **Apache Spark** | 3.3.0 | Nền tảng tính toán phân tán |
| **DeepLearning4J** | 1.0.0-M2.1 | Thư viện Deep Learning trên JVM |
| **ND4J Backend** | nd4j-native | Sử dụng OpenBLAS hoặc Intel oneDNN |
| **Môi trường đối sánh** | Python 3.11 | Dùng cho FastAPI và PyTorch (CPU) |

## **3. Cấu hình Biến môi trường (Cô lập tài nguyên)**
Để đo lường chính xác khả năng mở rộng của Spark và tránh việc các thư viện toán học cấp thấp tự động chiếm dụng toàn bộ luồng CPU (oversubscription), cần thực hiện khóa luồng tính toán như sau:
- `OMP_NUM_THREADS=1`: Giới hạn luồng cho OpenMP.
- `OPENBLAS_NUM_THREADS=1`: Giới hạn luồng cho thư viện OpenBLAS.
- `MKL_NUM_THREADS=1`: Giới hạn luồng cho thư viện Intel MKL (nếu có).

## **4. Quy hoạch Bộ nhớ Spark (Spark-Submit / SparkConf)**
Trong DL4J, bộ nhớ được chia thành hai phần: Heap Memory (quản lý bởi JVM) và Off-Heap/Native Memory (quản lý bởi LibND4J). Việc cấu hình sai lệch có thể dẫn đến lỗi OutOfMemoryError hoặc SIGSEGV.
- `spark.executor.memory=4g`: Dành cho lưu trữ dữ liệu RDD và các đối tượng Java trung gian.
- `spark.executor.memoryOverhead=8g`: Không gian quan trọng nhất, dùng để chứa các tensor INDArray và thực thi mã C++ từ LibND4J.
- **Cờ JVM bắt buộc:** Cấu hình qua `spark.executor.extraJavaOptions`:
  - `-Dorg.bytedeco.javacpp.maxbytes=8G`: Tham số này dùng để chặn LibND4J cấp phát vượt ngưỡng memoryOverhead, đảm bảo tính ổn định cho toàn bộ cluster.

## **5. Dữ liệu & Xử lý (DataVec)**
Quy trình nạp liệu cần được tối ưu để đảm bảo CPU luôn có dữ liệu để tính toán, giảm thiểu thời gian chờ I/O.
- Sử dụng `CSVRecordReader` để thực hiện đọc tệp tin dữ liệu đầu vào.
- **Chuẩn hóa:** Áp dụng Z-score trực tiếp trên Spark DataFrame hoặc sử dụng `NormalizerStandardize` của DL4J.
- **Pipeline tối ưu:** Bắt buộc sử dụng `AsyncDataSetIterator` để thực hiện cơ chế nạp trước (prefetch) dữ liệu vào bộ nhớ đệm, tránh tình trạng CPU phải chờ đợi đĩa cứng trong quá trình huấn luyện.
- **Batching:** Thiết lập `batchSizePerWorker` linh hoạt ở các mức 16, 32, hoặc 64 tùy thuộc vào giới hạn bộ nhớ thực tế.

## **6. Kịch bản Thực nghiệm Trọng tâm**
Các kịch bản dưới đây được thiết kế để đánh giá hiệu quả vận hành của hệ thống:
1. **Kiểm chứng MemoryWorkspaces:** So sánh hiệu năng và mức độ chiếm dụng bộ nhớ ảo (VmRSS) khi thiết lập `WorkspaceMode.NONE` so với `WorkspaceMode.ENABLED`.
2. **Kiểm chứng Co giãn Spark Local:** Thay đổi số lượng luồng Spark Executor (từ 1 đến 8 luồng) để xác định điểm bão hòa phần cứng (thường bị giới hạn bởi băng thông RAM hoặc Cache L3).
3. **In-Process vs Remote:** Đo lường độ trễ phân rã (p50, p99) trên các lô dữ liệu nhỏ (B=1, B=8) để chứng minh ưu thế về tốc độ của việc chạy suy luận trực tiếp trong cùng tiến trình JVM so với gọi qua API từ xa.

---
**Thông tin xác nhận cấu hình:**
- **Địa điểm thực nghiệm:** HUIT
- **Kỹ sư phụ trách:** Phạm Nguyễn Thanh Huy
