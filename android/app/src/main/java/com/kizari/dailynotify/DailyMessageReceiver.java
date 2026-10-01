package com.kizari.dailynotify;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Random;

public class DailyMessageReceiver extends BroadcastReceiver {

    private static final String PREFS = "daily_ai_prefs";
    private static final String ALARM_ACTION = "com.kizari.dailynotify.DAILY_ALARM";
    private static final int ALARM_REQUEST_CODE = 1001;

    private static final String KEY_NEXT_SOURCE = "next_source";
    private static final String KEY_NEXT_INDEX = "next_index";
    private static final String SOURCE_CUSTOM = "custom";
    private static final String SOURCE_LOCAL = "local";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;

        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );

        if (!prefs.getBoolean("enabled", false)) return;

        String language = prefs.getString(
                "language",
                "Burmese + English"
        );

        String message = takePreparedMessage(app, language);
        if (message == null || message.trim().isEmpty()) {
            message = randomMessage(app, language, new SecureRandom());
        }

        if (message != null && !message.trim().isEmpty()) {
            NotificationHelper.show(
                    app,
                    "Daily AI Notification",
                    message
            );
        }

        // As soon as the displayed message is consumed, select and save the
        // following message. There is no manual generation step and no AI call.
        ensureNextMessage(app);
        scheduleNext(app);
    }

    public static void showTestNotification(Context context) {
        if (context == null) return;

        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );
        String language = prefs.getString(
                "language",
                "Burmese + English"
        );

        String message = randomMessage(
                app,
                language,
                new SecureRandom()
        );

        if (message == null || message.trim().isEmpty()) {
            message = "Test notification is working. ✅";
        }

        NotificationHelper.show(
                app,
                "Test Notification",
                message
        );
    }

    public static void clearPreparedMessage(Context context) {
        if (context == null) return;
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_NEXT_SOURCE)
                .remove(KEY_NEXT_INDEX)
                .apply();
    }

    public static void ensureNextMessage(Context context) {
        if (context == null) return;

        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );

        if (!prefs.getBoolean("enabled", false)) return;

        String source = prefs.getString(KEY_NEXT_SOURCE, "");
        String rawIndex = prefs.getString(KEY_NEXT_INDEX, "");

        if (SOURCE_CUSTOM.equals(source)) {
            if (isValidIndex(rawIndex, CustomMessageStore.size(app))) return;
        } else if (SOURCE_LOCAL.equals(source)) {
            if (isValidIndex(rawIndex, LocalMessageBank.SIZE)) return;
        }

        Random random = new SecureRandom();

        boolean hasCustom = CustomMessageStore.size(app) > 0;
        boolean chooseCustom = hasCustom && random.nextInt(100) < 50;

        String sourceToStore = chooseCustom ? SOURCE_CUSTOM : SOURCE_LOCAL;
        int size = chooseCustom
                ? CustomMessageStore.size(app)
                : LocalMessageBank.SIZE;

        if (size <= 0) {
            sourceToStore = SOURCE_LOCAL;
            size = LocalMessageBank.SIZE;
        }

        int index = random.nextInt(size);

        String lastSource = prefs.getString("last_source", "");
        int lastIndex = prefs.getInt("last_index", -1);

        if (sourceToStore.equals(lastSource) && index == lastIndex && size > 1) {
            index = (index + 1 + random.nextInt(size - 1)) % size;
        }

        prefs.edit()
                .putString(KEY_NEXT_SOURCE, sourceToStore)
                .putString(KEY_NEXT_INDEX, String.valueOf(index))
                .apply();
    }

    private static String takePreparedMessage(
            Context context,
            String language
    ) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );

        String source = prefs.getString(KEY_NEXT_SOURCE, "");
        String rawIndex = prefs.getString(KEY_NEXT_INDEX, "");

        int size;
        if (SOURCE_CUSTOM.equals(source)) {
            size = CustomMessageStore.size(context);
        } else {
            size = LocalMessageBank.SIZE;
        }

        if (!isValidIndex(rawIndex, size)) return null;

        try {
            int index = Integer.parseInt(rawIndex.trim());
            String message;

            if (SOURCE_CUSTOM.equals(source)) {
                // Custom entries are randomly selected from their own saved list.
                // The exact index is stored by position, so rebuild the selected
                // entry deterministically by using the list index.
                message = getCustomByIndex(context, index, language);
            } else {
                message = LocalMessageBank.format(index, language);
            }

            prefs.edit()
                    .remove(KEY_NEXT_SOURCE)
                    .remove(KEY_NEXT_INDEX)
                    .putString("last_source", source)
                    .putInt("last_index", index)
                    .apply();

            return message;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String getCustomByIndex(
            Context context,
            int wantedIndex,
            String language
    ) {
        String raw = CustomMessageStore.loadRaw(context);
        String[] lines = raw.split("\\r?\\n");
        int current = 0;

        for (String line : lines) {
            String value = line.trim();
            if (value.isEmpty()) continue;

            String my = value;
            String en = value;
            int separator = value.indexOf("||");
            if (separator >= 0) {
                my = value.substring(0, separator).trim();
                en = value.substring(separator + 2).trim();
                if (my.isEmpty()) my = en;
                if (en.isEmpty()) en = my;
            }

            if (current == wantedIndex) {
                if ("Burmese only".equals(language)) return my;
                if ("English only".equals(language)) return en;
                return my + "\n\n" + en;
            }
            current++;
        }

        return null;
    }

    private static String randomMessage(
            Context context,
            String language,
            Random random
    ) {
        int customSize = CustomMessageStore.size(context);
        if (customSize > 0 && random.nextInt(100) < 50) {
            String custom = CustomMessageStore.random(
                    context,
                    language,
                    random
            );
            if (custom != null && !custom.trim().isEmpty()) {
                return custom;
            }
        }

        return LocalMessageBank.format(
                random.nextInt(LocalMessageBank.SIZE),
                language
        );
    }

    private static boolean isValidIndex(String value, int size) {
        if (value == null || value.trim().isEmpty() || size <= 0) return false;
        try {
            int index = Integer.parseInt(value.trim());
            return index >= 0 && index < size;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static void scheduleNext(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );

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

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = getPendingIntent(context);
        long triggerAt = calendar.getTimeInMillis();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent
                    );
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent
                    );
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                );
            }
        } catch (SecurityException ignored) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
            );
        }
    }

    private static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, DailyMessageReceiver.class);
        intent.setAction(ALARM_ACTION);
        return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static void cancel(Context context) {
        if (context == null) return;

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(getPendingIntent(context));
        }
    }
}
