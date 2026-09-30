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
    private static final long GENERATION_DEADLINE_MS = 30000L;

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
            Log.i(TAG, "Prefetch skipped because notification is disabled");
            return Result.success();
        }

        String deviceId = prefs.getString("device_id", null);
        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString("device_id", deviceId).apply();
        }

        String language = prefs.getString(
                "language", "Burmese + English");

        long startedAt = System.currentTimeMillis();
        long remainingMs = Math.max(
                1000L,
                GENERATION_DEADLINE_MS
                        - (System.currentTimeMillis() - startedAt));

        try {
            Log.i(TAG, "Prefetching next AI message. language=" + language);

            JSONObject response = ApiClient.generateMessage(
                    deviceId,
                    FIXED_STYLE,
                    language,
                    remainingMs);

            if (System.currentTimeMillis() - startedAt >= GENERATION_DEADLINE_MS) {
                throw new java.util.concurrent.TimeoutException(
                        "AI generation exceeded 30 seconds");
            }

            if (!response.optBoolean("success", false)) {
                throw new IllegalStateException(
                        "API success=false: " + response);
            }

            JSONObject data = response.optJSONObject("data");
            if (data == null) {
                throw new IllegalStateException("API response has no data");
            }

            String myText = data.optString("my", "").trim();
            String enText = data.optString("en", "").trim();
            if (myText.isEmpty() && enText.isEmpty()) {
                throw new IllegalStateException("API returned empty message");
            }

            // Store the NEXT notification only. Do not show anything here.
            prefs.edit()
                    .putString("next_my", myText)
                    .putString("next_en", enText)
                    .putString("next_language", language)
                    .putLong("next_generated_at", System.currentTimeMillis())
                    .putString("last_status", "AI pre-generated next message")
                    .putLong("last_success_at", System.currentTimeMillis())
                    .apply();

            Log.i(TAG, "Next AI message saved. No notification posted by worker.");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "AI pre-generation failed", e);
            prefs.edit()
                    .putString("last_status", "AI pre-generation failed")
                    .putLong("last_attempt_at", System.currentTimeMillis())
                    .apply();

            // WorkManager may retry when the short-lived API request fails.
            return Result.retry();
        }
    }
}
