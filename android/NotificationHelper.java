package com.kizari.dailynotify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public class NotificationHelper {

    private static final String CHANNEL_ID = "daily_ai_messages";
    private static final String CHANNEL_NAME = "Daily AI Messages";

    public static void show(
            Context context,
            String title,
            String body
    ) {
        if (context == null) {
            return;
        }

        Context appContext = context.getApplicationContext();

        // Android 13+ notification permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (appContext.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) appContext.getSystemService(
                        Context.NOTIFICATION_SERVICE
                );

        if (notificationManager == null) {
            return;
        }

        // Check whether notifications are disabled in system settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!notificationManager.areNotificationsEnabled()) {
                return;
            }
        }

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_DEFAULT
                    );

            channel.setDescription(
                    "Daily AI-generated Burmese and English messages"
            );

            notificationManager.createNotificationChannel(channel);
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(
                    appContext,
                    CHANNEL_ID
            );
        } else {
            builder = new Notification.Builder(appContext);
        }

        builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(body)
                )
                .setAutoCancel(true);

        int notificationId =
                (int) (System.currentTimeMillis() & 0x7fffffff);

        notificationManager.notify(
                notificationId,
                builder.build()
        );
    }
}
