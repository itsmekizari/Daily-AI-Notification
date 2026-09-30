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
    private static final String WORK_NAME = "daily_ai_next_message";
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
            return;
        }

        Log.i(TAG, "Scheduled alarm fired");

        String language = prefs.getString(
                "language", "Burmese + English");
        String cachedLanguage = prefs.getString("next_language", language);
        String myText = prefs.getString("next_my", "").trim();
        String enText = prefs.getString("next_en", "").trim();

        boolean cached = !myText.isEmpty() || !enText.isEmpty();
        String message = formatMessage(myText, enText, cachedLanguage);

        if (message.isEmpty()) {
            String[] fallback = FallbackMessages.get(FIXED_STYLE);
            message = formatMessage(fallback[0], fallback[1], language);
            prefs.edit()
                    .putString("last_status", "Cached AI message missing; fallback shown")
                    .apply();
        } else {
            prefs.edit()
                    .putString("last_status", "AI message notified")
                    .remove("last_error")
                    .apply();
        }

        // The current message is posted exactly once.
        NotificationHelper.show(
                appContext,
                "😏 Love Teasing",
                message);

        // Consume the current cache BEFORE starting generation for the next one.
        prefs.edit()
                .remove("next_my")
                .remove("next_en")
                .remove("next_language")
                .remove("next_generated_at")
                .apply();

        // The notification does not need to be dismissed by the user.
        // As soon as it is posted, the next AI message starts generating quietly.
        scheduleNext(appContext);
        enqueuePrefetch(appContext);

        Log.i(TAG, "Current notification posted (cached=" + cached
                + "); next message generation started automatically");
    }

    private static String formatMessage(
            String myText,
            String enText,
            String language) {

        if (myText == null) myText = "";
        if (enText == null) enText = "";
        if (language == null) language = "Burmese + English";

        myText = myText.trim();
        enText = enText.trim();

        if ("Burmese only".equals(language)) {
            return myText;
        }
        if ("English only".equals(language)) {
            return enText;
        }
        if (myText.isEmpty()) return enText;
        if (enText.isEmpty()) return myText;
        return myText + "\n\n" + enText;
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
                                15,
                                TimeUnit.SECONDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setExpedited(
                    OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST);
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                builder.build());
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
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Exact alarm unavailable; using inexact alarm", e);
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
