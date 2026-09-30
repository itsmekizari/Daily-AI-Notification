package com.kizari.dailynotify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DailyMessageReceiver extends BroadcastReceiver {

    private static final String PREFS = "daily_ai_prefs";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MINUTE = "minute";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (context == null) {
            return;
        }

        android.content.SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        boolean enabled =
                prefs.getBoolean(KEY_ENABLED, false);

        if (!enabled) {
            return;
        }

        String category =
                prefs.getString(KEY_CATEGORY, "Random");

        String deviceId =
                prefs.getString(KEY_DEVICE_ID, null);

        if (deviceId == null || deviceId.trim().isEmpty()) {

            deviceId = UUID.randomUUID().toString();

            prefs.edit()
                    .putString(KEY_DEVICE_ID, deviceId)
                    .apply();
        }

        String finalDeviceId = deviceId;
        String finalCategory = category;

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        executor.execute(() -> {

            try {

                JSONObject response =
                        ApiClient.generateMessage(
                                finalDeviceId,
                                finalCategory
                        );

                boolean success =
                        response.optBoolean("success", false);

                if (success) {

                    JSONObject data =
                            response.optJSONObject("data");

                    if (data != null) {

                        String myText =
                                data.optString(
                                        "my",
                                        ""
                                );

                        String enText =
                                data.optString(
                                        "en",
                                        ""
                                );

                        if (!myText.isEmpty()
                                || !enText.isEmpty()) {

                            String notificationText =
                                    myText;

                            if (!enText.isEmpty()) {

                                notificationText =
                                        myText
                                                + "\n\n"
                                                + enText;
                            }

                            NotificationHelper.showNotification(
                                    context,
                                    notificationText
                            );
                        }
                    }
                }

            } catch (Exception e) {

                e.printStackTrace();

                /*
                 * API မရရင် local fallback
                 * notification ပြနိုင်အောင်
                 * ဒီနေရာမှာ fallback ထည့်နိုင်တယ်။
                 */
            }

            scheduleNext(context);
        });

        executor.shutdown();
    }


    public static void scheduleNext(Context context) {

        android.content.SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        boolean enabled =
                prefs.getBoolean(KEY_ENABLED, false);

        if (!enabled) {
            return;
        }

        int hour =
                prefs.getInt(KEY_HOUR, 8);

        int minute =
                prefs.getInt(KEY_MINUTE, 0);

        Calendar calendar =
                Calendar.getInstance();

        calendar.set(
                Calendar.HOUR_OF_DAY,
                hour
        );

        calendar.set(
                Calendar.MINUTE,
                minute
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );

        /*
         * အချိန်ကျော်သွားပြီဆို
         * နောက်နေ့ကိုရွှေ့
         */
        if (calendar.getTimeInMillis()
                <= System.currentTimeMillis()) {

            calendar.add(
                    Calendar.DAY_OF_YEAR,
                    1
            );
        }

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        Intent intent =
                new Intent(
                        context,
                        DailyMessageReceiver.class
                );

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        1001,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        if (alarmManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (alarmManager.canScheduleExactAlarms()) {

                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );

            } else {

                /*
                 * Exact alarm permission မရှိသေးရင်
                 * inexact alarm သုံး
                 */
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


    public static void cancel(Context context) {

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        Intent intent =
                new Intent(
                        context,
                        DailyMessageReceiver.class
                );

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        1001,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        if (alarmManager != null) {

            alarmManager.cancel(
                    pendingIntent
            );
        }
    }
}
``
