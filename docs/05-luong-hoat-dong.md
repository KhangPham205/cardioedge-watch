# 05 — Luồng hoạt động & luồng dữ liệu

[← Quay lại mục lục](README.md)

## Vòng đời ứng dụng — phía đồng hồ (`wear/MainActivity`)

1. **Khởi tạo** — `onCreate`: `setContentView`, khởi tạo `ECGClassifier` từ asset `models/ecg_cnn_1d_detector.tflite`, gọi `checkPermissionsAndRun`.
2. **Xin quyền** — `BODY_SENSORS` + `READ_ADDITIONAL_HEALTH_DATA` (Samsung).
3. **Kết nối dịch vụ** — `HealthTrackingService.connectService()` → callback `onConnectionSuccess` → `startECGTracking`.
4. **Xử lý dữ liệu** — stream ECG → `ecgBuffer` → `normalizeData` → `classifier.predict` → 2 xác suất.
5. **Hiển thị + đồng bộ** — cập nhật UI, `sendToPhone`.
6. **Nhận model (song song)** — `onResume` add `DataClient` listener; nhận `/model_update` → `downloadAndApplyModel` → hot reload; `onPause` gỡ listener.
7. **Giải phóng** — `onDestroy`: `disconnectService`, `unsetEventListener`, `classifier.close()`, `downloadExecutor.shutdown()`.

## Vòng đời ứng dụng — phía điện thoại

1. `MainActivity.onCreate` → khởi tạo `ModelSender` + Room DB, gắn listener cho các nút.
2. **Nền:** `EcgListenerService` được hệ thống đánh thức khi có `/ecg_result` → ghi DB (kể cả khi app đóng).
3. `HistoryActivity` poll DB mỗi giây khi ở chế độ Live, vẽ chart; dừng poll ở `onPause`.
4. `AutoEcgWorker` chạy theo lịch WorkManager, đánh giá & bắn cảnh báo.

## Luồng dữ liệu chi tiết (input → output)

```
INPUT: Cảm biến ECG đồng hồ (mV, float)
   │  Samsung Health Sensor API — ECG_ON_DEMAND
   ▼
[wear] MainActivity.trackerListener.onDataReceived
   │  lọc val != 0.0f
   ▼
ecgBuffer (List<Float>) ── đủ 200 ──► normalizeData (min-max [0,1])
   ▼
ECGClassifier.predict → TFLite → float[2] {normal, abnormal}
   │
   ├──► UI đồng hồ (tvResult, tvStatus)
   │
   └──► DataMap "/ecg_result" ──[Data Layer / BT-WiFi]──►
            [mobile] EcgListenerService.onDataChanged
                 ▼
            EcgResult → Room (bảng ecg_results)
                 ▼
   ┌─────────────┴──────────────┐
   ▼                            ▼
getLiveResults(200)        getHistoryResults(AVG/phút)
   ▼                            ▼
moving-avg(4) → LineChart   BarChart (đỏ nếu ≥70%)

OUTPUT: Biểu đồ trực quan + Thông báo cảnh báo (AutoEcgWorker)

Chiều ngược (model):
[mobile] assets/*.tflite → ModelSender → Asset "/model_update"
   ──► [wear] downloadAndApplyModel → filesDir/custom_model.tflite
   ──► ECGClassifier.reloadModelFromFile
```

## Nguồn — biến đổi — đích của dữ liệu

| Giai đoạn | Dữ liệu | Vị trí xử lý |
|---|---|---|
| **Input** | Tín hiệu ECG thô (mV) | Cảm biến Samsung → `onDataReceived` |
| **Đệm** | 200 mẫu float | `ecgBuffer` (`wear/MainActivity`) |
| **Tiền xử lý** | Vector chuẩn hóa [0,1] | `normalizeData` |
| **Suy luận** | `float[2]` xác suất | `ECGClassifier.predict` (TFLite) |
| **Truyền** | DataMap (4 key) | `sendToPhone` → Data Layer |
| **Lưu trữ** | Bản ghi `EcgResult` | `EcgListenerService` → Room |
| **Tổng hợp** | Chuỗi/nhóm theo phút | `EcgDao` (SQL) |
| **Output** | Biểu đồ + thông báo | `HistoryActivity`, `AutoEcgWorker` |

---

**Xem tiếp:** [06 — Thành phần kỹ thuật](06-thanh-phan-ky-thuat.md)