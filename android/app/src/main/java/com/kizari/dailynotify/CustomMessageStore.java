package com.kizari.dailynotify;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

/** Stores messages entered from inside the app. */
public final class CustomMessageStore {
    private static final String PREFS = "daily_ai_prefs";
    private static final String KEY_MESSAGES = "custom_messages_json";

    private CustomMessageStore() {}

    public static List<String> getAll(Context context) {
        ArrayList<String> result = new ArrayList<>();
        if (context == null) return result;

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_MESSAGES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "").trim();
                if (!value.isEmpty()) result.add(value);
            }
        } catch (JSONException ignored) {
            // Invalid old data is treated as an empty list.
        }
        return result;
    }

    public static void saveAll(Context context, List<String> messages) {
        if (context == null) return;
        JSONArray array = new JSONArray();
        if (messages != null) {
            for (String message : messages) {
                if (message == null) continue;
                String value = message.trim();
                if (!value.isEmpty()) array.put(value);
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MESSAGES, array.toString())
                .remove("next_index")
                .remove("remaining_order")
                .remove("remaining_cursor")
                .apply();
    }

    public static int getCount(Context context) {
        return getAll(context).size();
    }
}
