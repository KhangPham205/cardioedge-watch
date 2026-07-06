package com.example.wearhelloapp;

import android.util.Log;

import com.example.wearhelloapp.data.EcgRepository;
import com.example.wearhelloapp.database.EcgResult;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class EcgListenerService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if ("/ecg_result".equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                    float normal = dataMapItem.getDataMap().getFloat("NORMAL_KEY");
                    float abnormal = dataMapItem.getDataMap().getFloat("ABNORMAL_KEY");
                    long timestamp = dataMapItem.getDataMap().getLong("TIMESTAMP");
                    String modelName = dataMapItem.getDataMap().getString("MODEL_NAME_KEY", "Unknown");

                    // Ghi bất đồng bộ qua repository (không còn allowMainThreadQueries).
                    EcgRepository.getInstance(getApplicationContext())
                            .insert(new EcgResult(timestamp, modelName, normal, abnormal));
                    Log.d("Mobile", "Đã nhận & lưu kết quả: " + modelName);
                }
            }
        }
    }
}
