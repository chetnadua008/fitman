package com.example.fitman.period;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitman.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CycleActivity extends AppCompatActivity {

    private LocalDate month = LocalDate.now().withDayOfMonth(1);
    private CycleAdapter adapter;
    private TextView tvMonth, tvSummary, tvCycleDay;
    private Button btnNext;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle);

        tvCycleDay = findViewById(R.id.tvCycleDay);
        tvMonth = findViewById(R.id.tvMonth);
        tvSummary = findViewById(R.id.tvSummary);
        btnNext = findViewById(R.id.btnNext);
        progress = findViewById(R.id.progress);

        // Mon..Sun header
        LinearLayout row = findViewById(R.id.weekdayRow);
        for (String n : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}) {
            TextView t = new TextView(this);
            t.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            t.setGravity(Gravity.CENTER);
            t.setText(n);
            row.addView(t);
        }

        RecyclerView rv = findViewById(R.id.rvDays);
        rv.setLayoutManager(new GridLayoutManager(this, 7));
        adapter = new CycleAdapter(this::showDay);
        rv.setAdapter(adapter);

        findViewById(R.id.btnPrev).setOnClickListener(v -> { month = month.minusMonths(1); loadMonth(); });
        btnNext.setOnClickListener(v -> { month = month.plusMonths(1); loadMonth(); });

        loadMonth();
        loadCycleInfo();
    }

    private void loadMonth() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        tvMonth.setText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())));
        btnNext.setEnabled(month.isBefore(LocalDate.now().withDayOfMonth(1)));
        progress.setVisibility(View.VISIBLE);

        // Look 3 days before the 1st too, so day 1's "period start" flag is correct
        // even when the streak began in the previous month.
        LocalDate rangeStart = month.minusDays(3);
        LocalDate rangeEnd = month.withDayOfMonth(month.lengthOfMonth());

        CycleSync.loadRange(rangeStart, rangeEnd)
                .addOnSuccessListener(flowByDate -> {
                    Map<String, CycleAdapter.DayData> map = new HashMap<>();
                    int loggedDays = 0;
                    for (int day = 1; day <= month.lengthOfMonth(); day++) {
                        LocalDate date = month.withDayOfMonth(day);
                        int flow = getOrZero(flowByDate, date);
                        boolean periodStart = flow > FlowLevel.NONE
                                && getOrZero(flowByDate, date.minusDays(1)) == FlowLevel.NONE;
                        if (flow > FlowLevel.NONE) loggedDays++;
                        map.put(date.toString(), new CycleAdapter.DayData(flow, periodStart));
                    }
                    adapter.setData(month, map);
                    tvSummary.setText("Period days logged this month: " + loggedDays);
                    progress.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("Cycle", "load failed", e);
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load data", Toast.LENGTH_LONG).show();
                });
    }

    private int getOrZero(Map<String, Integer> map, LocalDate date) {
        Integer v = map.get(date.toString());
        return v == null ? FlowLevel.NONE : v;
    }

    private void loadCycleInfo() {
        CycleSync.loadRecentPeriodDates()
                .addOnSuccessListener(dates -> {
                    CycleRules.CycleInfo info = CycleRules.compute(dates, LocalDate.now());
                    if (info.cycleDay < 0) {
                        tvCycleDay.setText("No period logged yet — tap a day below to add one");
                    } else {
                        StringBuilder sb = new StringBuilder("Cycle Day ").append(info.cycleDay);
                        if (info.onPeriod) sb.append("  •  Period day ").append(info.periodDay);
                        if (info.avgCycleLengthDays != null)
                            sb.append("  •  Avg cycle ").append(info.avgCycleLengthDays).append("d");
                        tvCycleDay.setText(sb.toString());
                    }
                })
                .addOnFailureListener(e -> Log.e("Cycle", "info failed", e));
    }

    private void showDay(LocalDate date, CycleAdapter.DayData d) {
        if (date.isAfter(LocalDate.now())) {
            Toast.makeText(this, "That day hasn't happened yet", Toast.LENGTH_SHORT).show();
            return;
        }
        String title = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()));
        final int[] selected = {d.flow};

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(FlowLevel.LABELS, d.flow, (dialog, which) -> selected[0] = which)
                .setPositiveButton("Save", (dialog, which) -> saveFlow(date, selected[0]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveFlow(LocalDate date, int flow) {
        progress.setVisibility(View.VISIBLE);
        CycleSync.logFlow(date, flow)
                .addOnSuccessListener(v -> { loadMonth(); loadCycleInfo(); })
                .addOnFailureListener(e -> {
                    Log.e("Cycle", "save failed", e);
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not save", Toast.LENGTH_LONG).show();
                });
    }
}
