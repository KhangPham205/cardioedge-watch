# 09 — Sơ đồ tổng quan

[← Quay lại mục lục](README.md)

> Các sơ đồ dùng cú pháp **Mermaid** (render được trên GitHub, VS Code + plugin, hoặc mermaid.live). Kèm bản ASCII cho môi trường không hỗ trợ.

## Kiến trúc hệ thống & quan hệ module

```mermaid
graph LR
    subgraph WEAR["⌚ WEAR (Galaxy Watch 7)"]
        SENSOR["Samsung Health<br/>Sensor API<br/>(ECG_ON_DEMAND)"]
        WMAIN["MainActivity<br/>buffer + normalize"]
        CLF["ECGClassifier<br/>(TFLite CNN-1D)"]
        SENSOR --> WMAIN --> CLF --> WMAIN
    end

    subgraph MOBILE["📱 MOBILE (Phone)"]
        MMAIN["MainActivity<br/>(menu)"]
        SENDER["ModelSender"]
        LISTENER["EcgListenerService"]
        DB[("Room DB<br/>ecg_results")]
        HIST["HistoryActivity<br/>(MPAndroidChart)"]
        WORKER["AutoEcgWorker<br/>(WorkManager)"]
        SETT["SettingsActivity"]
        MMAIN --> SENDER
        MMAIN --> HIST
        MMAIN --> SETT
        LISTENER --> DB
        DB --> HIST
        DB --> WORKER
        SETT --> WORKER
    end

    WMAIN -- "/ecg_result (kết quả)" --> LISTENER
    SENDER -- "/model_update (Asset .tflite)" --> WMAIN
```

## Luồng dữ liệu

```mermaid
flowchart TD
    A["Cảm biến ECG (mV, float)"] --> B["onDataReceived<br/>lọc val != 0"]
    B --> C["ecgBuffer (gom 200 mẫu)"]
    C --> D["normalizeData (min-max [0,1])"]
    D --> E["ECGClassifier.predict<br/>TFLite → float[2]"]
    E --> F["UI đồng hồ<br/>tvResult / tvStatus"]
    E --> G["sendToPhone<br/>/ecg_result"]
    G --> H["EcgListenerService"]
    H --> I[("Room DB")]
    I --> J["getLiveResults(200)<br/>→ moving avg(4) → LineChart"]
    I --> K["getHistoryResults<br/>AVG/phút → BarChart"]
    I --> L["AutoEcgWorker<br/>đánh giá + cảnh báo"]
```

## Luồng xử lý (state machine — phía đồng hồ)

```mermaid
stateDiagram-v2
    [*] --> onCreate
    onCreate --> LoadModel: khởi tạo ECGClassifier
    LoadModel --> CheckPerm: checkPermissionsAndRun
    CheckPerm --> Connect: quyền OK
    CheckPerm --> Denied: thiếu quyền
    Connect --> Tracking: onConnectionSuccess<br/>startECGTracking
    Tracking --> ProcessAI: onDataReceived
    ProcessAI --> ProcessAI: buffer < 200 (chờ)
    ProcessAI --> Predict: đủ 200 → normalize
    Predict --> Tracking: hiển thị + sendToPhone<br/>trượt 50 mẫu
    Tracking --> [*]: onDestroy (giải phóng)

    state "Song song" as par {
        [*] --> Listen: onResume
        Listen --> Reload: /model_update
        Reload --> Listen: reloadModelFromFile
    }
```

## Bản ASCII — kiến trúc tổng thể

```
        ┌───────────────────────────┐        Wearable Data Layer         ┌──────────────────────────────┐
        │      WEAR (Galaxy Watch)   │      (DataItem qua BT / Wi-Fi)     │        MOBILE (Phone)         │
        │                           │                                     │                              │
        │  Samsung Health Sensor API│                                     │  ┌────────────────────────┐  │
        │            │              │                                     │  │  MainActivity (menu)   │  │
        │            ▼              │      "/model_update" (Asset)        │  │   ├─ ModelSender ──────┼──┼──► gửi .tflite
        │  ┌──────────────────┐     │  ◄──────────────────────────────────┼──┤   ├─ btnHistory        │  │
        │  │   MainActivity   │     │                                     │  │   └─ btnSettings       │  │
        │  │ (buffer+normalize)│    │      "/ecg_result" (kết quả)        │  └────────────────────────┘  │
        │  │        │         │     │  ──────────────────────────────────►│  ┌────────────────────────┐  │
        │  │        ▼         │     │                                     │  │  EcgListenerService    │  │
        │  │  ECGClassifier   │     │                                     │  │        │ insert        │  │
        │  │  (TFLite CNN-1D) │     │                                     │  │        ▼               │  │
        │  └──────────────────┘     │                                     │  │  Room DB (ecg_results) │  │
        │                           │                                     │  │   │            │        │  │
        └───────────────────────────┘                                     │  │   ▼            ▼        │  │
                                                                          │  │ AutoEcgWorker  HistoryActivity
                                                                          │  │ (WorkManager)  (MPAndroidChart)
                                                                          │  │  + Cảnh báo    Live/History  │
                                                                          │  └────────────────────────┘  │
                                                                          │        ▲ SettingsActivity      │
                                                                          └──────────────────────────────┘
```

---

[← Quay lại mục lục](README.md)