package com.example.wearhelloapp.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.wearhelloapp.database.AppDatabase;
import com.example.wearhelloapp.database.EcgDao;
import com.example.wearhelloapp.database.EcgResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single source of truth cho dữ liệu ECG. Đứng giữa ViewModel/Service và Room DAO.
 *
 * <ul>
 *   <li>Ghi (insert) luôn chạy trên luồng nền ({@link #io}) → không bao giờ chặn main thread,
 *       thay cho {@code allowMainThreadQueries()} trước đây.</li>
 *   <li>Đọc cho UI trả về {@link LiveData} phản ứng — thay cho việc poll DB mỗi giây.</li>
 * </ul>
 */
public class EcgRepository {

    private static volatile EcgRepository INSTANCE;

    private final EcgDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private EcgRepository(Context context) {
        dao = AppDatabase.getInstance(context).ecgDao();
    }

    public static EcgRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (EcgRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EcgRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    /** Ghi bất đồng bộ. */
    public void insert(EcgResult result) {
        io.execute(() -> dao.insert(result));
    }

    public LiveData<List<String>> observeModelNames() {
        return dao.observeAllModelNames();
    }

    public LiveData<List<EcgResult>> observeLive(String modelName) {
        return dao.observeLiveResults(modelName);
    }

    public LiveData<List<EcgResult>> observeHistory(String modelName) {
        return dao.observeHistoryResults(modelName);
    }

    /** Truy vấn đồng bộ — chỉ dùng trong luồng nền (WorkManager). */
    public List<EcgResult> getLiveResultsSync(String modelName) {
        return dao.getLiveResults(modelName);
    }

    /** Cho phép tầng gọi chạy tác vụ tuỳ ý trên luồng IO dùng chung. */
    public void runOnIo(Runnable task) {
        io.execute(task);
    }
}
