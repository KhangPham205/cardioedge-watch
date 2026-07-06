package com.example.wearhelloapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.wearhelloapp.activity.HistoryActivity;
import com.example.wearhelloapp.database.AppDatabase;
import com.example.wearhelloapp.database.EcgDao;
import com.example.wearhelloapp.database.EcgResult;

import java.util.List;

/**
 * Worker chẩn đoán định kỳ.
 *
 * Kiến trúc (đã khép kín, thay cho stub {@code return 0.85f}):
 * <ol>
 *   <li>Điện thoại KHÔNG có cảm biến/không tự bịa kết quả. Worker gửi lệnh sang đồng hồ
 *       ({@link DiagnosisRequestSender}) để đồng hồ đo thật và chạy inference thật.</li>
 *   <li>Kết quả thật từ đồng hồ được {@link EcgListenerService} lưu vào DB.</li>
 *   <li>Worker đánh giá cảnh báo dựa trên các bản ghi THẬT gần nhất trong DB.</li>
 * </ol>
 *
 * Vì việc đo mất vài giây và diễn ra bất đồng bộ, lần chạy này đánh giá dựa trên dữ liệu
 * đã có; kết quả của phiên đo vừa yêu cầu sẽ được đánh giá ở lần chạy kế tiếp.
 */
public class AutoEcgWorker extends Worker {

    private static final int RECENT_WINDOW = 3;

    public AutoEcgWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // 1. Yêu cầu đồng hồ đo & chẩn đoán thật (nơi chứa cảm biến + model TFLite).
        new DiagnosisRequestSender().requestDiagnosisBlocking(context);

        // 2. Đánh giá cảnh báo dựa trên DỮ LIỆU THẬT gần nhất (không còn bịa số).
        EcgDao dao = AppDatabase.getInstance(context).ecgDao();
        List<EcgResult> recent = dao.getRecentResults(RECENT_WINDOW);
        evaluateHealthCondition(context, recent);

        return Result.success();
    }

    private void evaluateHealthCondition(Context context, List<EcgResult> recent) {
        if (recent.isEmpty()) {
            // Chưa có dữ liệu thật nào → không cảnh báo.
            return;
        }

        float currentProb = recent.get(0).abnormalProb; // Bản ghi mới nhất

        // LUẬT 1: NGUY HIỂM TỨC THỜI (nhịp tim cực xấu)
        if (currentProb >= 0.85f) {
            sendNotification(context,
                    "⚠️ CẢNH BÁO KHẨN CẤP",
                    "Nhịp tim cực kỳ bất thường. Vui lòng đến BỆNH VIỆN hoặc gọi cấp cứu ngay!",
                    NotificationCompat.PRIORITY_MAX);
            return;
        }

        // LUẬT 2: DIỄN BIẾN XẤU TỪ TỪ (nhiều lần liên tiếp đo ra bệnh)
        if (currentProb >= 0.70f && recent.size() >= RECENT_WINDOW) {
            boolean allHigh = true;
            for (int i = 0; i < RECENT_WINDOW; i++) {
                if (recent.get(i).abnormalProb < 0.70f) {
                    allHigh = false;
                    break;
                }
            }
            if (allHigh) {
                sendNotification(context,
                        "💊 Nhắc nhở sức khỏe",
                        "Rung nhĩ xuất hiện liên tục trong các lần đo gần đây. Hãy UỐNG THUỐC theo đơn của bác sĩ và nghỉ ngơi.",
                        NotificationCompat.PRIORITY_HIGH);
            }
        }
    }

    private void sendNotification(Context context, String title, String message, int priority) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "ECG_ALERTS";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Cảnh báo sức khỏe", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, HistoryActivity.class); // Bấm vào sẽ mở trang Lịch sử
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(priority)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        builder.setDefaults(NotificationCompat.DEFAULT_ALL);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
