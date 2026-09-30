package com.kizari.dailynotify;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.util.UUID;

public class DailyMessageWorker extends Worker {

    private static final String PREFS = "daily_ai_prefs";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_DEVICE_ID = "device_id";

    public DailyMessageWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (!prefs.getBoolean("enabled", false)) {
            return Result.success();
        }

        String category = prefs.getString(KEY_CATEGORY, "Random");
        String language = prefs.getString(KEY_LANGUAGE, "Burmese + English");
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);

        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        }

        try {
            JSONObject response = ApiClient.generateMessage(
                    deviceId,
                    category,
                    language
            );

            if (!response.optBoolean("success", false)) {
                return Result.retry();
            }

            JSONObject data = response.optJSONObject("data");
            if (data == null) {
                return Result.retry();
            }

            String myText = data.optString("my", "").trim();
            String enText = data.optString("en", "").trim();

            String message;
            if ("Burmese only".equals(language)) {
                message = myText;
            } else if ("English only".equals(language)) {
                message = enText;
            } else {
                message = myText;
                if (!enText.isEmpty()) {
                    if (!message.isEmpty()) message += "\n\n";
                    message += enText;
                }
            }

            if (message.trim().isEmpty()) {
                return Result.retry();
            }

            NotificationHelper.show(
                    context,
                    category,
                    message
            );

            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
