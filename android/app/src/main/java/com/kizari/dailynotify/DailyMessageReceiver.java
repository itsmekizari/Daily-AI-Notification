package com.kizari.dailynotify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class DailyMessageReceiver extends BroadcastReceiver {

    private static final String TAG = "DailyAINotify";
    private static final String PREFS = "daily_ai_prefs";
    private static final String WORK_NAME = "daily_ai_prefetch_work";
    private static final String ALARM_ACTION =
            "com.kizari.dailynotify.DAILY_ALARM";
    private static final String FIXED_STYLE = "Love Teasing";
    private static final int ALARM_REQUEST_CODE = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;

        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);

        if (!prefs.getBoolean("enabled", false)) {
            Log.i(TAG, "Alarm received but app is disabled");
            return;
        }

        Log.i(TAG, "Daily alarm received");

        // Today's notification uses the message that was generated earlier.
        showCachedMessage(appContext, prefs);

        // Schedule the next day's alarm first, then quietly pre-generate the
        // NEXT message immediately after today's notification.
        scheduleNext(appContext);
        enqueuePrefetch(appContext);
    }

    private static void showCachedMessage(
            Context context,
            SharedPreferences prefs) {

        String language = prefs.getString(
                "language", "Burmese + English");
        String cachedLanguage = prefs.getString("next_language", language);
        String myText = prefs.getString("next_my", "").trim();
        String enText = prefs.getString("next_en", "").trim();

        String message = formatMessage(myText, enText, cachedLanguage);

        if (message.isEmpty()) {
            // First-run or a rare generation failure. Show a safe fallback now,
            // then the worker will generate the next real AI message.
            String[] fallback = FallbackMessages.get(FIXED_STYLE);
            message = formatMessage(fallback[0], fallback[1], language);
            prefs.edit()
                    .putString("last_status", "No cached AI message - fallback shown")
                    .apply();
        } else {
            prefs.edit()
                    .putString("last_status", "Cached AI message notified")
                    .apply();
        }

        NotificationHelper.show(
                context,
                "😏 Love Teasing",
                message);

        // Consume the cached message so a failed next generation can never
        // accidentally repeat yesterday's message.
        prefs.edit()
                .remove("next_my")
                .remove("next_en")
                .remove("next_language")
                .remove("next_generated_at")
                .apply();
    }

    private static String formatMessage(
            String myText,
            String enText,
            String language) {

        if (myText == null) myText = "";
        if (enText == null) enText = "";
        if (language == null) language = "Burmese + English";

        if ("Burmese only".equals(language)) {
            return myText.trim();
        }
        if ("English only".equals(language)) {
            return enText.trim();
        }
        if (myText.trim().isEmpty()) return enText.trim();
        if (enText.trim().isEmpty()) return myText.trim();
        return myText.trim() + "\n\n" + enText.trim();
    }

    public static void ensurePrefetched(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false)) return;

        String language = prefs.getString(
                "language", "Burmese + English");
        String cachedLanguage = prefs.getString("next_language", "");
        boolean hasMessage = !prefs.getString("next_my", "").trim().isEmpty()
                || !prefs.getString("next_en", "").trim().isEmpty();

        if (!hasMessage || !language.equals(cachedLanguage)) {
            enqueuePrefetch(context);
        }
    }

    public static void enqueuePrefetch(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false)) return;

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest.Builder builder =
                new OneTimeWorkRequest.Builder(DailyMessageWorker.class)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                5,
                                TimeUnit.SECONDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setExpedited(
                    OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST);
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                builder.build());

        Log.i(TAG, "Next-message prefetch enqueued");
    }

    public static void scheduleNext(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);

        if (!prefs.getBoolean("enabled", false)) return;

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

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = getPendingIntent(context);
        long triggerAt = calendar.getTimeInMillis();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent);
                    Log.i(TAG, "Exact alarm scheduled for " + calendar.getTime());
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent);
                    Log.w(TAG, "Exact alarm permission missing; inexact alarm scheduled");
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent);
                Log.i(TAG, "Exact alarm scheduled for " + calendar.getTime());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Exact alarm scheduling failed", e);
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent);
        }
    }

    private static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, DailyMessageReceiver.class);
        intent.setAction(ALARM_ACTION);
        return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void cancel(Context context) {
        if (context == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(
                Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(getPendingIntent(context));
        }

        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
}
