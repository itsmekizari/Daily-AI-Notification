package com.kizari.dailynotify;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;
import java.util.*;

public class DailyMessageReceiver extends BroadcastReceiver {
    private static final int REQUEST_CODE = 7412;

    @Override
    public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences p =
            context.getSharedPreferences("daily_ai_prefs", Context.MODE_PRIVATE);

        if (!p.getBoolean("enabled", false)) return;

        final String category = p.getString("category", "Random");
        final String deviceId = p.getString("device_id", "unknown");

        new Thread(() -> {
            String my;
            String en;
            try {
                JSONObject result = ApiClient.generateMessage(deviceId, category);
                JSONObject data = result.getJSONObject("data");
                my = data.getString("my");
                en = data.getString("en");
            } catch (Exception e) {
                String[] fallback = FallbackMessages.next(context, category);
                my = fallback[0];
                en = fallback[1];
            }

            NotificationHelper.show(context, "🤭 " + category, my + "\n\n" + en);

            int hour = p.getInt("hour", 8);
            int minute = p.getInt("minute", 0);
            scheduleNext(context, hour, minute);
        }).start();
    }

    public static void scheduleNext(Context context, int hour, int minute) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, DailyMessageReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(Calendar.getInstance())) next.add(Calendar.DAY_OF_YEAR, 1);

        if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi
            );
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
        }
    }

    public static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, DailyMessageReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        am.cancel(pi);
    }
}
