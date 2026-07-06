package com.example.wearhelloapp.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ecg_results")
public class EcgResult {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public long timestamp;      // Thời gian đo
    public String modelName;    // Tên model (VD: "Basic Heart Rate", "AFib Detector")
    public float normalProb;    // Xác suất bình thường
    public float abnormalProb;  // Xác suất bất thường

    public EcgResult(long timestamp, String modelName, float normalProb, float abnormalProb) {
        this.timestamp = timestamp;
        this.modelName = modelName;
        this.normalProb = normalProb;
        this.abnormalProb = abnormalProb;
    }
}