package com.example.fitman.calendar;

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
import com.example.fitman.gamification.GameRules;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CalendarActivity extends AppCompatActivity {

    private LocalDate month = LocalDate.now().withDayOfMonth(1);
    private CalendarAdapter adapter;
    private TextView tvMonth, tvSummary;
    private Button btnNext;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

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
        adapter = new CalendarAdapter(this::showDay);
        rv.setAdapter(adapter);

        findViewById(R.id.btnPrev).setOnClickListener(v -> { month = month.minusMonths(1); loadMonth(); });
        btnNext.setOnClickListener(v -> { month = month.plusMonths(1); loadMonth(); });

        loadMonth();
    }

    private void loadMonth() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        tvMonth.setText(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())));
        btnNext.setEnabled(month.isBefore(LocalDate.now().withDayOfMonth(1)));
        progress.setVisibility(View.VISIBLE);

        String start = month.toString();
        String end = month.withDayOfMonth(month.lengthOfMonth()).toString();

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).collection("days")
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThanOrEqualTo("date", end)
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, CalendarAdapter.DayData> map = new HashMap<>();
                    long totalSteps = 0; int points = 0;
                    for (DocumentSnapshot d : qs) {
                        Long s = d.getLong("steps");
                        boolean p = Boolean.TRUE.equals(d.getBoolean("pointEarned"));
                        long steps = s == null ? 0 : s;
                        map.put(d.getId(), new CalendarAdapter.DayData(steps, p));
                        totalSteps += steps;
                        if (p) points++;
                    }
                    adapter.setData(month, map);
                    tvSummary.setText("Points: " + points + "   •   Steps: "
                            + NumberFormat.getInstance().format(totalSteps));
                    progress.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("Calendar", "load failed", e);
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Could not load data", Toast.LENGTH_LONG).show();
                });
    }

    private void showDay(LocalDate date, CalendarAdapter.DayData d) {
        if (date.isAfter(LocalDate.now())) {
            Toast.makeText(this, "That day hasn't happened yet", Toast.LENGTH_SHORT).show();
            return;
        }
        NumberFormat nf = NumberFormat.getInstance();
        String title = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()));
        String msg = "Steps: " + nf.format(d.steps)
                + "\nPoint earned: " + (d.pointEarned ? "Yes" : "No");
        if (!d.pointEarned) {
            long left = Math.max(0, GameRules.STEPS_PER_POINT - d.steps);
            msg += "\n(" + nf.format(left) + " more steps needed)";
        }
        new AlertDialog.Builder(this)
                .setTitle(title).setMessage(msg).setPositiveButton("OK", null).show();
    }
}