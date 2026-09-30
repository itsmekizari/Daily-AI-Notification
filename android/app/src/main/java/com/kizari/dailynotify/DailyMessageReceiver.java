package com.kizari.dailynotify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class DailyMessageReceiver extends BroadcastReceiver {

    private static final String PREFS = "daily_ai_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String WORK_NAME = "daily_ai_message_work";
    private static final int ALARM_REQUEST_CODE = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;

        Context appContext = context.getApplicationContext();
        SharedPreferences prefs =
                appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (!prefs.getBoolean(KEY_ENABLED, false)) {
            return;
        }

        // Schedule the next day immediately. The actual network work is handed
        // to WorkManager so Android does not kill a long-running HTTP request.
        scheduleNext(appContext);
        enqueueMessageWork(appContext);
    }

    private static void enqueueMessageWork(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(DailyMessageWorker.class)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                androidx.work.BackoffPolicy.EXPONENTIAL,
                                30,
                                TimeUnit.SECONDS
                        )
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    public static void scheduleNext(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (!prefs.getBoolean(KEY_ENABLED, false)) return;

        int hour = prefs.getInt("hour", 8);
        int minute = prefs.getInt("minute", 0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = getPendingIntent(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    private static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, DailyMessageReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.cancel(getPendingIntent(context));
        }

        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
}
