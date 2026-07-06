package com.example.wearhelloapp.activity;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.wearhelloapp.R;
import com.example.wearhelloapp.database.EcgResult;
import com.example.wearhelloapp.viewmodel.HistoryViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình lịch sử. Đã bỏ hoàn toàn việc poll DB mỗi giây bằng Handler và
 * {@code allowMainThreadQueries()} — nay quan sát LiveData từ {@link HistoryViewModel},
 * Room tự phát lại dữ liệu khi có bản ghi mới.
 */
public class HistoryActivity extends AppCompatActivity {

    private LineChart chartLive;
    private BarChart chartHistory;
    private Spinner spinner;
    private TextView tvDateInfo;

    private HistoryViewModel viewModel;
    private boolean isLiveMode = true;

    // Bộ nhớ đệm dữ liệu mới nhất để vẽ lại khi người dùng đổi tab (không cần truy vấn lại).
    private List<EcgResult> latestLive = new ArrayList<>();
    private List<EcgResult> latestHistory = new ArrayList<>();

    private final List<String> modelNames = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        chartLive = findViewById(R.id.chartLive);
        chartHistory = findViewById(R.id.chartHistory);
        spinner = findViewById(R.id.spinnerModels);
        RadioGroup rgTabs = findViewById(R.id.rgTabs);
        tvDateInfo = findViewById(R.id.tvDateInfo);

        RadioButton rbLive = findViewById(R.id.rbLive);
        RadioButton rbHistory = findViewById(R.id.rbHistory);

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        setupChartsBaseStyle();
        setupSpinner();

        updateTabUI(rbLive, true);
        updateTabUI(rbHistory, false);

        rgTabs.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbLive) {
                isLiveMode = true;
                chartLive.setVisibility(View.VISIBLE);
                chartHistory.setVisibility(View.GONE);
                updateTabUI(rbLive, true);
                updateTabUI(rbHistory, false);
                renderLive(latestLive);
            } else {
                isLiveMode = false;
                chartLive.setVisibility(View.GONE);
                chartHistory.setVisibility(View.VISIBLE);
                updateTabUI(rbHistory, true);
                updateTabUI(rbLive, false);
                renderHistory(latestHistory);
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        observeData();
    }

    private void observeData() {
        // Danh sách model → cập nhật spinner
        viewModel.getModelNames().observe(this, names -> {
            modelNames.clear();
            if (names == null || names.isEmpty()) {
                modelNames.add("Chưa có dữ liệu");
            } else {
                modelNames.addAll(names);
            }
            spinnerAdapter.notifyDataSetChanged();
            // Chọn mặc định phần tử đầu nếu chưa chọn gì
            if (spinner.getSelectedItemPosition() < 0 && !modelNames.isEmpty()) {
                spinner.setSelection(0);
            }
        });

        // Dữ liệu LIVE — tự cập nhật khi có bản ghi mới
        viewModel.getLiveResults().observe(this, results -> {
            latestLive = results != null ? results : new ArrayList<>();
            if (isLiveMode) renderLive(latestLive);
        });

        // Dữ liệu HISTORY (đã gom nhóm theo phút)
        viewModel.getHistoryResults().observe(this, results -> {
            latestHistory = results != null ? results : new ArrayList<>();
            if (!isLiveMode) renderHistory(latestHistory);
        });
    }

    private void updateTabUI(RadioButton rb, boolean isSelected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(12);
        if (isSelected) {
            shape.setColor(Color.parseColor("#00796B"));
            rb.setTextColor(Color.WHITE);
        } else {
            shape.setColor(Color.TRANSPARENT);
            rb.setTextColor(Color.parseColor("#546E7A"));
        }
        rb.setBackground(shape);
    }

    private void setupChartsBaseStyle() {
        // Style cho Live Chart
        chartLive.getDescription().setEnabled(false);
        chartLive.setDrawGridBackground(false);
        chartLive.getAxisRight().setEnabled(false);
        chartLive.getLegend().setEnabled(false);
        chartLive.setNoDataText("Đang tải dữ liệu...");

        XAxis xLive = chartLive.getXAxis();
        xLive.setPosition(XAxis.XAxisPosition.BOTTOM);
        xLive.setDrawGridLines(false);
        xLive.setTextColor(Color.parseColor("#90A4AE"));
        xLive.setGranularity(1f);

        YAxis yLive = chartLive.getAxisLeft();
        yLive.setAxisMinimum(0f);
        yLive.setAxisMaximum(110f);
        yLive.setDrawGridLines(true);
        yLive.setGridColor(Color.parseColor("#ECEFF1"));

        LimitLine ll = new LimitLine(70f, "Cảnh báo");
        ll.setLineColor(Color.parseColor("#FF5252"));
        ll.setLineWidth(1.5f);
        ll.setTextColor(Color.parseColor("#FF5252"));
        yLive.addLimitLine(ll);

        // Style tương tự cho History Chart
        chartHistory.getDescription().setEnabled(false);
        chartHistory.setDrawGridBackground(false);
        chartHistory.getAxisRight().setEnabled(false);
        chartHistory.getLegend().setEnabled(false);

        XAxis xHist = chartHistory.getXAxis();
        xHist.setPosition(XAxis.XAxisPosition.BOTTOM);
        xHist.setDrawGridLines(false);
        xHist.setGranularity(1f);

        YAxis yHist = chartHistory.getAxisLeft();
        yHist.setAxisMinimum(0f);
        yHist.setAxisMaximum(110f);
        yHist.setDrawGridLines(true);
        yHist.setGridColor(Color.parseColor("#ECEFF1"));
    }

    private void setupSpinner() {
        spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modelNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = modelNames.get(position);
                if (!"Chưa có dữ liệu".equals(selected)) {
                    viewModel.selectModel(selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void renderLive(List<EcgResult> source) {
        if (source == null || source.isEmpty()) return;

        // observeLiveResults trả về DESC (mới nhất trước) → đảo về thứ tự thời gian tăng dần.
        List<EcgResult> results = new ArrayList<>(source);
        Collections.reverse(results);

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDateInfo.setText("Dữ liệu ngày: " + dayFormat.format(new Date(results.get(results.size() - 1).timestamp)));

        ArrayList<Entry> entries = new ArrayList<>();
        final ArrayList<Long> timestamps = new ArrayList<>();
        int windowSize = 4;
        float sum = 0f;
        java.util.LinkedList<Float> windowQueue = new java.util.LinkedList<>();

        for (int i = 0; i < results.size(); i++) {
            float rawProb = results.get(i).abnormalProb * 100;
            timestamps.add(results.get(i).timestamp);
            sum += rawProb;
            windowQueue.add(rawProb);
            if (windowQueue.size() > windowSize) sum -= windowQueue.removeFirst();
            float smoothedProb = sum / windowQueue.size();
            entries.add(new Entry(i, smoothedProb));
        }

        LineDataSet set = new LineDataSet(entries, "Nguy cơ");
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawCircles(false);
        set.setLineWidth(3f);
        set.setColor(Color.parseColor("#00796B"));
        set.setDrawFilled(true);

        set.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#4DB6AC"), Color.parseColor("#00FFFFFF")}));

        LineData data = new LineData(set);
        data.setDrawValues(false);

        chartLive.getXAxis().setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < timestamps.size()) return mFormat.format(new Date(timestamps.get(index)));
                return "";
            }
        });

        chartLive.setData(data);
        chartLive.setVisibleXRangeMaximum(15f);
        chartLive.moveViewToX(entries.size());
        chartLive.invalidate();
    }

    private void renderHistory(List<EcgResult> source) {
        if (source == null || source.isEmpty()) return;

        List<EcgResult> results = source;

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        String range = dayFormat.format(new Date(results.get(0).timestamp)) + " - " +
                dayFormat.format(new Date(results.get(results.size() - 1).timestamp));
        tvDateInfo.setText("Khoảng thời gian: " + range);

        ArrayList<BarEntry> entries = new ArrayList<>();
        final ArrayList<Long> timestamps = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            float abnormalPercent = results.get(i).abnormalProb * 100;
            entries.add(new BarEntry(i, abnormalPercent));
            timestamps.add(results.get(i).timestamp);
        }

        BarDataSet set = new BarDataSet(entries, "Trung bình");
        List<Integer> colors = new ArrayList<>();
        for (BarEntry e : entries) {
            if (e.getY() >= 70f) colors.add(Color.parseColor("#E53935"));
            else colors.add(Color.parseColor("#00796B"));
        }
        set.setColors(colors);

        BarData data = new BarData(set);
        data.setDrawValues(false);
        data.setBarWidth(0.6f);

        chartHistory.getXAxis().setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < timestamps.size()) return mFormat.format(new Date(timestamps.get(index)));
                return "";
            }
        });

        chartHistory.setData(data);
        chartHistory.setVisibleXRangeMaximum(10f);
        chartHistory.animateY(1000);
        chartHistory.invalidate();
    }
}
