package com.example.wearhelloapp.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.wearhelloapp.R;
import com.example.wearhelloapp.viewmodel.MainViewModel;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_POST_NOTIFICATIONS = 2001;

    private MainViewModel viewModel;
    private View statusDot;
    private TextView tvConnectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermissionIfNeeded();

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        statusDot = findViewById(R.id.ivStatusDot);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);

        MaterialButton btnDiagnoseNow = findViewById(R.id.btnDiagnoseNow);
        Button btnNormal = findViewById(R.id.btnLoadNormal);
        Button btnAfib = findViewById(R.id.btnLoadAfib);
        Button btnHistory = findViewById(R.id.btnHistory);
        MaterialButton btnOpenSettings = findViewById(R.id.btnOpenSettings);

        btnDiagnoseNow.setOnClickListener(v -> {
            viewModel.requestDiagnosis();
            Toast.makeText(this, "Đang yêu cầu đồng hồ đo & chẩn đoán…", Toast.LENGTH_SHORT).show();
        });

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        btnNormal.setOnClickListener(v -> {
            Toast.makeText(this, "Đang gửi model Cơ bản…", Toast.LENGTH_SHORT).show();
            viewModel.sendModelToWatch("model_normal.tflite", "Basic Heart Rate");
        });

        btnAfib.setOnClickListener(v -> {
            Toast.makeText(this, "Đang gửi model Rung nhĩ…", Toast.LENGTH_SHORT).show();
            viewModel.sendModelToWatch("model_afib_v2.tflite", "AFib Detector");
        });

        btnOpenSettings.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        viewModel.getConnectionState().observe(this, this::renderConnectionState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra lại trạng thái kết nối mỗi khi màn hình hiển thị.
        viewModel.refreshConnection();
    }

    private void renderConnectionState(int state) {
        int colorRes;
        int textRes;
        switch (state) {
            case MainViewModel.STATE_CONNECTED:
                colorRes = R.color.status_connected;
                textRes = R.string.status_connected;
                break;
            case MainViewModel.STATE_DISCONNECTED:
                colorRes = R.color.status_disconnected;
                textRes = R.string.status_disconnected;
                break;
            default:
                colorRes = R.color.status_waiting;
                textRes = R.string.status_checking;
                break;
        }
        int color = ContextCompat.getColor(this, colorRes);
        statusDot.setBackgroundTintList(ColorStateList.valueOf(color));
        tvConnectionStatus.setText(textRes);
    }

    /**
     * Từ Android 13 (API 33 / TIRAMISU), thông báo cần xin quyền runtime POST_NOTIFICATIONS,
     * nếu không các cảnh báo sức khỏe của AutoEcgWorker sẽ bị hệ thống chặn im lặng.
     */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
            }
        }
    }
}
