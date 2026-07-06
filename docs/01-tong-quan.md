# 01 — Tổng quan dự án

[← Quay lại mục lục](README.md)

## Dự án này là gì?

**AIOnWatch** là một hệ thống Android **2 module đồng hành (companion apps)** dùng để **theo dõi tín hiệu điện tim (ECG) và phát hiện bất thường nhịp tim** (đặc biệt là rung nhĩ – AFib) bằng mô hình AI chạy trực tiếp trên đồng hồ thông minh.

- **`wear/`** — ứng dụng chạy trên **Samsung Galaxy Watch 7** (Wear OS): đọc ECG từ cảm biến, chạy suy luận AI on-device.
- **`mobile/`** — ứng dụng điện thoại: quản lý mô hình, lưu lịch sử, hiển thị biểu đồ, cảnh báo.

`README.md` gốc của dự án xác nhận ngắn gọn: *"Deploy local models on Samsung Galaxy Watch 7"*.

## Giải quyết vấn đề gì?

- Đưa mô hình AI (TensorFlow Lite) **chạy cục bộ ngay trên đồng hồ** thay vì gửi dữ liệu ECG thô lên server → giảm độ trễ, bảo mật dữ liệu sức khỏe, hoạt động không cần mạng.
- **Cho phép nạp nóng (hot-swap) mô hình** từ điện thoại sang đồng hồ mà không cần cài lại app.
- Lưu lịch sử, phân tích xu hướng và **cảnh báo sớm** khi có nguy cơ tim mạch.

## Mục tiêu chính

1. Thu tín hiệu ECG thời gian thực từ cảm biến Samsung Health.
2. Phân loại nhị phân: **Bình thường (Normal)** vs **Bất thường/Rung nhĩ (Abnormal/AFib)**.
3. Đồng bộ kết quả sang điện thoại để lưu trữ và trực quan hóa.
4. Cho phép cập nhật mô hình AI linh hoạt (OTA model update qua Data Layer).
5. Cảnh báo người dùng theo ngưỡng và xu hướng.

## Đối tượng sử dụng

- **Người dùng cuối:** người đeo Galaxy Watch 7 muốn tự theo dõi tình trạng tim mạch.
- **Nhóm phát triển:** **[Giả định]** nhóm nghiên cứu/đồ án về triển khai mô hình AI y sinh trên thiết bị đeo (căn cứ: đường dẫn `01_AISeQLab/Project`, comment tiếng Việt, package `com.example.*` mang tính học thuật/PoC).

## Đề tài / bài toán

Phân loại nhị phân tín hiệu ECG (Normal vs AFib) kết hợp:
- **Signal processing** nhẹ (chuẩn hóa, cửa sổ trượt).
- **Edge AI inference** (TensorFlow Lite trên Wear OS).
- **Đồng bộ dữ liệu đa thiết bị** (Wearable Data Layer).

## Bối cảnh & lịch sử phát triển (từ git log)

| Commit | Ý nghĩa |
|---|---|
| `950a9d7` | Initial commit (template) |
| `0f8b12f` | init flow code |
| `168f68b` | Get ECG Signal |
| `ac8a623` | Kết nối Watch và lấy tín hiệu ECG thành công |
| `4a89a25` | Kết nối điện thoại (emulator) với Watch 7 |
| `ad9a759` | init mobile module |
| `b809ec3` | UI mới cho app điện thoại |
| `829da10` | Thêm lịch cảnh báo & biểu đồ sức khỏe trực tiếp |

Nhánh hiện tại: `dev`. Nhánh chính: `main`. Có thêm nhánh `test`.

---

**Xem tiếp:** [02 — Kiến trúc tổng thể](02-kien-truc.md)