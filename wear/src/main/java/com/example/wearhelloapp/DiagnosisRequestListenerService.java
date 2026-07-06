package com.example.wearhelloapp;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Nhận lệnh "/request_diagnosis" do điện thoại (AutoEcgWorker) gửi tới và kích hoạt
 * một phiên đo trên đồng hồ. MainActivity tự bắt đầu đo ngay trong onCreate
 * ({@code checkPermissionsAndRun()}), nên mở nó lên tương đương với khởi động inference thật.
 *
 * Lưu ý: từ Android 10+, việc khởi chạy Activity từ nền bị hạn chế. Trên Wear OS,
 * lệnh đến từ Data Layer thường được cấp phép, nhưng đây vẫn là cơ chế best-effort —
 * nếu hệ thống chặn, người dùng cần mở app thủ công.
 */
public class DiagnosisRequestListenerService extends WearableListenerService {

    private static final String PATH_REQUEST_DIAGNOSIS = "/request_diagnosis";
    private static final String TAG = "DiagnosisListener";

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (PATH_REQUEST_DIAGNOSIS.equals(messageEvent.getPath())) {
            Log.d(TAG, "Nhận yêu cầu chẩn đoán từ điện thoại → mở màn hình đo");
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Không thể tự mở màn hình đo (bị hạn chế nền?)", e);
            }
        }
    }
}
