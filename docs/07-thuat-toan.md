# 07 — Thuật toán & logic quan trọng

[← Quay lại mục lục](README.md)

## 7.1. Signal processing (đồng hồ)

### Chuẩn hóa Min-Max (`MainActivity.normalizeData`)

```java
float range = Math.max(max - min, 1e-6f);
normalized[i] = (rawData[i] - min) / range;
```

- Đưa tín hiệu về khoảng `[0, 1]` trước inference.
- Chống chia cho 0 bằng epsilon `1e-6f`.
- Độ phức tạp O(n) với n = 200.

### Cửa sổ trượt có overlap (`MainActivity.processAI`)

- Buffer 200 mẫu; mỗi chu kỳ chỉ trượt (xóa) 50 mẫu đầu → **overlap 75%** (giữ lại 150 mẫu).
- Mục đích: tăng tần suất dự đoán, làm kết quả mượt và phản ứng nhanh hơn.

## 7.2. AI/ML pipeline

```
ECG_MV (float)
  → gom 200 mẫu
  → min-max normalize → [0,1]
  → reshape [1][200][1]
  → Interpreter.run
  → float[2] {normal, abnormal}
```

- **Quyết định nhãn:** `out[1] > out[0]` → "CÓ BẤT THƯỜNG"; ngược lại "BÌNH THƯỜNG" (`processAI`).
- Hiển thị phần trăm: `N: xx.x% - A: yy.y%`.

## 7.3. Hậu xử lý & business logic (điện thoại)

### Làm mượt Live chart — Moving Average cửa sổ 4 (`HistoryActivity.loadLiveChart`)

```java
sum += rawProb;
windowQueue.add(rawProb);
if (windowQueue.size() > windowSize) sum -= windowQueue.removeFirst();
float smoothedProb = sum / windowQueue.size();
```

- Thuật toán trượt tổng (sliding sum) O(n), cửa sổ 4 điểm.
- Giảm nhiễu răng cưa trên đồ thị đường.

### Tổng hợp History — Downsampling theo phút (`EcgDao.getHistoryResults`)

```sql
SELECT MIN(id) as id, MIN(timestamp) as timestamp, modelName,
       AVG(normalProb) as normalProb, AVG(abnormalProb) as abnormalProb
FROM ecg_results WHERE modelName = :name
GROUP BY (timestamp / 60000) ORDER BY timestamp ASC
```

- Gom nhóm theo phút (`timestamp / 60000`) + tính trung bình ngay trong SQL.
- Giảm số điểm vẽ → tránh lag; xử lý phía DB thay vì client.

### Máy trạng thái cảnh báo 2 luật (`AutoEcgWorker.evaluateHealthCondition`)

| Luật | Điều kiện | Hành động | Priority |
|---|---|---|---|
| 1 — Nguy hiểm tức thời | `currentProb ≥ 0.85` | Cảnh báo khẩn cấp | `PRIORITY_MAX` |
| 2 — Diễn biến xấu | `currentProb ≥ 0.70` VÀ 3 lần đo gần nhất đều `≥ 0.70` | Nhắc uống thuốc | `PRIORITY_HIGH` |

## 7.4. Các đoạn phức tạp / nhạy cảm

### Hot-swap model (`MainActivity.downloadAndApplyModel`)

- Chạy trên thread `downloadExecutor`: I/O đọc `Asset` + `Tasks.await` + tạo lại `Interpreter`.
- **Rủi ro race condition:** thread này có thể `close()` interpreter trong khi thread cảm biến (`processAI`) đang `interpreter.run()`. **Không có đồng bộ hóa** — có thể gây NPE/crash.

### Đọc file model (`ModelSender.sendModelToWatch`)

```java
byte[] buffer = new byte[inputStream.available()];
inputStream.read(buffer);  // ⚠ đọc một lần duy nhất
```

- `available()` **không** bảo đảm bằng kích thước file; `read()` một lần **không** bảo đảm đọc hết.
- Với file ~800KB (`model_normal.tflite`), rủi ro gửi model **cụt/hỏng** → interpreter fail phía đồng hồ.
- **Cách sửa đúng:** dùng vòng lặp đọc hết stream (giống `downloadAndApplyModel` phía wear đã làm) hoặc `readAllBytes()` (API 33+).

---

**Xem tiếp:** [08 — Trạng thái & chất lượng mã](08-trang-thai-chat-luong.md)