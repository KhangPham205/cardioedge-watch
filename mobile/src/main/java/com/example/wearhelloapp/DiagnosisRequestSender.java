package com.example.wearhelloapp;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

/**
 * Gửi lệnh "yêu cầu đo & chẩn đoán" tới đồng hồ — nơi thực sự chứa cảm biến ECG và model.
 *
 * Kiến trúc: điện thoại KHÔNG tự bịa kết quả. Nó chỉ đánh thức đồng hồ để đồng hồ đo thật,
 * chạy inference thật rồi gửi kết quả về ("/ecg_result"). Đây là lý do worker gọi hàm này
 * thay cho stub {@code return 0.85f} trước đây.
 */
public class DiagnosisRequestSender {

    public static final String PATH_REQUEST_DIAGNOSIS = "/request_diagnosis";
    private static final String TAG = "DiagnosisRequest";

    /** Gọi trong luồng nền (WorkManager) — dùng Tasks.await nên sẽ chặn luồng gọi. */
    public boolean requestDiagnosisBlocking(Context context) {
        try {
            List<Node> nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes());
            if (nodes.isEmpty()) {
                Log.w(TAG, "Không có đồng hồ nào đang kết nối");
                return false;
            }
            for (Node node : nodes) {
                Tasks.await(Wearable.getMessageClient(context)
                        .sendMessage(node.getId(), PATH_REQUEST_DIAGNOSIS, new byte[0]));
                Log.d(TAG, "Đã gửi yêu cầu chẩn đoán tới: " + node.getDisplayName());
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Không gửi được yêu cầu chẩn đoán", e);
            return false;
        }
    }
}
