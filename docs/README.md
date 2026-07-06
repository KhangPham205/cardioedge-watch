# Tài liệu dự án AIOnWatch

Bộ tài liệu phân tích toàn diện dự án **AIOnWatch** — hệ thống Android hai module (đồng hồ Wear OS + điện thoại) chạy mô hình AI cục bộ để phát hiện bất thường nhịp tim (rung nhĩ) từ tín hiệu ECG.

> Tài liệu được biên soạn dưới góc nhìn của một Senior Software Engineer đang onboarding vào dự án. Mọi kết luận đều dẫn chiếu tới file/class/hàm cụ thể. Những chỗ chưa chắc chắn được đánh dấu **[Giả định]**.
>
> Ngày biên soạn: 2026-07-05 · Nhánh: `dev`

## Mục lục

| # | Tài liệu | Nội dung |
|---|---|---|
| 01 | [Tổng quan dự án](01-tong-quan.md) | Dự án là gì, giải quyết vấn đề gì, đối tượng, mục tiêu |
| 02 | [Kiến trúc tổng thể](02-kien-truc.md) | Mô hình kiến trúc, luồng dữ liệu, luồng xử lý chính |
| 03 | [Cấu trúc source code](03-cau-truc-source.md) | Cây thư mục, vai trò module, entry point, class quan trọng |
| 04 | [Chức năng hệ thống](04-chuc-nang.md) | Liệt kê & giải thích từng chức năng end-to-end |
| 05 | [Luồng hoạt động & luồng dữ liệu](05-luong-hoat-dong.md) | Vòng đời ứng dụng và đường đi của dữ liệu |
| 06 | [Thành phần kỹ thuật](06-thanh-phan-ky-thuat.md) | Framework, SDK, API, thư viện, model AI |
| 07 | [Thuật toán & logic quan trọng](07-thuat-toan.md) | Signal processing, AI pipeline, business logic |
| 08 | [Trạng thái & chất lượng mã](08-trang-thai-chat-luong.md) | Đã xong, còn thiếu, TODO, rủi ro lỗi, đánh giá chất lượng |
| 09 | [Sơ đồ tổng quan](09-so-do.md) | Sơ đồ kiến trúc, quan hệ module, luồng dữ liệu/xử lý |

## Tóm tắt nhanh

- **wear/** — App đồng hồ (Galaxy Watch 7): đọc ECG qua Samsung Health Sensor API, chạy TensorFlow Lite CNN-1D on-device, gửi kết quả sang điện thoại.
- **mobile/** — App điện thoại: nhận & lưu kết quả (Room DB), vẽ biểu đồ (MPAndroidChart), gửi/nạp nóng model sang đồng hồ, chẩn đoán định kỳ + cảnh báo.
- **Kênh giao tiếp:** Wearable Data Layer API (`/ecg_result` và `/model_update`).

> **Cảnh báo cho người mới:** file `wear/src/main/assets/model_config.json` mô tả một kiến trúc (ONNX/SLM, input 128) **KHÔNG khớp** với code thật (TensorFlow Lite, input 200). Đây là tàn dư của hướng đi cũ — xem [mục 08](08-trang-thai-chat-luong.md).