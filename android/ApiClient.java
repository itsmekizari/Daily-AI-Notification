package com.kizari.dailynotify;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    public static String API_URL = "https://YOUR-DOMAIN.example/daily_ai/api.php";

    public static JSONObject generateMessage(String deviceId, String category) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        JSONObject body = new JSONObject();
        body.put("action", "generate");
        body.put("device_id", deviceId);
        body.put("category", category);

        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300
            ? c.getInputStream() : c.getErrorStream();

        String response;
        try (BufferedReader r = new BufferedReader(
            new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder s = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) s.append(line);
            response = s.toString();
        }

        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code);
        }
        return new JSONObject(response);
    }
}
