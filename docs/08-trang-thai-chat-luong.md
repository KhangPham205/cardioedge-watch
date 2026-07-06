# 08 — Trạng thái & chất lượng mã

[← Quay lại mục lục](README.md)

## Trạng thái hiện tại

### ✅ Đã hoàn thành

- Kết nối cảm biến ECG Samsung + thu tín hiệu (commit `ac8a623`, `4a89a25`).
- Pipeline inference TFLite on-device hoạt động end-to-end.
- Đồng bộ kết quả đồng hồ → điện thoại qua Data Layer.
- Gửi & hot-swap model từ điện thoại sang đồng hồ.
- Lưu Room DB + 2 loại biểu đồ (commit `829da10`, `b809ec3`).
- Cài đặt lịch tự động + khung logic cảnh báo.

### ⚠️ Còn thiếu / đang dở

- **`AutoEcgWorker.runEcgDiagnosisModel()` là STUB** (`return 0.85f`) — chẩn đoán tự động **chưa thật sự chạy AI**. Model chỉ có trên đồng hồ; worker chạy trên điện thoại (không có runtime inference ECG) → luồng "tự động" chưa khép kín.
- **`model_config.json` LỆCH với code:** khai báo `input_length: 128` nhưng code dùng **200**; trỏ tới ONNX/SLM (distilgpt2) không dùng → tàn dư hướng đi cũ.
- **`currentModelName` mặc định "Unknown Model"** trên đồng hồ; chỉ đặt tên khi nhận model update. Lần đo đầu (dùng model asset mặc định) sẽ gắn nhãn "Unknown Model" trong DB.

### 📝 TODO / code chưa dùng

- Nút **"CHẨN ĐOÁN NGAY"** (`btnDiagnoseNow`) bị comment trong cả `mobile/MainActivity.java` và `activity_main.xml`.
- `EcgDao.getResultsByModel` — khai báo nhưng không được gọi.
- Dependency **Gson**, **ONNX runtime**, **tflite metadata / select-tf-ops** — không thấy dùng trong code Java.
- `EcgListenerService.saveToDatabase` có comment thừa nhận `allowMainThreadQueries` chỉ dùng cho demo.

### 🐞 Điểm có khả năng gây lỗi

| # | Vấn đề | Vị trí | Hậu quả |
|---|---|---|---|
| 1 | `new byte[available()]` + `read()` một lần | `ModelSender` | Model gửi đi cụt/hỏng với file lớn |
| 2 | Không đồng bộ hóa interpreter khi hot-swap | `MainActivity.downloadAndApplyModel` vs `processAI` | NPE/crash do race condition |
| 3 | Model shape cứng `[1,200,1]` | `ECGClassifier.predict` | Crash nếu model gửi tới có shape khác |
| 4 | `allowMainThreadQueries()` + poll DB mỗi 1s | `EcgListenerService`, `HistoryActivity`, `AutoEcgWorker` | Nguy cơ ANR |
| 5 | `POST_NOTIFICATIONS` không xin runtime | `mobile` (targetSdk 36 ≥ 33) | Thông báo cảnh báo bị chặn |
| 6 | `disconnectService` không reset `healthTrackingService = null` | `wear/MainActivity` nút DỪNG | Toggle BẮT ĐẦU/DỪNG sai trạng thái |
| 7 | Lệch version Play Services Wearable (18.1.0 vs 19.0.0) | 2 module | Thường vẫn ghép được, nên đồng bộ |
| 8 | Room DB dựng lại ở nhiều nơi thay vì singleton | 4 vị trí | Lãng phí tài nguyên, nợ kỹ thuật |

## Đánh giá chất lượng source code

### Điểm mạnh

- Pipeline edge-AI **gọn, dễ theo dõi**, đúng trọng tâm bài toán.
- Tách **tầng database** (Room DAO/Entity) rõ ràng.
- **Version catalog** tập trung, build hiện đại (Kotlin DSL).
- SQL thông minh: gom nhóm/AVG ngay trong DAO thay vì xử lý trên client.
- Có xử lý vòng đời cơ bản (`onDestroy` giải phóng tài nguyên, `onPause` gỡ listener).

### Điểm yếu

- **Dễ đọc:** tốt ở từng file, comment tiếng Việt hữu ích; nhưng **trộn lẫn quyền hạn, cảm biến, AI, networking, UI trong một `MainActivity`** (God Activity).
- **Dễ bảo trì:** yếu — không có ViewModel/Repository/DI; Room DB `databaseBuilder(...).build()` **lặp lại ở 4 nơi**; nhiều magic string (`"NORMAL_KEY"`, `"/ecg_result"`, `"ecg-database"`) rải rác, dễ lệch.
- **Khả năng mở rộng:** hạn chế — thêm loại tín hiệu/model mới đòi sửa nhiều chỗ; logic chẩn đoán auto còn stub.
- **Tính nhất quán:** trung bình — `model_config.json` mô tả kiến trúc khác hẳn code thực → dễ gây hiểu nhầm.
- **Tách biệt module:** UI ↔ logic ↔ data chưa tách; threading thủ công (Executors/Handler) không thống nhất.
- **Test:** chỉ còn `ExampleUnitTest`/`ExampleInstrumentedTest` mặc định — **không có test thực chất**.

### Kết luận

Mã ở mức **prototype/đồ án đã chạy được** (proof-of-concept hoàn chỉnh cho luồng chính), **chưa phải chất lượng production**.

### Ưu tiên refactor đề xuất

1. Sửa `ModelSender` đọc file bằng vòng lặp/`readAllBytes` (rủi ro cao nhất).
2. Đồng bộ hóa (lock) interpreter khi hot-swap.
3. Đưa Room DB về **singleton** + truy vấn **async** (bỏ `allowMainThreadQueries`).
4. Dọn `model_config.json` / ONNX / Gson thừa; thống nhất version Play Services.
5. Hiện thực hóa `AutoEcgWorker` (hoặc chuyển chẩn đoán auto sang đồng hồ nơi có model).
6. Xin runtime `POST_NOTIFICATIONS`; sửa toggle trạng thái nút đo trên đồng hồ.

---

**Xem tiếp:** [09 — Sơ đồ tổng quan](09-so-do.md)