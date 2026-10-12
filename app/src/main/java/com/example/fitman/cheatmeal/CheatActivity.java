package com.example.fitman.cheatmeal;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitman.cheatmeal.CheatAdapter;
import com.example.fitman.cheatmeal.CheatLevel;
import com.example.fitman.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

public class CheatActivity extends AppCompatActivity {

    private LocalDate month = LocalDate.now().withDayOfMonth(1);
    private CheatAdapter adapter;
    private TextView tvMonth, tvSummary, tvHeader;
    private Button btnNext;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cheat);

        tvHeader = findViewById(R.id.tvHeader);
        tvMonth = findViewById(R.id.tvMonth);
        tvSummary = findViewById(R.id.tvSummary);
        btnNext = findViewById(R.id.btnNext);
        progress = findViewById(R.id.progress);

        // Mon..Sun header row
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
        adapter = new CheatAdapter(this::showDay);
        rv.setAdapter(adapter);

        findViewById(R.id.btnPrev).setOnClickListener(v -> { month = month.minusMonths(1); loadMonth(); });
        btnNext.setOnClickListener(v -> { month = month.plusMonths(1); loadMonth(); });

        loadMonth();
        loadStreak();
    }

    private void loadMonth() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        tvMonth.setText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())));
        btnNext.setEnabled(month.isBefore(LocalDate.now().withDayOfMonth(1)));
        progress.setVisibility(View.VISIBLE);

        LocalDate end = month.withDayOfMonth(month.lengthOfMonth());
        CheatSync.loadRange(month, end)
                .addOnSuccessListener(map -> {
                    int light = 0, medium = 0, high = 0;
                    for (CheatSync.Entry e : map.values()) {
                        if (e.level == CheatLevel.LIGHT) light++;
                        else if (e.level == CheatLevel.MEDIUM) medium++;
                        else if (e.level == CheatLevel.HIGH) high++;
                    }
                    adapter.setData(month, map);
                    int total = light + medium + high;
                    tvSummary.setText("Cheat days this month: " + total
                            + "  (" + light + " light • " + medium + " medium • " + high + " high)");
                    progress.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("Cheat", "load failed", e);
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load data", Toast.LENGTH_LONG).show();
                });
    }

    /** Header: days since the last cheat meal (like the cycle-day header in the cycle tracker). */
    private void loadStreak() {
        CheatSync.loadLastCheatDate()
                .addOnSuccessListener(last -> {
                    if (last == null) {
                        tvHeader.setText("No cheat meals in the last 90 days");
                        return;
                    }
                    long days = ChronoUnit.DAYS.between(last, LocalDate.now());
                    if (days == 0) {
                        tvHeader.setText("Cheat meal logged today");
                    } else {
                        tvHeader.setText("Clean streak: " + days + (days == 1 ? " day" : " days"));
                    }
                })
                .addOnFailureListener(e -> Log.e("Cheat", "streak failed", e));
    }

    private void showDay(LocalDate date, CheatSync.Entry entry) {
        if (date.isAfter(LocalDate.now())) {
            Toast.makeText(this, "That day hasn't happened yet", Toast.LENGTH_SHORT).show();
            return;
        }
        String title = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()));

        View view = getLayoutInflater().inflate(R.layout.dialog_cheat, null);
        final EditText etMeal = view.findViewById(R.id.etMeal);
        final RadioGroup rg = view.findViewById(R.id.rgLevel);

        etMeal.setText(entry.meal);
        // unlogged day starts on Medium so a quick "Save" logs something sensible
        rg.check(idFor(entry.level == CheatLevel.NONE ? CheatLevel.MEDIUM : entry.level));

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Save", (dialog, which) ->
                        saveCheat(date, levelFor(rg.getCheckedRadioButtonId()),
                                etMeal.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int idFor(int level) {
        switch (level) {
            case CheatLevel.LIGHT:  return R.id.rbLight;
            case CheatLevel.MEDIUM: return R.id.rbMedium;
            case CheatLevel.HIGH:   return R.id.rbHigh;
            default:                return R.id.rbNone;
        }
    }

    private int levelFor(int checkedId) {
        if (checkedId == R.id.rbLight)  return CheatLevel.LIGHT;
        if (checkedId == R.id.rbMedium) return CheatLevel.MEDIUM;
        if (checkedId == R.id.rbHigh)   return CheatLevel.HIGH;
        return CheatLevel.NONE;
    }

    private void saveCheat(LocalDate date, int level, String meal) {
        progress.setVisibility(View.VISIBLE);
        CheatSync.logCheat(date, level, meal)
                .addOnSuccessListener(v -> { loadMonth(); loadStreak(); })
                .addOnFailureListener(e -> {
                    Log.e("Cheat", "save failed", e);
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not save", Toast.LENGTH_LONG).show();
                });
    }
}
