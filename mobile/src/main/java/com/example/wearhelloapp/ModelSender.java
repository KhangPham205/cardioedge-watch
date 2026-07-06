package com.example.wearhelloapp;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModelSender {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Trong file ModelSender.java
    public void sendModelToWatch(Context context, String assetFileName, String modelDisplayInfo) {
        executor.execute(() -> {
            try {
                Log.d("Phone", "Đang đọc file: " + assetFileName);

                // Đọc toàn bộ file bằng vòng lặp buffer để tránh model bị cụt/hỏng.
                // Lưu ý: inputStream.available() KHÔNG bảo đảm bằng kích thước file,
                // và read() một lần KHÔNG bảo đảm đọc hết với file lớn (~800KB).
                byte[] buffer;
                try (InputStream inputStream = context.getAssets().open(assetFileName);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[8192];
                    int read;
                    while ((read = inputStream.read(chunk)) != -1) {
                        baos.write(chunk, 0, read);
                    }
                    buffer = baos.toByteArray();
                }
                Log.d("Phone", "Đã đọc " + buffer.length + " bytes");

                Asset asset = Asset.createFromBytes(buffer);
                // --------------------

                PutDataMapRequest dataMap = PutDataMapRequest.create("/model_update");
                dataMap.getDataMap().putAsset("model_file", asset);
                dataMap.getDataMap().putString("model_info", modelDisplayInfo);
                dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());

                PutDataRequest request = dataMap.asPutDataRequest();
                request.setUrgent();

                Task<DataItem> putTask = Wearable.getDataClient(context).putDataItem(request);
                Tasks.await(putTask);

                Log.d("Phone", ">>> Đã gửi xong: " + modelDisplayInfo);

            } catch (Exception e) {
                Log.e("Phone", "Lỗi gửi model", e);
            }
        });
    }
}