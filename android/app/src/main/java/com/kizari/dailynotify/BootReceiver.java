package com.kizari.dailynotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    private static final String PREFS = "daily_ai_prefs";

    private static final String ACTION_TIME_SET =
            "android.intent.action.TIME_SET";
    private static final String ACTION_TIMEZONE_CHANGED =
            "android.intent.action.TIMEZONE_CHANGED";
    private static final String ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        boolean supported =
                Intent.ACTION_BOOT_COMPLETED.equals(action)
                        || ACTION_TIME_SET.equals(action)
                        || ACTION_TIMEZONE_CHANGED.equals(action)
                        || ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action);

        if (!supported) return;

        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );

        if (prefs.getBoolean("enabled", false)) {
            DailyMessageReceiver.ensureNextMessage(appContext);
            DailyMessageReceiver.scheduleNext(appContext);
        }
    }
}
