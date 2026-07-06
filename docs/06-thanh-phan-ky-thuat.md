# 06 — Thành phần kỹ thuật

[← Quay lại mục lục](README.md)

## Bảng công nghệ

| Hạng mục | Công nghệ | Vị trí |
|---|---|---|
| Ngôn ngữ | Java 11 | toàn bộ |
| Build | Gradle Kotlin DSL, AGP 8.13.2, version catalog | `*.gradle.kts`, `libs.versions.toml` |
| **AI/ML** | **TensorFlow Lite 2.17.0** + support 0.5.0 + metadata 0.5.0 + `select-tf-ops` 2.16.1 | `wear/build.gradle.kts`, `ECGClassifier` |
| **ONNX Runtime** | `onnxruntime-android:1.23.2` — **khai báo nhưng KHÔNG dùng trong code** | `wear/build.gradle.kts`, `model_config.json` |
| Health SDK | **Samsung Health Sensor API 1.4.1** (.aar cục bộ) | `wear/libs/`, `MainActivity` |
| Giao tiếp thiết bị | Play Services Wearable (Data Layer) — wear `18.1.0`, mobile `19.0.0` (⚠ lệch version) | cả 2 module |
| Database | **Room 2.6.1** (SQLite) | `mobile/database/*` |
| Charts | **MPAndroidChart v3.1.0** | `HistoryActivity` |
| Background | **WorkManager 2.11.1** | `SettingsActivity`, `AutoEcgWorker` |
| UI (mobile) | AppCompat 1.6.1, Material Components, ConstraintLayout 2.1.4, CardView | layouts |
| UI (wear) | Wear 1.3.0, BoxInsetLayout | `wear/.../activity_main.xml` |
| Serialization | Gson 2.13.2 — **khai báo, chưa thấy dùng** | `mobile/build.gradle.kts` |
| Sensor | ECG (`ECG_ON_DEMAND`, `ECG_MV`) | `MainActivity` |

## Thông số model AI

- **Input shape:** `[1, 200, 1]` (1 batch × 200 mẫu × 1 kênh) — xem `ECGClassifier`: `INPUT_LENGTH=200`, `INPUT_CHANNELS=1`.
- **Output shape:** `[1, 2]` (2 lớp: Normal, Abnormal) — `OUTPUT_CLASSES=2`.
- **Interpreter:** 2 luồng (`options.setNumThreads(2)`).

### File model thực tế

| File | Vị trí | Kích thước | Ghi chú |
|---|---|---|---|
| `ecg_cnn_1d_detector.tflite` | wear/assets/models | 817.708 B | Model mặc định nạp lúc khởi động |
| `cnn_1d_model.tflite` | wear/assets/models | 817.708 B | **[Giả định]** trùng với file trên |
| `model_normal.tflite` | mobile/assets | 817.708 B | **[Giả định]** cùng model (gửi qua nút "Nhịp Tim Cơ Bản") |
| `model_afib.tflite` | mobile/assets | 166.920 B | Kiến trúc nhỏ hơn |
| `model_afib_v2.tflite` | mobile/assets | 166.936 B | Gửi qua nút "Rung Nhĩ (AFib)" |

> Các file 817.708 B có kích thước hệt nhau → **[Giả định]** cùng một model. `model_afib*` nhỏ hơn nhiều → kiến trúc/tham số khác. Cần kiểm chứng shape input của `model_afib_v2` khớp `[1,200,1]` (rủi ro crash — xem [08](08-trang-thai-chat-luong.md)).

## Cảnh báo cấu hình lệch

File `wear/src/main/assets/model_config.json`:

```json
{
  "model_type": "cnn",
  "cnn_model_path": "models/cnn_1d_model.tflite",
  "onnx_model_path": "/sdcard/models/slm_model.onnx",
  "onnx_model_url": "https://huggingface.co/onnx-community/distilgpt2-ONNX/...",
  "input_length": 128,
  "output_classes": 2
}
```

Vấn đề:
- `input_length: 128` **lệch** với code thật (`200`).
- Trỏ tới model ONNX/SLM (distilgpt2) — **không được dùng** ở bất kỳ đâu trong code Java.
- File này **không được đọc** bởi code hiện tại → là tàn dư của hướng đi cũ (thử nghiệm SLM/ONNX).

## Không sử dụng

- Camera, Bluetooth trực tiếp (đi qua Data Layer thay vì API BT thô).
- Server backend / cloud.
- ONNX runtime, Gson (khai báo nhưng không gọi).

---

**Xem tiếp:** [07 — Thuật toán & logic quan trọng](07-thuat-toan.md)