package com.kizari.dailynotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "DailyAINotify";
    private static final String PREFS = "daily_ai_prefs";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !Intent.ACTION_TIME_CHANGED.equals(action) &&
                !Intent.ACTION_TIMEZONE_CHANGED.equals(action) &&
                !"android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);

        if (prefs.getBoolean("enabled", false)) {
            DailyMessageReceiver.scheduleNext(appContext);
            Log.i(TAG, "Rescheduled after system event: " + action);
        }
    }
}
