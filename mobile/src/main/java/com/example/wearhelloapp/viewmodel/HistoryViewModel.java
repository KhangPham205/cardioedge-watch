package com.example.wearhelloapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.wearhelloapp.data.EcgRepository;
import com.example.wearhelloapp.database.EcgResult;

import java.util.List;

/**
 * ViewModel cho HistoryActivity. Giữ model đang chọn và trả về LiveData phản ứng.
 * Khi có bản ghi ECG mới, Room tự phát lại dữ liệu → biểu đồ tự cập nhật,
 * không cần Handler poll mỗi giây như trước.
 */
public class HistoryViewModel extends AndroidViewModel {

    private final MutableLiveData<String> selectedModel = new MutableLiveData<>();

    private final LiveData<List<String>> modelNames;
    private final LiveData<List<EcgResult>> liveResults;
    private final LiveData<List<EcgResult>> historyResults;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        EcgRepository repo = EcgRepository.getInstance(application);
        modelNames = repo.observeModelNames();
        liveResults = Transformations.switchMap(selectedModel, repo::observeLive);
        historyResults = Transformations.switchMap(selectedModel, repo::observeHistory);
    }

    public void selectModel(String modelName) {
        if (modelName != null && !modelName.equals(selectedModel.getValue())) {
            selectedModel.setValue(modelName);
        }
    }

    public LiveData<List<String>> getModelNames() {
        return modelNames;
    }

    public LiveData<List<EcgResult>> getLiveResults() {
        return liveResults;
    }

    public LiveData<List<EcgResult>> getHistoryResults() {
        return historyResults;
    }
}
