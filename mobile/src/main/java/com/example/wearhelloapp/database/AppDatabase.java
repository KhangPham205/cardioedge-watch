package com.example.wearhelloapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {EcgResult.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract EcgDao ecgDao();

    private static volatile AppDatabase INSTANCE;

    /**
     * Singleton: trước đây {@code databaseBuilder(...)} bị gọi lặp lại ở 4 nơi
     * (MainActivity, HistoryActivity, EcgListenerService, AutoEcgWorker) — mỗi lần
     * tạo một kết nối mới, lãng phí và dễ gây khoá file. Nay dùng chung 1 instance.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "ecg-database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
