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
    private static final int DAILY_NOTIFICATION_ID = 2001;
    private static final int TEST_NOTIFICATION_ID = 2002;

    private NotificationHelper() {
    }

    public static boolean show(Context context, String title, String body) {
        if (context == null || body == null || body.trim().isEmpty()) {
            return false;
        }

        Context appContext = context.getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                appContext.checkSelfPermission(
                        android.Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        NotificationManager manager =
                (NotificationManager) appContext.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                !manager.areNotificationsEnabled()) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(
                    "Daily AI-generated Burmese and English messages");
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(appContext, CHANNEL_ID)
                : new Notification.Builder(appContext);

        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title == null || title.trim().isEmpty()
                        ? "Daily AI Notification" : title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true);

        manager.notify(DAILY_NOTIFICATION_ID, builder.build());
        return true;
    }
    public static boolean showGenerating(Context context) {
        return showWithId(
                context,
                "🤖 Daily AI",
                "AI က ဒီနေ့အတွက် Love Teasing message ကို generate လုပ်နေပါတယ်...\n\n30 seconds အတွင်း စောင့်ပေးပါ 💬",
                DAILY_NOTIFICATION_ID);
    }

    public static boolean showTest(Context context, String title, String body) {
        return showWithId(context, title, body, TEST_NOTIFICATION_ID);
    }

    private static boolean showWithId(Context context, String title, String body, int id) {
        if (context == null || body == null || body.trim().isEmpty()) return false;
        Context appContext = context.getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                appContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        NotificationManager manager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !manager.areNotificationsEnabled()) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Daily AI-generated Burmese and English messages");
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(appContext, CHANNEL_ID)
                : new Notification.Builder(appContext);

        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title == null || title.trim().isEmpty()
                        ? "Daily AI Notification" : title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true);

        manager.notify(id, builder.build());
        return true;
    }

}
