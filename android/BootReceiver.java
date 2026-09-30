package com.kizari.dailynotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    private static final String PREFS = "daily_ai_prefs";
    private static final String KEY_ENABLED = "enabled";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) {
            return;
        }

        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        Context appContext =
                context.getApplicationContext();

        SharedPreferences prefs =
                appContext.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        if (prefs.getBoolean(KEY_ENABLED, false)) {
            DailyMessageReceiver.scheduleNext(appContext);
        }
    }
}
