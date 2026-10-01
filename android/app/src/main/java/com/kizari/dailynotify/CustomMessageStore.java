package com.kizari.dailynotify;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * User-editable messages stored inside the app. The editor is intentionally
 * hidden behind a long-press gesture in MainActivity.
 *
 * One line = one message.
 * Optional bilingual format:
 * Burmese text || English text
 */
public final class CustomMessageStore {

    private static final String PREFS = "daily_ai_prefs";
    private static final String KEY_MESSAGES = "custom_messages";
    private static final String DELIMITER = "||";

    private CustomMessageStore() {
    }

    public static String loadRaw(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_MESSAGES, "");
    }

    public static void saveRaw(Context context, String raw) {
        if (context == null) return;
        String clean = raw == null ? "" : raw.trim();
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MESSAGES, clean)
                .apply();
    }

    public static int size(Context context) {
        return parse(context).size();
    }

    public static String random(Context context, String language, Random random) {
        List<Entry> entries = parse(context);
        if (entries.isEmpty()) return null;
        Entry entry = entries.get(random.nextInt(entries.size()));
        return entry.format(language);
    }

    private static List<Entry> parse(Context context) {
        List<Entry> entries = new ArrayList<>();
        String raw = loadRaw(context);
        if (raw.isEmpty()) return entries;

        String[] lines = raw.split("\\r?\\n");
        for (String line : lines) {
            String value = line.trim();
            if (value.isEmpty()) continue;

            int separator = value.indexOf(DELIMITER);
            if (separator >= 0) {
                String my = value.substring(0, separator).trim();
                String en = value.substring(separator + DELIMITER.length()).trim();
                if (!my.isEmpty() || !en.isEmpty()) {
                    entries.add(new Entry(my.isEmpty() ? en : my, en.isEmpty() ? my : en));
                }
            } else {
                entries.add(new Entry(value, value));
            }
        }
        return entries;
    }

    private static final class Entry {
        final String my;
        final String en;

        Entry(String my, String en) {
            this.my = my;
            this.en = en;
        }

        String format(String language) {
            if ("Burmese only".equals(language)) return my;
            if ("English only".equals(language)) return en;
            return my + "\n\n" + en;
        }
    }
}
