package com.kizari.dailynotify;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    private static final String API_URL =
            "https://daily-ai-notification-api.onrender.com/daily-message";

    public static JSONObject generateMessage(
            String deviceId,
            String category
    ) throws Exception {

        URL url = new URL(API_URL);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setDoOutput(true);

        connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        connection.setRequestProperty(
                "Accept",
                "application/json"
        );

        JSONObject requestBody = new JSONObject();

        requestBody.put("device_id", deviceId);
        requestBody.put("category", category);

        try (OutputStream outputStream =
                     connection.getOutputStream()) {

            outputStream.write(
                    requestBody.toString()
                            .getBytes(StandardCharsets.UTF_8)
            );
        }

        int responseCode = connection.getResponseCode();

        InputStream inputStream;

        if (responseCode >= 200 && responseCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        if (inputStream == null) {
            throw new IOException(
                    "Server returned HTTP " + responseCode
            );
        }

        StringBuilder responseBuilder =
                new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     inputStream,
                                     StandardCharsets.UTF_8
                             )
                     )) {

            String line;

            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
        }

        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException(
                    "HTTP " + responseCode +
                    ": " + responseBuilder
            );
        }

        return new JSONObject(
                responseBuilder.toString()
        );
    }
}
