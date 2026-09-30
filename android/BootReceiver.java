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

        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {

            Context appContext =
                    context.getApplicationContext();

            SharedPreferences prefs =
                    appContext.getSharedPreferences(
                            PREFS,
                            Context.MODE_PRIVATE
                    );

            boolean enabled =
                    prefs.getBoolean(KEY_ENABLED, false);

            if (enabled) {
                DailyMessageReceiver.scheduleNext(
                        appContext
                );
            }
        }
    }
}
