package com.kizari.dailynotify;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class ApiClient {

    private static final String API_URL =
            "https://daily-ai-notification-api.onrender.com/daily-message";

    private ApiClient() {
    }

    public static JSONObject generateMessage(
            String deviceId,
            String style,
            String language
    ) throws Exception {
        return generateMessage(deviceId, style, language, 30000L);
    }

    public static JSONObject generateMessage(
            String deviceId,
            String style,
            String language,
            long totalTimeoutMs
    ) throws Exception {
        if (totalTimeoutMs < 1000L) {
            totalTimeoutMs = 1000L;
        }

        HttpURLConnection connection = null;
        long deadline = System.currentTimeMillis() + totalTimeoutMs;

        try {
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout((int) Math.min(8000L, totalTimeoutMs));
            connection.setReadTimeout((int) Math.min(20000L, totalTimeoutMs));
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty(
                    "Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Connection", "close");

            JSONObject body = new JSONObject();
            body.put("device_id", deviceId);
            body.put("category", style);
            body.put("language", language);

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
                output.flush();
            }

            if (System.currentTimeMillis() >= deadline) {
                throw new IOException("AI request exceeded 30 seconds");
            }

            int code = connection.getResponseCode();
            InputStream stream = (code >= 200 && code < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (stream == null) {
                throw new IOException("HTTP " + code + " with empty response");
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                    if (System.currentTimeMillis() >= deadline) {
                        throw new IOException("AI response exceeded 30 seconds");
                    }
                }
            }

            if (System.currentTimeMillis() >= deadline) {
                throw new IOException("AI response exceeded 30 seconds");
            }

            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + ": " + result);
            }

            return new JSONObject(result.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
