package com.example.fitman.sync;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StepWorker extends Worker {

    public StepWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        //result of background function can be - success , retry, or failure
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return Result.success();

        SensorManager sm = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor == null) return Result.failure();

        final long[] value = {-1};
        final CountDownLatch latch = new CountDownLatch(1);
        SensorEventListener listener = new SensorEventListener() {
            @Override public void onSensorChanged(SensorEvent e) {
                value[0] = (long) e.values[0];
                latch.countDown();
            }
            @Override public void onAccuracyChanged(Sensor s, int a) { }
        };

        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        sm.unregisterListener(listener);

        if (value[0] < 0) return Result.retry();
        try {
            StepSync.SyncResult r = Tasks.await(StepSync.sync(value[0]));
            Tasks.await(ChallengeSync.pushProgress(r.steps));
            ChallengeSync.deliverPending(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}