# 04 — Chức năng hệ thống

[← Quay lại mục lục](README.md)

## 4.1. Đo & phân loại ECG on-device (đồng hồ)

- **Dùng để làm gì:** đọc ECG thô, phân loại Normal/Abnormal thời gian thực.
- **Hoạt động:**
  1. `checkPermissionsAndRun()` xin `BODY_SENSORS` + quyền Samsung Health.
  2. `connectHealthService()` → `HealthTrackingService.connectService()`.
  3. Callback `onConnectionSuccess` → `startECGTracking()` lấy `HealthTrackerType.ECG_ON_DEMAND`.
  4. Mỗi `DataPoint` lấy `ValueKey.EcgSet.ECG_MV`; lọc `val != 0.0f`.
  5. `processAI()` gom buffer 200 mẫu → `normalizeData()` (min-max) → `classifier.predict()`.
- **Class tham gia:** `wear/MainActivity`, `ECGClassifier`.
- **Luồng end-to-end:** cảm biến → `trackerListener.onDataReceived` → `processAI` → hiển thị (`tvResult`, `tvStatus`) + `sendToPhone`. Sau mỗi dự đoán xóa 50 mẫu đầu (`ecgBuffer.subList(0,50).clear()`) → cửa sổ trượt overlap 150 mẫu.

## 4.2. Gửi kết quả sang điện thoại

- **Dùng để làm gì:** đồng bộ kết quả phân loại từ đồng hồ về điện thoại.
- **Hoạt động:** `sendToPhone(normal, abnormal)` tạo `PutDataMapRequest("/ecg_result")` với các key:
  - `NORMAL_KEY` (float), `ABNORMAL_KEY` (float), `TIMESTAMP` (long), `MODEL_NAME_KEY` (string).
  - Gọi `setUrgent()` để ưu tiên gửi.
- **Class tham gia:** `wear/MainActivity.sendToPhone`.

## 4.3. Nạp nóng mô hình từ điện thoại (OTA model update)

- **Dùng để làm gì:** đổi model AI trên đồng hồ mà không cần cài lại app.
- **Class tham gia:** `ModelSender` (gửi) + `wear/MainActivity.onDataChanged`/`downloadAndApplyModel` (nhận) + `ECGClassifier.reloadModelFromFile`.
- **Luồng end-to-end:**
  1. Người dùng bấm "Nhịp Tim Cơ Bản" / "Rung Nhĩ" trên điện thoại.
  2. `sendModelToWatch(...)` đọc asset thành `byte[]` → `Asset.createFromBytes` → gửi path `/model_update` kèm `model_info`.
  3. Đồng hồ nhận `Asset` trong `onDataChanged`, lưu `custom_model.tflite` vào `getFilesDir()`.
  4. Gọi `reloadModelFromFile` (đóng interpreter cũ, map file mới bằng `MappedByteBuffer`).

## 4.4. Lưu trữ lịch sử (điện thoại)

- **Dùng để làm gì:** lưu mọi kết quả ECG để phân tích sau.
- **Hoạt động:** `EcgListenerService.saveToDatabase()` chèn `EcgResult` vào Room DB `ecg-database`, bảng `ecg_results`.
- **Class tham gia:** `EcgListenerService`, `AppDatabase`, `EcgDao`, `EcgResult`.

## 4.5. Hiển thị biểu đồ Live & History

- **Dùng để làm gì:** trực quan hóa xu hướng nguy cơ theo thời gian.
- **Class tham gia:** `HistoryActivity`, `EcgDao`, MPAndroidChart.
- **Chế độ Live** (`loadLiveChart`):
  - Lấy `getLiveResults` (200 điểm mới nhất, sắp xếp DESC → đảo lại).
  - Làm mượt bằng **moving average cửa sổ 4**.
  - Vẽ `LineChart` cubic bezier, có `LimitLine` cảnh báo ở mức 70.
  - Tự refresh mỗi 1s (`realTimeRunnable` + `Handler`).
- **Chế độ History** (`loadHistoryChart`):
  - `getHistoryResults` gom nhóm theo **phút** (`GROUP BY timestamp/60000`), tính `AVG(abnormalProb)`.
  - Vẽ `BarChart`, tô đỏ (`#E53935`) cột ≥ 70%, xanh (`#00796B`) nếu thấp hơn.
- Spinner lọc theo `modelName` (`getAllModelNames`).

## 4.6. Chẩn đoán tự động định kỳ + cảnh báo

- **Dùng để làm gì:** tự động đánh giá và cảnh báo nguy cơ theo lịch.
- **Class tham gia:** `SettingsActivity` (lịch) + `AutoEcgWorker` (thực thi) + WorkManager.
- **Hoạt động:**
  - `SettingsActivity` lưu chu kỳ (1/4/8/12/24h) vào `SharedPreferences ("ECG_PREFS")`, đăng ký `PeriodicWorkRequest` (`ExistingPeriodicWorkPolicy.REPLACE`), tên duy nhất `AUTO_ECG_DIAGNOSIS`.
  - `AutoEcgWorker.evaluateHealthCondition`:
    - **Luật 1** — `prob ≥ 0.85` → cảnh báo khẩn (`PRIORITY_MAX`, "đến bệnh viện/gọi cấp cứu").
    - **Luật 2** — `prob ≥ 0.70` và 3 lần đo gần nhất đều `≥ 0.70` → nhắc uống thuốc (`PRIORITY_HIGH`).
  - `sendNotification` tạo `NotificationChannel "ECG_ALERTS"`, đặt `PendingIntent` mở `HistoryActivity`.

> ⚠️ **Quan trọng:** `runEcgDiagnosisModel()` hiện trả về `0.85f` cứng — đây là **stub**, chưa thật sự đọc ECG hay chạy AI. Luồng "tự động" chưa khép kín (xem [08](08-trang-thai-chat-luong.md)).

---

**Xem tiếp:** [05 — Luồng hoạt động & luồng dữ liệu](05-luong-hoat-dong.md)