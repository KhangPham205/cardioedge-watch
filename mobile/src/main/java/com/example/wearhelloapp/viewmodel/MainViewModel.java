package com.example.wearhelloapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wearhelloapp.DiagnosisRequestSender;
import com.example.wearhelloapp.ModelSender;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel cho MainActivity (điện thoại): gửi model, yêu cầu chẩn đoán, và theo dõi
 * trạng thái kết nối với đồng hồ. Mọi logic Data Layer nằm ngoài Activity và sống sót
 * qua xoay màn hình.
 */
public class MainViewModel extends AndroidViewModel {

    public static final int STATE_CHECKING = 0;
    public static final int STATE_CONNECTED = 1;
    public static final int STATE_DISCONNECTED = 2;

    private final ModelSender modelSender = new ModelSender();
    private final DiagnosisRequestSender diagnosisSender = new DiagnosisRequestSender();
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Integer> connectionState = new MutableLiveData<>(STATE_CHECKING);

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Integer> getConnectionState() {
        return connectionState;
    }

    public void sendModelToWatch(String assetFileName, String modelDisplayInfo) {
        modelSender.sendModelToWatch(getApplication(), assetFileName, modelDisplayInfo);
    }

    /** Yêu cầu đồng hồ đo & chẩn đoán thật (chạy nền, không chặn UI). */
    public void requestDiagnosis() {
        io.execute(() -> diagnosisSender.requestDiagnosisBlocking(getApplication()));
    }

    /** Kiểm tra đồng hồ có đang kết nối không rồi cập nhật {@link #connectionState}. */
    public void refreshConnection() {
        connectionState.postValue(STATE_CHECKING);
        Wearable.getNodeClient(getApplication()).getConnectedNodes()
                .addOnSuccessListener(nodes ->
                        connectionState.postValue(nodes.isEmpty() ? STATE_DISCONNECTED : STATE_CONNECTED))
                .addOnFailureListener(e -> connectionState.postValue(STATE_DISCONNECTED));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        io.shutdown();
    }
}
