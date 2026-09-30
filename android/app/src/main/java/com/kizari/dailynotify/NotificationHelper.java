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

    public static void show(Context context, String title, String body) {
        if (context == null) return;

        Context appContext = context.getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                appContext.checkSelfPermission(
                        android.Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) appContext.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                !manager.areNotificationsEnabled()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(
                    "Daily AI-generated Burmese and English messages");
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(appContext, CHANNEL_ID)
                : new Notification.Builder(appContext);

        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true);

        int id = (int) (System.currentTimeMillis() & 0x7fffffff);
        manager.notify(id, builder.build());
    }
}
