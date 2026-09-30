package com.kizari.dailynotify;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.LOCKED_BOOT_COMPLETED".equals(intent.getAction())) {

            android.content.SharedPreferences p =
                context.getSharedPreferences("daily_ai_prefs", Context.MODE_PRIVATE);

            if (p.getBoolean("enabled", false)) {
                DailyMessageReceiver.scheduleNext(
                    context,
                    p.getInt("hour", 8),
                    p.getInt("minute", 0)
                );
            }
        }
    }
}
