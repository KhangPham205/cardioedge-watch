package com.example.wearhelloapp.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.wearhelloapp.AutoEcgWorker;
import com.example.wearhelloapp.R;

import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup rgInterval;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("ECG_PREFS", MODE_PRIVATE);
        rgInterval = findViewById(R.id.rgInterval);

        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());

        // Load cài đặt cũ
        int savedInterval = prefs.getInt("AUTO_INTERVAL", 0);
        switch (savedInterval) {
            case 1: rgInterval.check(R.id.rb1h); break;
            case 4: rgInterval.check(R.id.rb4h); break;
            case 8: rgInterval.check(R.id.rb8h); break;
            case 12: rgInterval.check(R.id.rb12h); break;
            case 24: rgInterval.check(R.id.rb24h); break;
            default: rgInterval.check(R.id.rbOff); break;
        }

        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> {
            int intervalHours = 0;
            int selectedId = rgInterval.getCheckedRadioButtonId();

            if (selectedId == R.id.rb1h) intervalHours = 1;
            else if (selectedId == R.id.rb4h) intervalHours = 4;
            else if (selectedId == R.id.rb8h) intervalHours = 8;
            else if (selectedId == R.id.rb12h) intervalHours = 12;
            else if (selectedId == R.id.rb24h) intervalHours = 24;

            // Lưu vào máy
            prefs.edit().putInt("AUTO_INTERVAL", intervalHours).apply();

            // Cập nhật WorkManager (Lịch chạy ngầm)
            setupAutoDiagnosis(intervalHours);

            Toast.makeText(this, "Đã lưu cài đặt!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupAutoDiagnosis(int hours) {
        WorkManager workManager = WorkManager.getInstance(this);
        String WORK_NAME = "AUTO_ECG_DIAGNOSIS";

        if (hours == 0) {
            workManager.cancelUniqueWork(WORK_NAME); // Tắt tự động
        } else {
            // Lên lịch lặp lại mỗi X giờ
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    AutoEcgWorker.class, hours, TimeUnit.HOURS)
                    .build();

            workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE, // Thay thế lịch cũ nếu có
                    request
            );
        }
    }
}