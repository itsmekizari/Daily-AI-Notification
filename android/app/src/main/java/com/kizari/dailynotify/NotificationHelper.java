package com.kizari.dailynotify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public final class NotificationHelper {

    private static final String CHANNEL_ID = "daily_ai_messages";
    private static final String CHANNEL_NAME = "Daily AI Messages";

    private NotificationHelper() {
    }

    public static boolean show(Context context, String title, String body) {
        if (context == null) return false;

        Context appContext = context.getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && appContext.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        NotificationManager nm =
                (NotificationManager) appContext.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        if (nm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !nm.areNotificationsEnabled()) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(
                    "Daily AI-generated Love Teasing notifications");
            nm.createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(appContext, CHANNEL_ID)
                : new Notification.Builder(appContext);

        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title == null ? "Daily AI Notification" : title)
                .setContentText(body == null ? "" : body)
                .setStyle(new Notification.BigTextStyle().bigText(body == null ? "" : body))
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_DEFAULT);

        int id = (int) (System.currentTimeMillis() & 0x7fffffff);
        nm.notify(id, builder.build());
        return true;
    }

    public static boolean showTest(Context context, String title, String body) {
        return show(context, title, body);
    }
}
