package com.kizari.dailynotify;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.util.UUID;

public class DailyMessageWorker extends Worker {

    private static final String TAG = "DailyAINotify";
    private static final String PREFS = "daily_ai_prefs";
    private static final String FIXED_STYLE = "Love Teasing";
    private static final long MAX_GENERATION_MS = 30000L;

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
            return Result.success();
        }

        String language = prefs.getString(
                "language", "Burmese + English");
        String deviceId = prefs.getString("device_id", null);

        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString("device_id", deviceId).apply();
        }

        long startedAt = System.currentTimeMillis();

        try {
            Log.i(TAG, "Generating next AI message automatically");

            JSONObject response = ApiClient.generateMessage(
                    deviceId,
                    FIXED_STYLE,
                    language,
                    MAX_GENERATION_MS);

            if (System.currentTimeMillis() - startedAt > MAX_GENERATION_MS) {
                throw new java.util.concurrent.TimeoutException(
                        "Generation exceeded 30 seconds");
            }

            if (!response.optBoolean("success", false)) {
                throw new IllegalStateException(
                        "API returned success=false: " + response);
            }

            JSONObject data = response.optJSONObject("data");
            if (data == null) {
                throw new IllegalStateException("API response has no data");
            }

            String myText = data.optString("my", "").trim();
            String enText = data.optString("en", "").trim();

            if (myText.isEmpty() && enText.isEmpty()) {
                throw new IllegalStateException("AI returned an empty message");
            }

            // Only replace the cache after a complete successful generation.
            // The worker never posts a notification.
            prefs.edit()
                    .putString("next_my", myText)
                    .putString("next_en", enText)
                    .putString("next_language", language)
                    .putLong("next_generated_at", System.currentTimeMillis())
                    .putString("last_status", "AI pre-generated next message")
                    .remove("last_error")
                    .apply();

            Log.i(TAG, "Next AI message saved successfully");
            return Result.success();

        } catch (Exception e) {
            String error = e.getClass().getSimpleName();
            if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
                error += ": " + e.getMessage().trim();
            }
            if (error.length() > 500) {
                error = error.substring(0, 500);
            }

            Log.e(TAG, "Next AI generation failed: " + error, e);
            prefs.edit()
                    .putString("last_status", "AI pre-generation failed")
                    .putString("last_error", error)
                    .putLong("last_attempt_at", System.currentTimeMillis())
                    .apply();

            return Result.retry();
        }
    }
}
