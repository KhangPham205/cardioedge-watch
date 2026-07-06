# CardioEdge Watch

> Tên repo đề xuất: `cardioedge-watch`

CardioEdge Watch là hệ thống Android hai module dùng để thu tín hiệu ECG trên Samsung Galaxy Watch, chạy mô hình TensorFlow Lite ngay trên đồng hồ, rồi đồng bộ kết quả về điện thoại để lưu trữ, trực quan hóa và cảnh báo.

Dự án hiện là prototype/phần mềm nghiên cứu. Kết quả phân loại không thay thế chẩn đoán y khoa, thiết bị y tế được chứng nhận, hoặc tư vấn từ bác sĩ.

## Vì sao tên này?

`cardioedge-watch` gói được ba ý chính của dự án:

- `cardio`: bài toán tim mạch, cụ thể là tín hiệu ECG và nguy cơ rung nhĩ.
- `edge`: mô hình AI chạy cục bộ trên thiết bị đeo, không phụ thuộc server.
- `watch`: trọng tâm triển khai là Wear OS/Samsung Galaxy Watch.

Nếu cần một tên thuần kỹ thuật hơn, hai lựa chọn phụ hợp lý là `watch-ecg-ai` hoặc `ecg-edge-watch`.

## Tổng quan

Repo gồm hai ứng dụng companion cùng dùng package/applicationId `com.example.wearhelloapp` để Wearable Data Layer ghép cặp đúng giữa điện thoại và đồng hồ:

- `wear/`: ứng dụng Wear OS trên đồng hồ. Module này đọc ECG bằng Samsung Health Sensor API, chuẩn hóa tín hiệu, chạy TFLite on-device và gửi xác suất Normal/Abnormal về điện thoại.
- `mobile/`: ứng dụng Android trên điện thoại. Module này kiểm tra kết nối đồng hồ, gửi model TFLite sang đồng hồ, yêu cầu đo/chẩn đoán, nhận kết quả, lưu Room DB, hiển thị biểu đồ và tạo cảnh báo định kỳ.

Luồng chính:

```text
Điện thoại
  -> /request_diagnosis
Đồng hồ
  -> Samsung ECG sensor
  -> min-max normalize
  -> TensorFlow Lite classifier
  -> /ecg_result
Điện thoại
  -> EcgListenerService
  -> Room DB
  -> biểu đồ Live/History + cảnh báo
```

Luồng cập nhật model:

```text
mobile assets/*.tflite
  -> ModelSender
  -> /model_update + Asset
  -> wear/files/custom_model.tflite
  -> ECGClassifier.reloadModelFromFile()
```

## Tính năng chính

- Đọc tín hiệu ECG on-demand trên đồng hồ qua Samsung Health Sensor API.
- Chạy mô hình TensorFlow Lite trực tiếp trên Wear OS.
- Phân loại nhị phân: Normal và Abnormal/AFib.
- Đọc shape đầu vào của model động, hỗ trợ model có độ dài tín hiệu khác nhau.
- Hot-swap model từ điện thoại sang đồng hồ mà không cần cài lại app.
- Đồng bộ kết quả qua Wearable Data Layer.
- Lưu lịch sử đo vào Room database trên điện thoại.
- Hiển thị biểu đồ live và lịch sử bằng MPAndroidChart.
- Lọc dữ liệu theo tên model đã dùng.
- Đặt lịch chẩn đoán định kỳ bằng WorkManager.
- Gửi notification khi dữ liệu gần nhất vượt ngưỡng cảnh báo.

## Kiến trúc

```text
cardioedge-watch/
├── mobile/                         # Android phone app
│   ├── src/main/assets/             # model_normal.tflite, model_afib_v2.tflite
│   └── src/main/java/com/example/wearhelloapp/
│       ├── activity/                # Main, History, Settings screens
│       ├── data/                    # EcgRepository
│       ├── database/                # Room DB, DAO, Entity
│       ├── viewmodel/               # MainViewModel, HistoryViewModel
│       ├── AutoEcgWorker.java       # Scheduled diagnosis + notifications
│       ├── DiagnosisRequestSender.java
│       ├── EcgListenerService.java  # Receives /ecg_result
│       └── ModelSender.java         # Sends /model_update
│
├── wear/                           # Wear OS app
│   ├── libs/                        # Samsung Health Sensor API AAR
│   └── src/main/
│       ├── assets/models/           # default TFLite models
│       └── java/com/example/wearhelloapp/
│           ├── MainActivity.java    # ECG capture, inference, sync
│           ├── DiagnosisRequestListenerService.java
│           └── sensor/ECGClassifier.java
│
├── docs/                           # Tài liệu phân tích chi tiết
├── gradle/libs.versions.toml        # Version catalog
├── settings.gradle.kts              # :mobile + :wear
└── build.gradle.kts
```

## Data Layer contract

Các path chính giữa hai thiết bị:

| Path | Chiều | Mục đích |
| --- | --- | --- |
| `/request_diagnosis` | điện thoại -> đồng hồ | Yêu cầu đồng hồ mở phiên đo/chẩn đoán |
| `/model_update` | điện thoại -> đồng hồ | Gửi file model `.tflite` dạng `Asset` |
| `/ecg_result` | đồng hồ -> điện thoại | Gửi kết quả phân loại ECG |

Payload `/ecg_result`:

| Key | Kiểu | Ý nghĩa |
| --- | --- | --- |
| `NORMAL_KEY` | `float` | Xác suất nhịp bình thường |
| `ABNORMAL_KEY` | `float` | Xác suất bất thường/rung nhĩ |
| `TIMESTAMP` | `long` | Thời điểm đo theo `System.currentTimeMillis()` |
| `MODEL_NAME_KEY` | `String` | Tên model đang chạy trên đồng hồ |

Payload `/model_update`:

| Key | Kiểu | Ý nghĩa |
| --- | --- | --- |
| `model_file` | `Asset` | Nội dung file `.tflite` |
| `model_info` | `String` | Tên hiển thị của model |
| `timestamp` | `long` | Thời điểm gửi model |

## AI pipeline trên đồng hồ

1. `MainActivity` kết nối `HealthTrackingService`.
2. `HealthTrackerType.ECG_ON_DEMAND` trả về từng mẫu ECG qua `ECG_MV`.
3. App gom đủ số mẫu theo `ECGClassifier.getInputLength()`.
4. Tín hiệu được chuẩn hóa min-max về khoảng `[0, 1]`.
5. `ECGClassifier` reshape thành `[1, inputLength, inputChannels]`.
6. TensorFlow Lite trả về vector xác suất, thường là `[normal, abnormal]`.
7. Đồng hồ hiển thị trạng thái và gửi kết quả sang điện thoại.

`ECGClassifier` dùng lock khi inference và reload model để tránh đóng `Interpreter` trong lúc model đang chạy.

## Tech stack

- Java 11 source compatibility.
- Android Gradle Plugin 8.13.2, Kotlin DSL, Gradle wrapper.
- Wear OS + Android phone companion app.
- Google Play Services Wearable Data Layer 19.0.0.
- Samsung Health Sensor API 1.4.1 AAR.
- TensorFlow Lite 2.17.0.
- AndroidX Lifecycle ViewModel/LiveData.
- Room 2.6.1.
- WorkManager 2.11.1.
- Material Components, AppCompat, ConstraintLayout.
- MPAndroidChart 3.1.0.

## Yêu cầu

- Android Studio phiên bản mới, JDK 17 hoặc tương thích với AGP 8.x.
- Samsung Galaxy Watch hỗ trợ ECG và Samsung Health Sensor API.
- Điện thoại Android đã ghép cặp với đồng hồ.
- Quyền `BODY_SENSORS` và `com.samsung.android.hardware.sensormanager.permission.READ_ADDITIONAL_HEALTH_DATA` trên đồng hồ.
- Quyền `POST_NOTIFICATIONS` trên điện thoại nếu muốn nhận cảnh báo.

## Cách build và chạy

Mở repo bằng Android Studio, sync Gradle, sau đó build cả hai module:

```bash
./gradlew :mobile:assembleDebug :wear:assembleDebug
```

Trên Windows PowerShell:

```powershell
.\gradlew.bat :mobile:assembleDebug :wear:assembleDebug
```

Cài app điện thoại và app đồng hồ qua Android Studio bằng cách chọn đúng target device cho từng module. Có thể dùng Gradle install task nếu môi trường ADB đang trỏ đúng thiết bị:

```bash
./gradlew :mobile:installDebug
./gradlew :wear:installDebug
```

## Cách sử dụng

1. Cài `mobile` trên điện thoại và `wear` trên đồng hồ đã ghép cặp.
2. Mở app trên đồng hồ và cấp quyền cảm biến.
3. Mở app trên điện thoại, kiểm tra trạng thái kết nối đồng hồ.
4. Chọn model `Nhịp tim cơ bản` hoặc `Rung nhĩ (AFib)` để gửi sang đồng hồ.
5. Bấm `Chẩn đoán ngay` trên điện thoại hoặc `BẮT ĐẦU` trên đồng hồ.
6. Mở `Lịch sử thống kê` để xem biểu đồ live/history theo model.
7. Vào `Cài đặt` để bật lịch chẩn đoán tự động theo chu kỳ 1, 4, 8, 12 hoặc 24 giờ.

## Trạng thái hiện tại

Đã có:

- Luồng đo ECG và inference on-device trên đồng hồ.
- Đồng bộ kết quả đồng hồ -> điện thoại.
- Gửi và reload model điện thoại -> đồng hồ.
- Room DB singleton, Repository, ViewModel và LiveData cho màn hình history.
- Biểu đồ live/history và logic cảnh báo dựa trên dữ liệu gần nhất.
- Worker định kỳ gửi yêu cầu đo sang đồng hồ và đánh giá dữ liệu đã lưu.

Cần lưu ý:

- Đây là prototype nghiên cứu, chưa có kiểm định lâm sàng.
- Chất lượng dự đoán phụ thuộc hoàn toàn vào dữ liệu huấn luyện và model TFLite.
- Tính năng đo tự động từ nền có thể bị hệ điều hành hạn chế; nếu Wear OS chặn mở màn hình đo từ background, người dùng cần mở app đồng hồ thủ công.
- Emulator không thay thế được kiểm thử ECG thật trên phần cứng Samsung.
- Tên package và root project vẫn còn dấu vết template: `com.example.wearhelloapp` và `WearHelloApp`. Khi productize nên đổi đồng bộ cả hai module.

## Tài liệu chi tiết

Thư mục `docs/` chứa tài liệu phân tích sâu hơn:

- [Tổng quan dự án](docs/01-tong-quan.md)
- [Kiến trúc tổng thể](docs/02-kien-truc.md)
- [Cấu trúc source code](docs/03-cau-truc-source.md)
- [Chức năng hệ thống](docs/04-chuc-nang.md)
- [Luồng hoạt động và dữ liệu](docs/05-luong-hoat-dong.md)
- [Thành phần kỹ thuật](docs/06-thanh-phan-ky-thuat.md)
- [Thuật toán và logic quan trọng](docs/07-thuat-toan.md)
- [Trạng thái và chất lượng mã](docs/08-trang-thai-chat-luong.md)
- [Sơ đồ tổng quan](docs/09-so-do.md)

Một vài tài liệu trong `docs/` có thể phản ánh trạng thái cũ của source; README này được viết theo code hiện tại trong repo.

## License

MIT License. Xem [LICENSE](LICENSE).
