# 02 — Kiến trúc tổng thể

[← Quay lại mục lục](README.md)

## Mô hình kiến trúc

Đây là kiến trúc **Client–Server phân tán trên 2 thiết bị**, cụ thể là mô hình **Wearable Companion** của Android:

| Vai trò | Thiết bị | Trách nhiệm |
|---|---|---|
| **Producer / Edge-AI node** | Đồng hồ (`wear`) | Sinh dữ liệu ECG + suy luận AI on-device |
| **Consumer / Manager node** | Điện thoại (`mobile`) | Nhận kết quả, lưu trữ, hiển thị, cung cấp mô hình |

- **Kênh giao tiếp:** **Wearable Data Layer API** (`com.google.android.gms.wearable`) — cơ chế đồng bộ `DataItem` qua Bluetooth/Wi-Fi.
- Hai path chính: `/ecg_result` (đồng hồ → điện thoại) và `/model_update` (điện thoại → đồng hồ).

### Tổ chức lớp trong mỗi app

**[Giả định] Dự án không tuân theo pattern chính thức nào (không MVVM / Clean Architecture).** Thực chất là **kiến trúc phân lớp mỏng lấy Activity làm trung tâm (Activity-centric layered)**:

- UI + business logic gộp trong `Activity`.
- Một tầng `database` riêng (Room) ở phía mobile.
- Các service/worker rời rạc (`EcgListenerService`, `AutoEcgWorker`).
- **Không có** ViewModel, Repository, hay Dependency Injection.

## Luồng dữ liệu tổng thể

```
┌──────────────────────── ĐỒNG HỒ (wear) ────────────────────────┐
│  Cảm biến ECG (Samsung Health Sensor API)                        │
│        │ onDataReceived(DataPoint, ECG_MV)                       │
│        ▼                                                          │
│  ecgBuffer (gom 200 mẫu)  →  normalizeData (min-max)             │
│        │                                                          │
│        ▼                                                          │
│  ECGClassifier.predict()  →  TFLite CNN-1D  →  [Normal, Abnormal]│
│        │                                                          │
│        ├─► Hiển thị trên tvResult/tvStatus                       │
│        └─► sendToPhone()  ── PutDataMapRequest("/ecg_result") ──┐ │
└─────────────────────────────────────────────────────────────── │─┘
                                                                   │ Data Layer
┌──────────────────────── ĐIỆN THOẠI (mobile) ─────────────────── │─┐
│  EcgListenerService.onDataChanged("/ecg_result")  ◄─────────────┘ │
│        │                                                          │
│        ▼                                                          │
│  Room DB (ecg-database / bảng ecg_results)                        │
│        │                                                          │
│        ▼                                                          │
│  HistoryActivity → MPAndroidChart (Live line / History bar)       │
│                                                                   │
│  MainActivity → ModelSender.sendModelToWatch()                    │
│        └── PutDataMapRequest("/model_update") + Asset ──────────► │ (về đồng hồ)
│  SettingsActivity → WorkManager → AutoEcgWorker (định kỳ, stub)   │
└───────────────────────────────────────────────────────────────── ┘
```

## Luồng xử lý chính

1. Người dùng bấm **BẮT ĐẦU** trên đồng hồ → xin quyền → kết nối `HealthTrackingService`.
2. Cảm biến ECG stream từng mẫu `float` (mV) → buffer đủ 200 mẫu.
3. Chuẩn hóa min-max → chạy TFLite → ra 2 xác suất → hiển thị + gửi sang điện thoại.
4. Điện thoại lưu DB → vẽ biểu đồ; worker nền đánh giá và bắn cảnh báo.

## Đặc điểm ghép nối 2 module

- Cả hai module dùng **cùng package `com.example.wearhelloapp` và cùng `applicationId`** — chuẩn bắt buộc cho cặp app Wear companion để Data Layer ghép nối đúng.
- Còn tồn tại tên `WearHelloApp` (`rootProject.name` trong `settings.gradle.kts`) — dấu vết dự án khởi tạo từ template "Hello".

---

**Xem tiếp:** [03 — Cấu trúc source code](03-cau-truc-source.md)