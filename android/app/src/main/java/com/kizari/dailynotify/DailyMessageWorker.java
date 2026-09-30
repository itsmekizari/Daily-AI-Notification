package com.kizari.dailynotify;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.util.UUID;

public class DailyMessageWorker extends Worker {

    private static final String TAG = "DailyAINotify";
    private static final String PREFS = "daily_ai_prefs";
    private static final String FIXED_STYLE = "Love Teasing";

    public DailyMessageWorker(
            @NonNull Context appContext,
            @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);

        if (!prefs.getBoolean("enabled", false)) {
            Log.i(TAG, "Worker skipped because notification is disabled");
            return Result.success();
        }

        String language = prefs.getString("language", "Burmese + English");
        String deviceId = prefs.getString("device_id", null);

        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString("device_id", deviceId).apply();
        }

        try {
            Log.i(TAG, "Calling AI API. style=" + FIXED_STYLE + ", language=" + language);

            JSONObject response = ApiClient.generateMessage(deviceId, language);
            if (!response.optBoolean("success", false)) {
                throw new IllegalStateException("API success=false: " + response);
            }

            JSONObject data = response.optJSONObject("data");
            if (data == null) {
                throw new IllegalStateException("API response has no data");
            }

            String myText = data.optString("my", "").trim();
            String enText = data.optString("en", "").trim();
            String message = formatMessage(myText, enText, language);

            if (message.trim().isEmpty()) {
                throw new IllegalStateException("API returned empty message");
            }

            if (!NotificationHelper.show(context, "😏 Love Teasing", message)) {
                throw new IllegalStateException("Notification could not be posted");
            }

            prefs.edit()
                    .putString("category", FIXED_STYLE)
                    .putString("last_status", "AI success")
                    .putLong("last_success_at", System.currentTimeMillis())
                    .apply();

            Log.i(TAG, "AI notification posted successfully");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "AI request failed", e);

            String[] fallback = FallbackMessages.get();
            String fallbackMessage = formatMessage(
                    fallback[0], fallback[1], language);

            boolean shown = NotificationHelper.show(
                    context,
                    "😏 Love Teasing",
                    fallbackMessage);

            prefs.edit()
                    .putString("category", FIXED_STYLE)
                    .putString("last_status", shown
                            ? "AI unavailable - fallback used"
                            : "AI unavailable - notification blocked")
                    .putLong("last_attempt_at", System.currentTimeMillis())
                    .apply();

            return shown ? Result.success() : Result.retry();
        }
    }

    private String formatMessage(String myText, String enText, String language) {
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
}
