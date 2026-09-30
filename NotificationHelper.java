package com.kizari.dailynotify;

import android.app.*;
import android.content.*;
import android.os.*;

public class NotificationHelper {
    private static final String CHANNEL_ID = "daily_ai_messages";

    public static void show(Context context, String title, String body) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager nm =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Daily AI Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            nm.createNotificationChannel(ch);
        }

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(context, CHANNEL_ID)
            : new Notification.Builder(context);

        b.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true);

        nm.notify((int)(System.currentTimeMillis() & 0x7fffffff), b.build());
    }
}
