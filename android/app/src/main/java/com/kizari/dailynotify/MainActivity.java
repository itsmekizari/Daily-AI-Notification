package com.kizari.dailynotify;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String PREFS = "daily_ai_prefs";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 5001;
    private static final String FIXED_STYLE = "Love Teasing";
    private static final String[] LANGUAGES = {
            "Burmese + English", "Burmese only", "English only"
    };

    private SharedPreferences prefs;
    private Spinner languageSpinner;
    private Switch enabledSwitch;
    private TextView timeText;
    private TextView statusText;
    private int selectedHour = 8;
    private int selectedMinute = 0;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ensureDeviceId();
        loadPrefs();
        buildUi();
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs != null) {
            if (prefs.getBoolean("enabled", false)) {
                DailyMessageReceiver.scheduleNext(this);
            }
            updateStatus();
        }
    }

    private void ensureDeviceId() {
        String id = prefs.getString("device_id", null);
        if (id == null || id.trim().isEmpty()) {
            prefs.edit().putString("device_id", UUID.randomUUID().toString()).apply();
        }
    }

    private void loadPrefs() {
        selectedHour = prefs.getInt("hour", 8);
        selectedMinute = prefs.getInt("minute", 0);
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(22), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText("🤖 Daily AI Notification");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("နေ့တိုင်း AI က အချစ်အကြောင်းကို စနှောက်၊ လှောင်ပြောင်ပြီး playful social-media vibe နဲ့ message အသစ်တစ်ခု generate လုပ်ပေးမယ်");
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, marginParams(0, 6, 0, 22));

        root.addView(label("AI Style"), marginParams(0, 0, 0, 4));
        TextView fixedStyle = new TextView(this);
        fixedStyle.setText("😏 " + FIXED_STYLE + "\nတစ်မျိုးတည်းပဲ • AI auto-generated");
        fixedStyle.setTextSize(18);
        fixedStyle.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(fixedStyle, matchWrap());

        root.addView(label("Language"), marginParams(0, 18, 0, 4));
        languageSpinner = new Spinner(this);
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, LANGUAGES);
        languageAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);
        selectSpinner(languageSpinner,
                prefs.getString("language", "Burmese + English"));
        root.addView(languageSpinner, matchWrap());

        root.addView(label("Daily time"), marginParams(0, 18, 0, 4));
        timeText = new TextView(this);
        timeText.setTextSize(24);
        updateTimeText();
        root.addView(timeText, marginParams(0, 0, 0, 8));

        Button setTime = button("SET TIME");
        setTime.setOnClickListener(v -> showTimePicker());
        root.addView(setTime, matchWrap());

        enabledSwitch = new Switch(this);
        enabledSwitch.setText("Enable daily notification");
        enabledSwitch.setTextSize(17);
        enabledSwitch.setChecked(prefs.getBoolean("enabled", false));
        root.addView(enabledSwitch, marginParams(0, 18, 0, 10));

        Button save = button("SAVE & SCHEDULE");
        save.setOnClickListener(v -> saveAndSchedule());
        root.addView(save, matchWrap());

        Button generate = button("GENERATE AI NOW");
        generate.setOnClickListener(v -> generateNow());
        root.addView(generate, marginParams(0, 8, 0, 0));

        Button test = button("TEST NOTIFICATION");
        test.setOnClickListener(v -> {
            boolean shown = NotificationHelper.show(
                    this,
                    "😏 Love Teasing",
                    "စမ်းသပ် notification အောင်မြင်ပါတယ်!\n\nTest notification works!");
            if (!shown) {
                Toast.makeText(this,
                        "Notification permission is blocked.",
                        Toast.LENGTH_LONG).show();
            }
        });
        root.addView(test, marginParams(0, 8, 0, 0));

        Button exactAlarm = button("OPEN EXACT ALARM SETTINGS");
        exactAlarm.setOnClickListener(v -> openExactAlarmSettings());
        root.addView(exactAlarm, marginParams(0, 8, 0, 0));

        Button notification = button("OPEN NOTIFICATION SETTINGS");
        notification.setOnClickListener(v -> openNotificationSettings());
        root.addView(notification, marginParams(0, 8, 0, 0));

        statusText = new TextView(this);
        statusText.setTextSize(15);
        root.addView(statusText, marginParams(0, 18, 0, 0));
        updateStatus();

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void generateNow() {
        String language = String.valueOf(languageSpinner.getSelectedItem());
        boolean enabled = enabledSwitch.isChecked();

        prefs.edit()
                .putString("category", FIXED_STYLE)
                .putString("language", language)
                .putBoolean("enabled", enabled)
                .apply();

        Toast.makeText(this, "AI is generating a new message...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                String deviceId = prefs.getString("device_id", UUID.randomUUID().toString());
                JSONObject response = ApiClient.generateMessage(deviceId, language);
                JSONObject data = response.optJSONObject("data");

                if (!response.optBoolean("success", false) || data == null) {
                    throw new IllegalStateException("API returned an unsuccessful response");
                }

                String my = data.optString("my", "").trim();
                String en = data.optString("en", "").trim();
                String message = formatMessage(my, en, language);

                prefs.edit()
                        .putString("category", FIXED_STYLE)
                        .putString("last_status", "AI success")
                        .putLong("last_success_at", System.currentTimeMillis())
                        .apply();

                runOnUiThread(() -> {
                    boolean shown = NotificationHelper.show(this, "😏 Love Teasing", message);
                    Toast.makeText(this,
                            shown ? "AI generated ✅" : "AI generated, but notification is blocked",
                            Toast.LENGTH_LONG).show();
                    updateStatus();
                });
            } catch (Exception e) {
                prefs.edit().putString("last_status", "AI error: " + safeError(e)).apply();
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "AI error: " + safeError(e),
                            Toast.LENGTH_LONG).show();
                    updateStatus();
                });
            }
        });
    }

    private String safeError(Exception e) {
        String text = e.getMessage();
        if (text == null || text.trim().isEmpty()) return e.getClass().getSimpleName();
        if (text.length() > 160) return text.substring(0, 160);
        return text;
    }

    private String formatMessage(String my, String en, String language) {
        if ("Burmese only".equals(language)) return my;
        if ("English only".equals(language)) return en;
        if (my.isEmpty()) return en;
        if (en.isEmpty()) return my;
        return my + "\n\n" + en;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(18);
        return view;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    updateTimeText();
                },
                selectedHour,
                selectedMinute,
                false);
        dialog.show();
    }

    private void updateTimeText() {
        if (timeText == null) return;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
        calendar.set(Calendar.MINUTE, selectedMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        String formatted = DateFormat.getTimeInstance(
                DateFormat.SHORT, Locale.getDefault()).format(calendar.getTime());
        timeText.setText(formatted);
    }

    private void saveAndSchedule() {
        String language = String.valueOf(languageSpinner.getSelectedItem());
        boolean enabled = enabledSwitch.isChecked();

        prefs.edit()
                .putString("category", FIXED_STYLE)
                .putString("language", language)
                .putInt("hour", selectedHour)
                .putInt("minute", selectedMinute)
                .putBoolean("enabled", enabled)
                .apply();

        if (enabled) {
            DailyMessageReceiver.scheduleNext(this);
            Toast.makeText(this, "Daily AI notification scheduled ✅", Toast.LENGTH_SHORT).show();
        } else {
            DailyMessageReceiver.cancel(this);
            Toast.makeText(this, "Daily notification disabled", Toast.LENGTH_SHORT).show();
        }
        updateStatus();
    }

    private void updateStatus() {
        if (statusText == null) return;

        boolean enabled = prefs.getBoolean("enabled", false);
        String language = prefs.getString("language", "Burmese + English");
        String lastStatus = prefs.getString("last_status", "Not run yet");

        StringBuilder text = new StringBuilder();
        text.append("Status: ").append(enabled ? "ON ✅" : "OFF ❌");
        text.append("\nAI Style: ").append(FIXED_STYLE);
        text.append("\nLanguage: ").append(language);
        text.append("\nNext scheduled time: ").append(nextTimeText());
        text.append("\nLast AI status: ").append(lastStatus);
        statusText.setText(text.toString());
    }

    private String nextTimeText() {
        if (!prefs.getBoolean("enabled", false)) return "Not scheduled";

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
        calendar.set(Calendar.MINUTE, selectedMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                .format(new Date(calendar.getTimeInMillis()));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                Intent intent = new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception ignored) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        } else {
            Toast.makeText(this,
                    "Exact alarm access is not required on this Android version.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }

    private void selectSpinner(Spinner spinner, String value) {
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (value.equals(adapter.getItem(i))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams marginParams(
            int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
