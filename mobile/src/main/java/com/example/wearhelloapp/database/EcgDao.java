package com.example.wearhelloapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EcgDao {

    @Insert
    void insert(EcgResult result);

    // ===== Reactive (cho UI) — Room tự phát lại khi bảng thay đổi, KHÔNG cần poll mỗi giây =====

    /** Danh sách tên model để tạo menu lọc. */
    @Query("SELECT DISTINCT modelName FROM ecg_results")
    LiveData<List<String>> observeAllModelNames();

    /** LIVE: 200 điểm mới nhất theo model. */
    @Query("SELECT * FROM ecg_results WHERE modelName = :name ORDER BY timestamp DESC LIMIT 200")
    LiveData<List<EcgResult>> observeLiveResults(String name);

    /** HISTORY: gom nhóm theo phút, tính trung bình xác suất bất thường. */
    @Query("SELECT MIN(id) as id, MIN(timestamp) as timestamp, modelName, " +
            "AVG(normalProb) as normalProb, AVG(abnormalProb) as abnormalProb " +
            "FROM ecg_results WHERE modelName = :name GROUP BY (timestamp / 60000) ORDER BY timestamp ASC")
    LiveData<List<EcgResult>> observeHistoryResults(String name);

    // ===== Sync (chỉ gọi NGOÀI main thread: WorkManager / Executor) =====

    /** Dùng cho AutoEcgWorker (chạy trên luồng nền của WorkManager). */
    @Query("SELECT * FROM ecg_results WHERE modelName = :name ORDER BY timestamp DESC LIMIT 200")
    List<EcgResult> getLiveResults(String name);

    /** N kết quả THẬT gần nhất (mọi model) — để đánh giá cảnh báo dựa trên dữ liệu thực. */
    @Query("SELECT * FROM ecg_results ORDER BY timestamp DESC LIMIT :limit")
    List<EcgResult> getRecentResults(int limit);
}
