# 03 — Cấu trúc source code

[← Quay lại mục lục](README.md)

## Cây thư mục

```
AIOnWatch/
├── settings.gradle.kts          # Khai báo 2 module: :wear, :mobile
├── gradle/libs.versions.toml    # Version catalog tập trung
│
├── wear/                        # === APP ĐỒNG HỒ ===
│   ├── libs/samsung-health-sensor-api-1.4.1.aar   # SDK cảm biến Samsung (đóng gói .aar)
│   ├── src/main/assets/
│   │   ├── model_config.json                       # Cấu hình model (⚠ LỆCH với code)
│   │   └── models/*.tflite                          # Model CNN-1D
│   └── src/main/java/com/example/wearhelloapp/
│       ├── MainActivity.java                        # ★ Entry point đồng hồ
│       └── sensor/ECGClassifier.java                # Wrapper TFLite Interpreter
│
└── mobile/                      # === APP ĐIỆN THOẠI ===
    ├── src/main/assets/*.tflite                     # Kho model để gửi sang đồng hồ
    └── src/main/java/com/example/wearhelloapp/
        ├── activity/
        │   ├── MainActivity.java                    # ★ Entry point điện thoại
        │   ├── HistoryActivity.java                 # Biểu đồ Live + History
        │   └── SettingsActivity.java                # Cài lịch tự động chẩn đoán
        ├── database/
        │   ├── AppDatabase.java                     # Room DB (@Database v1)
        │   ├── EcgDao.java                          # DAO truy vấn
        │   └── EcgResult.java                       # Entity bảng ecg_results
        ├── EcgListenerService.java                  # Nhận kết quả ECG từ đồng hồ
        ├── ModelSender.java                         # Gửi model .tflite sang đồng hồ
        └── AutoEcgWorker.java                       # Worker chẩn đoán định kỳ (stub)
```

## Vai trò từng module

### `wear/` — Ứng dụng đồng hồ
- Chịu trách nhiệm **thu tín hiệu và chạy AI**.
- Phụ thuộc `samsung-health-sensor-api-1.4.1.aar` (cục bộ) + TensorFlow Lite + Play Services Wearable.
- `minSdk = 30`, `targetSdk = 30`, `compileSdk = 36` (xem `wear/build.gradle.kts`).

### `mobile/` — Ứng dụng điện thoại
- Chịu trách nhiệm **quản lý, lưu trữ, trực quan hóa, cảnh báo**.
- Phụ thuộc Room, MPAndroidChart, WorkManager, Play Services Wearable.
- `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36` (xem `mobile/build.gradle.kts`).

## Entry points

| Loại | Vị trí | Ghi chú |
|---|---|---|
| Đồng hồ (LAUNCHER) | `wear/.../MainActivity` | Khai báo trong `wear/AndroidManifest.xml` |
| Điện thoại (LAUNCHER) | `mobile/.../activity/MainActivity` | Khai báo trong `mobile/AndroidManifest.xml` |
| Điểm vào ngầm (điện thoại) | `EcgListenerService` | Kích hoạt bởi Data Layer khi có `/ecg_result`, không cần mở app |

## Các class / component quan trọng

| Class | Module | Vai trò |
|---|---|---|
| `MainActivity` | wear | Điều phối toàn bộ: quyền, cảm biến, buffer, inference, gửi kết quả, nhận model |
| `ECGClassifier` | wear | Nạp & chạy TFLite, hỗ trợ reload model nóng (`reloadModelFromFile`) |
| `MainActivity` | mobile | Menu chính: chọn model gửi đi, mở History/Settings |
| `ModelSender` | mobile | Đóng gói `.tflite` thành `Asset`, gửi qua path `/model_update` |
| `EcgListenerService` | mobile | `WearableListenerService` nhận `/ecg_result`, ghi DB |
| `HistoryActivity` | mobile | Vẽ biểu đồ đường (live) & cột (history) bằng MPAndroidChart |
| `SettingsActivity` | mobile | Lưu chu kỳ chẩn đoán, đăng ký WorkManager |
| `AutoEcgWorker` | mobile | `Worker` định kỳ + logic cảnh báo (hiện là stub) |
| `EcgDao` / `EcgResult` / `AppDatabase` | mobile | Tầng lưu trữ Room |

## Manifest & khai báo hệ thống

**Đồng hồ (`wear/AndroidManifest.xml`):**
- Quyền: `INTERNET`, `BODY_SENSORS`, `READ_ADDITIONAL_HEALTH_DATA` (Samsung).
- `uses-feature android.hardware.type.watch` (bắt buộc).
- `meta-data` đăng ký listener của Samsung Health Tracking.

**Điện thoại (`mobile/AndroidManifest.xml`):**
- Quyền: `POST_NOTIFICATIONS` (⚠ khai báo nhưng chưa xin runtime).
- 3 Activity: `MainActivity` (LAUNCHER), `HistoryActivity`, `SettingsActivity`.
- 1 Service: `EcgListenerService` với intent-filter `DATA_CHANGED` + path `/ecg_result`.

---

**Xem tiếp:** [04 — Chức năng hệ thống](04-chuc-nang.md)