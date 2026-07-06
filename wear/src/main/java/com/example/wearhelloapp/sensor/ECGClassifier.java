package com.example.wearhelloapp.sensor;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Bọc TensorFlow Lite Interpreter cho bài toán phân loại ECG.
 *
 * An toàn luồng: mọi truy cập tới {@link #interpreter} đều nằm trong khối
 * {@code synchronized (lock)} nên việc hot-swap model ({@link #reloadModelFromFile})
 * không thể xảy ra đồng thời với inference ({@link #predict}) → tránh
 * NullPointerException / crash native khi đóng interpreter đang chạy.
 *
 * Shape input/output được đọc động từ chính model thay vì fix cứng, nên model mới
 * gửi sang có kích thước khác vẫn chạy đúng.
 */
public class ECGClassifier {
    private static final String TAG = "ECGClassifier";

    private final Object lock = new Object();
    private Interpreter interpreter;

    // Được suy ra từ model tại thời điểm load (giá trị mặc định chỉ là fallback).
    private int inputLength = 200;
    private int inputChannels = 1;
    private int outputClasses = 2;

    public ECGClassifier(AssetManager assetManager, String modelPath) throws IOException {
        MappedByteBuffer model = loadModelFile(assetManager, modelPath);
        synchronized (lock) {
            loadInterpreter(model);
        }
    }

    /** Load lại model từ file đã tải về (hot-swap). An toàn với luồng inference. */
    public void reloadModelFromFile(File modelFile) throws IOException {
        // Map file ngoài critical-section để giảm thời gian giữ lock.
        MappedByteBuffer mappedByteBuffer;
        try (FileInputStream inputStream = new FileInputStream(modelFile);
             FileChannel fileChannel = inputStream.getChannel()) {
            mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
        }

        synchronized (lock) {
            if (interpreter != null) {
                interpreter.close(); // Đóng model cũ
                interpreter = null;
            }
            loadInterpreter(mappedByteBuffer);
        }
        Log.d(TAG, "Đã reload model mới thành công! inputLength=" + inputLength
                + " channels=" + inputChannels + " classes=" + outputClasses);
    }

    /** Phải được gọi trong khối synchronized(lock). */
    private void loadInterpreter(MappedByteBuffer buffer) {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(2);
        interpreter = new Interpreter(buffer, options);
        readShapes();
    }

    /** Đọc shape thực tế của model. Phải được gọi trong khối synchronized(lock). */
    private void readShapes() {
        try {
            int[] inShape = interpreter.getInputTensor(0).shape(); // vd [1, 200, 1] hoặc [1, 200]
            if (inShape.length >= 3) {
                inputLength = inShape[1];
                inputChannels = inShape[2];
            } else if (inShape.length == 2) {
                inputLength = inShape[1];
                inputChannels = 1;
            }
            int[] outShape = interpreter.getOutputTensor(0).shape(); // vd [1, 2]
            outputClasses = outShape[outShape.length - 1];
        } catch (Exception e) {
            Log.w(TAG, "Không đọc được shape từ model, dùng giá trị mặc định", e);
        }
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /** Số mẫu đầu vào mà model hiện tại yêu cầu (để tầng gọi buffer đúng lượng). */
    public int getInputLength() {
        synchronized (lock) {
            return inputLength;
        }
    }

    public int getOutputClasses() {
        synchronized (lock) {
            return outputClasses;
        }
    }

    public float[] predict(float[] signal) {
        synchronized (lock) {
            if (interpreter == null) {
                throw new IllegalStateException("Interpreter chưa được khởi tạo");
            }
            if (signal.length != inputLength) {
                throw new IllegalArgumentException(
                        "Expected input length = " + inputLength + " but got " + signal.length);
            }

            float[][][] input = new float[1][inputLength][inputChannels];
            for (int i = 0; i < inputLength; i++) {
                input[0][i][0] = signal[i];
            }

            float[][] output = new float[1][outputClasses];
            interpreter.run(input, output);
            return output[0];
        }
    }

    public void close() {
        synchronized (lock) {
            if (interpreter != null) {
                interpreter.close();
                interpreter = null;
            }
        }
    }
}
