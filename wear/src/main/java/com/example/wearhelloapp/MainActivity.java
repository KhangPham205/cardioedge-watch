package com.example.wearhelloapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.wearhelloapp.sensor.ECGClassifier;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.samsung.android.service.health.tracking.ConnectionListener;
import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.HealthTrackerException;
import com.samsung.android.service.health.tracking.HealthTrackingService;
import com.samsung.android.service.health.tracking.data.DataPoint;
import com.samsung.android.service.health.tracking.data.HealthTrackerType;
import com.samsung.android.service.health.tracking.data.ValueKey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends FragmentActivity implements DataClient.OnDataChangedListener {

    // Tên model mặc định (khớp với model asset nạp lúc khởi động), tránh gắn nhãn "Unknown Model"
    // trong lần đo đầu tiên khi chưa nhận model nào từ điện thoại.
    private static final String DEFAULT_MODEL_NAME = "ECG Detector (mặc định)";
    private String currentModelName = DEFAULT_MODEL_NAME;
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView tvResult, tvStatus;
    private HealthTrackingService healthTrackingService;
    private HealthTracker ecgTracker;
    private ECGClassifier classifier;

    private final ArrayList<Float> ecgBuffer = new ArrayList<>();

    private static final String PERM_SAMSUNG_HEALTH = "com.samsung.android.hardware.sensormanager.permission.READ_ADDITIONAL_HEALTH_DATA";

    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.tvResult);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnAction = findViewById(R.id.btnAction);

        btnAction.setOnClickListener(v -> {
            if (healthTrackingService != null) {
                stopTracking();
                tvStatus.setText("Đã dừng đo");
                btnAction.setText("BẮT ĐẦU");
            } else {
                checkPermissionsAndRun();
                btnAction.setText("DỪNG ĐO");
            }
        });

        try {
            classifier = new ECGClassifier(getAssets(), "models/ecg_cnn_1d_detector.tflite");
        } catch (Exception e) { 
            Log.e(TAG, "Model Error", e); 
        }

        checkPermissionsAndRun();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
    }

    private void checkPermissionsAndRun() {
        int bodyState = ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS);
        int samsungState = ActivityCompat.checkSelfPermission(this, PERM_SAMSUNG_HEALTH);

        if (bodyState == PackageManager.PERMISSION_GRANTED && samsungState == PackageManager.PERMISSION_GRANTED) {
            connectHealthService();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BODY_SENSORS, PERM_SAMSUNG_HEALTH},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) connectHealthService();
            else tvStatus.setText("Thiếu quyền!");
        }
    }

    private void connectHealthService() {
        tvStatus.setText("Đang kết nối...");
        healthTrackingService = new HealthTrackingService(connectionListener, this);
        healthTrackingService.connectService();
    }

    private final ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void onConnectionSuccess() {
            startECGTracking();
        }

        @Override
        public void onConnectionEnded() { }

        @Override
        public void onConnectionFailed(HealthTrackerException e) {
            runOnUiThread(() -> tvStatus.setText("Lỗi kết nối: " + e.getErrorCode()));
        }
    };

    private void startECGTracking() {
        try {
            ecgTracker = healthTrackingService.getHealthTracker(HealthTrackerType.ECG_ON_DEMAND);
            ecgTracker.setEventListener(trackerListener);
            runOnUiThread(() -> tvStatus.setText("Sẵn sàng đo..."));
        } catch (Exception e) {
            runOnUiThread(() -> tvResult.setText("Không hỗ trợ ECG"));
        }
    }

    /**
     * Dừng đo và giải phóng hoàn toàn tài nguyên đo.
     * Bắt buộc reset {@code healthTrackingService = null} và gỡ listener của tracker,
     * nếu không nút BẮT ĐẦU/DỪNG sẽ kẹt trạng thái (lần bấm sau vẫn thấy service != null).
     */
    private void stopTracking() {
        if (ecgTracker != null) {
            ecgTracker.unsetEventListener();
            ecgTracker = null;
        }
        if (healthTrackingService != null) {
            healthTrackingService.disconnectService();
            healthTrackingService = null;
        }
        ecgBuffer.clear();
    }

    private final HealthTracker.TrackerEventListener trackerListener = new HealthTracker.TrackerEventListener() {
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            for (DataPoint dp : list) {
                float val = dp.getValue(ValueKey.EcgSet.ECG_MV);
                if (val != 0.0f) processAI(val);
            }
        }

        @Override
        public void onFlushCompleted() { }

        @Override
        public void onError(HealthTracker.TrackerError trackerError) {
            runOnUiThread(() -> tvStatus.setText("Lỗi: " + trackerError));
        }
    };

    private void processAI(float value) {
        if (classifier == null) return;
        ecgBuffer.add(value);

        // Số mẫu đầu vào lấy động từ model (không fix cứng 200) để tương thích model mới.
        int inputLength = classifier.getInputLength();
        if (ecgBuffer.size() < inputLength) return;

        float[] rawInput = new float[inputLength];
        for (int i = 0; i < inputLength; i++) rawInput[i] = ecgBuffer.get(i);

        float[] normalizedInput = normalizeData(rawInput);

        final float[] out;
        try {
            out = classifier.predict(normalizedInput);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Model có thể vừa bị hot-swap sang kích thước khác giữa chừng → bỏ qua khung này.
            Log.w(TAG, "Bỏ qua khung inference: " + e.getMessage());
            ecgBuffer.clear();
            return;
        }

        sendToPhone(out[0], out[1]);

        runOnUiThread(() -> {
            String status = (out[1] > out[0]) ? "CÓ BẤT THƯỜNG" : "BÌNH THƯỜNG";
            tvResult.setText(String.format(Locale.getDefault(), "%s\nN: %.1f%% - A: %.1f%%", status, out[0]*100, out[1]*100));
            tvStatus.setText(status.contains("BẤT THƯỜNG") ? "Nguy cơ cao!" : "Nhịp tim ổn định");
        });

        int overlap = Math.min(50, inputLength / 2);
        ecgBuffer.subList(0, overlap).clear();
    }

    private float[] normalizeData(float[] rawData) {
        float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
        for (float v : rawData) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        float range = Math.max(max - min, 1e-6f);
        float[] normalized = new float[rawData.length];
        for (int i = 0; i < rawData.length; i++) normalized[i] = (rawData[i] - min) / range;
        return normalized;
    }

    private void sendToPhone(float normal, float abnormal) {
        PutDataMapRequest dataMap = PutDataMapRequest.create("/ecg_result");
        dataMap.getDataMap().putFloat("NORMAL_KEY", normal);
        dataMap.getDataMap().putFloat("ABNORMAL_KEY", abnormal);
        dataMap.getDataMap().putLong("TIMESTAMP", System.currentTimeMillis());
        dataMap.getDataMap().putString("MODEL_NAME_KEY", currentModelName);

        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();
        Wearable.getDataClient(this).putDataItem(request);
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED && "/model_update".equals(event.getDataItem().getUri().getPath())) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset asset = dataMapItem.getDataMap().getAsset("model_file");
                String info = dataMapItem.getDataMap().getString("model_info");
                if (asset != null) downloadAndApplyModel(asset, info);
            }
        }
    }

    private void downloadAndApplyModel(Asset asset, String modelName) {
        this.currentModelName = modelName;
        downloadExecutor.execute(() -> {
            try {
                InputStream inputStream = Tasks.await(Wearable.getDataClient(this).getFdForAsset(asset)).getInputStream();
                File outFile = new File(getFilesDir(), "custom_model.tflite");
                try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
                }
                classifier.reloadModelFromFile(outFile);
                runOnUiThread(() -> tvStatus.setText("Đã cập nhật: " + modelName));
            } catch (Exception e) {
                Log.e(TAG, "Update Error", e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (healthTrackingService != null) healthTrackingService.disconnectService();
        if (ecgTracker != null) ecgTracker.unsetEventListener();
        if (classifier != null) classifier.close();
        downloadExecutor.shutdown();
    }
}