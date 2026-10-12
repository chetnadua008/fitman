package com.example.fitman;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.Gravity;
import android.Manifest;
import android.location.Location;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.fitman.authentication.LoginActivity;
import com.example.fitman.calendar.CalendarActivity;
import com.example.fitman.calendar.DirectoryHelper;
import com.example.fitman.cheatmeal.CheatActivity;
import com.example.fitman.sync.ChallengeSync;
import com.example.fitman.challenge.ChallengesActivity;
import com.example.fitman.challenge.NotificationHelper;
import com.example.fitman.gamification.GameRules;
import com.example.fitman.sync.StepSync;
import com.example.fitman.sync.StepWorker;
import com.example.fitman.gamification.WeeklyBarChartView;
import com.example.fitman.period.CycleActivity;
import com.example.fitman.weather.WalkAdvisor;
import com.example.fitman.weather.WeatherData;
import com.example.fitman.weather.WeatherService;
import com.example.fitman.weather.WeatherWorker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.TimeUnit;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private FusedLocationProviderClient fusedLocationClient;
    private long lastSyncMillis = 0;
    private TextView tvHello, tvSize, tvSteps, tvPoints, tvWeekly, tvStatus;
    private ProgressBar bar;
    private ImageView imgMan;
    private WeeklyBarChartView weekChart;

    private TextView tvWeather;

    private DrawerLayout drawerLayout;
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startTracking();
                else tvStatus.setText("Permission denied. Enable 'Physical activity' in app settings.");
            });
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {

                        Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);

                        if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                            loadWeather();
                        } else {
                            tvWeather.setText("Location permission is needed for local weather.");
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { goToLogin(); return; }

        setContentView(R.layout.activity_main);
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);
        drawerLayout = findViewById(R.id.drawerLayout);

        findViewById(R.id.btnMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(Gravity.LEFT));
        tvHello = findViewById(R.id.tvHello);
        tvSize = findViewById(R.id.tvSize);
        tvSteps = findViewById(R.id.tvSteps);
        tvPoints = findViewById(R.id.tvPoints);
        tvWeekly = findViewById(R.id.tvWeekly);
        tvStatus = findViewById(R.id.tvStatus);
        weekChart = findViewById(R.id.weekChart);
        tvWeather = findViewById(R.id.tvWeather);
        tvWeather.setText("Checking weather...");
        requestLocationAndLoadWeather();

        bar = findViewById(R.id.bar);
        imgMan = findViewById(R.id.imgMan);
        Button logout = findViewById(R.id.btnLogout);

        tvHello.setText("Hi, " + user.getDisplayName());
        logout.setOnClickListener(v -> signOut());
        findViewById(R.id.btnChallenges).setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.LEFT);
            startActivity(new Intent(this, ChallengesActivity.class));
        });

        DirectoryHelper.registerSelf(user);
        NotificationHelper.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        findViewById(R.id.btnCalendar).setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.LEFT);
            startActivity(new Intent(this, CalendarActivity.class));
        });

        findViewById(R.id.btnCycle).setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.LEFT);
            startActivity(new Intent(this, CycleActivity.class));
        });

        findViewById(R.id.btnCheatMeal).setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.LEFT);
            startActivity(new Intent(this, CheatActivity.class));
        });


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor == null) {
            tvStatus.setText("This phone has no step counter sensor.");
            return;
        }
        ensurePermission();
        loadWeeklyChart();
    }

    private void ensurePermission() {
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
        } else {
            startTracking();
        }
    }

    private void startTracking() {
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        tvStatus.setText("Tracking steps...");

        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(StepWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("step-sync", ExistingPeriodicWorkPolicy.KEEP, req);
        PeriodicWorkRequest weatherReq = new PeriodicWorkRequest.Builder(WeatherWorker.class, 30, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork("weather-check", ExistingPeriodicWorkPolicy.KEEP, weatherReq);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lastSyncMillis = 0;                       // sync right away on return
        if (stepSensor != null && hasPermission()) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
        ChallengeSync.deliverPending(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    private boolean hasPermission() {
        return Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = System.currentTimeMillis();
        if (now - lastSyncMillis < 20_000) return;   // write at most every 20 s
        lastSyncMillis = now;

        StepSync.sync((long) event.values[0])
                .addOnSuccessListener(this::showResult)
                .addOnFailureListener(e -> {
                    Log.e("Sync", "failed", e);
                    tvStatus.setText("Sync failed (offline?). Will retry.");
                });
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void showResult(StepSync.SyncResult r) {
        tvSteps.setText(r.steps + " steps");
        bar.setProgress((int) Math.min(r.steps, GameRules.STEPS_PER_POINT));
        tvPoints.setText("Total points: " + r.totalPoints);
        tvWeekly.setText("This week: " + r.weeklyPoints);
        tvStatus.setText("Synced to cloud");

        float scale; String label;
        switch (GameRules.sizeFor(r.effectivePoints)) {
            case GameRules.LARGE:  scale = 1.0f;  label = "Strong"; break;
            case GameRules.MEDIUM: scale = 0.75f; label = "Fit";    break;
            default:               scale = 0.5f;  label = "Starter"; break;
        }
        tvSize.setText(label);
        imgMan.animate().scaleX(scale).scaleY(scale).setDuration(500).start();
        loadWeeklyChart();
        ChallengeSync.pushProgress(r.steps);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        WorkManager.getInstance(this).cancelUniqueWork("step-sync");
        CredentialManager.create(this).clearCredentialStateAsync(
                new ClearCredentialStateRequest(), null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override public void onResult(Void v) { goToLogin(); }
                    @Override public void onError(ClearCredentialException e) { goToLogin(); }
                });
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
    private void loadWeeklyChart() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.temporal.TemporalAdjusters
                .previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).collection("days")
                .whereGreaterThanOrEqualTo("date", weekStart.toString())
                .whereLessThanOrEqualTo("date", weekEnd.toString())
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, Long> stepsMap = new HashMap<>();
                    Map<String, Boolean> earnedMap = new HashMap<>();
                    for (DocumentSnapshot d : qs) {
                        Long s = d.getLong("steps");
                        stepsMap.put(d.getId(), s == null ? 0 : s);
                        earnedMap.put(d.getId(), Boolean.TRUE.equals(d.getBoolean("pointEarned")));
                    }
                    weekChart.setWeek(weekStart, stepsMap, earnedMap);
                })
                .addOnFailureListener(e -> Log.e("WeekChart", "load failed", e));
    }
    private void loadWeather() {

        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            tvWeather.setText("Location permission is needed for local weather.");
            return;
        }

        fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
        ).addOnSuccessListener(location -> {

            if (location == null) {
                tvWeather.setText("Couldn't determine your location.");
                return;
            }

            double lat = location.getLatitude();
            double lon = location.getLongitude();

            Log.d("Weather", "GPS location: " + lat + ", " + lon);

            WeatherService.fetch(
                    lat,
                    lon,
                    new WeatherService.Callback() {

                        @Override
                        public void onResult(WeatherData data) {
                            WalkAdvisor.Advice advice =
                                    WalkAdvisor.evaluate(data);

                            tvWeather.setText(advice.message);

                            tvWeather.setTextColor(
                                    advice.goodRightNow
                                            ? 0xFF2E7D32
                                            : 0xFF616161
                            );
                        }

                        @Override
                        public void onError(Exception e) {
                            tvWeather.setText(
                                    "Couldn't load weather right now."
                            );
                        }
                    }
            );

        }).addOnFailureListener(e -> {
            Log.e("Weather", "Location failed", e);
            tvWeather.setText("Couldn't determine your location.");
        });
    }
    private void requestLocationAndLoadWeather() {

        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {

            locationPermissionLauncher.launch(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }
            );

            return;
        }

        loadWeather();
    }
}