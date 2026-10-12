package com.example.fitman.weather;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fitman.challenge.NotificationHelper;

import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.google.android.gms.tasks.Tasks;
public class WeatherWorker extends Worker {

    private static String lastNotifiedSlot = null;   // avoids duplicate pings within one process lifetime

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        LocalTime now = LocalTime.now();

        if (now.isBefore(LocalTime.of(8, 0))
                || now.isAfter(LocalTime.of(18, 0))) {

            return Result.success();   // outside daytime walk hours
        }

        // Check location permission
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(
                        getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            return Result.success();
        }

        FusedLocationProviderClient locationClient =
                LocationServices.getFusedLocationProviderClient(
                        getApplicationContext()
                );

        try {

            // Get current phone location
            Location location = Tasks.await(
                    locationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            null
                    ),
                    15,
                    TimeUnit.SECONDS
            );

            if (location == null) {
                return Result.retry();
            }

            double lat = location.getLatitude();
            double lon = location.getLongitude();

            // Fetch weather for current GPS location
            AtomicReference<WeatherData> holder =
                    new AtomicReference<>();

            CountDownLatch latch =
                    new CountDownLatch(1);

            WeatherService.fetch(
                    lat,
                    lon,
                    new WeatherService.Callback() {

                        @Override
                        public void onResult(WeatherData data) {
                            holder.set(data);
                            latch.countDown();
                        }

                        @Override
                        public void onError(Exception e) {
                            latch.countDown();
                        }
                    }
            );

            latch.await(15, TimeUnit.SECONDS);

            WeatherData data = holder.get();

            if (data == null) {
                return Result.retry();
            }

            WalkAdvisor.Advice advice =
                    WalkAdvisor.evaluate(data);

            if (advice.goodRightNow
                    && !data.currentTime.equals(lastNotifiedSlot)) {

                lastNotifiedSlot = data.currentTime;

                NotificationHelper.show(
                        getApplicationContext(),
                        "It's cloudy and "
                                + Math.round(data.currentTemp)
                                + "°C right now — good time for a short walk!"
                );
            }

            return Result.success();

        } catch (Exception e) {

            return Result.retry();
        }
    }
}