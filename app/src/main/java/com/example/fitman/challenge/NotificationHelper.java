package com.example.fitman.challenge;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public final class NotificationHelper {
    private static final String CHANNEL_ID = "challenges";
    private static int nextId = 1000;

    private NotificationHelper() {}

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Challenges", NotificationManager.IMPORTANCE_DEFAULT);
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    public static void show(Context context, String message) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;   // no permission, skip silently
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("FitMan")
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(nextId++, builder.build());
    }
}