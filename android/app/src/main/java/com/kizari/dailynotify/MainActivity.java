package com.kizari.dailynotify;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final String PREFS = "daily_ai_prefs";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 5001;

    private static final String[] CATEGORIES = {
            "Random", "Teasing", "Funny", "Cute", "Motivational",
            "Good Morning", "Good Night", "Study Reminder"
    };

    private static final String[] LANGUAGES = {
            "Burmese + English", "Burmese only", "English only"
    };

    private SharedPreferences prefs;
    private Spinner categorySpinner;
    private Spinner languageSpinner;
    private Switch enabledSwitch;
    private TextView timeText;
    private TextView statusText;
    private int selectedHour = 8;
    private int selectedMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ensureDeviceId();
        loadPrefs();
        buildUi();
        requestNotificationPermissionIfNeeded();
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(22), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText("🤖 Daily AI Notification");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("နေ့စဉ် AI-generated notification");
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, marginParams(0, 6, 0, 22));

        TextView categoryLabel = label("Category");
        root.addView(categoryLabel, marginParams(0, 0, 0, 4));

        categorySpinner = new Spinner(this);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CATEGORIES);
        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);
        selectSpinner(categorySpinner, prefs.getString("category", "Random"));
        root.addView(categorySpinner, matchWrap());

        TextView languageLabel = label("Language");
        root.addView(languageLabel, marginParams(0, 18, 0, 4));

        languageSpinner = new Spinner(this);
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, LANGUAGES);
        languageAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);
        selectSpinner(languageSpinner,
                prefs.getString("language", "Burmese + English"));
        root.addView(languageSpinner, matchWrap());

        TextView timeLabel = label("Daily time");
        root.addView(timeLabel, marginParams(0, 18, 0, 4));

        timeText = new TextView(this);
        timeText.setTextSize(24);
        updateTimeText();
        root.addView(timeText, marginParams(0, 0, 0, 8));

        Button setTime = new Button(this);
        setTime.setText("SET TIME");
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

        Button test = button("TEST NOTIFICATION");
        test.setOnClickListener(v -> NotificationHelper.show(
                this,
                "🤭 Test",
                "စမ်းသပ် notification အောင်မြင်ပါတယ်!\n\nTest notification works!"));
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

        setContentView(root);
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
                false
        );
        dialog.show();
    }

    private void updateTimeText() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
        calendar.set(Calendar.MINUTE, selectedMinute);
        String formatted = DateFormat.getTimeInstance(
                DateFormat.SHORT, Locale.getDefault()).format(calendar.getTime());
        if (timeText != null) timeText.setText(formatted);
    }

    private void saveAndSchedule() {
        String category = String.valueOf(categorySpinner.getSelectedItem());
        String language = String.valueOf(languageSpinner.getSelectedItem());
        boolean enabled = enabledSwitch.isChecked();

        prefs.edit()
                .putString("category", category)
                .putString("language", language)
                .putInt("hour", selectedHour)
                .putInt("minute", selectedMinute)
                .putBoolean("enabled", enabled)
                .apply();

        if (enabled) {
            DailyMessageReceiver.scheduleNext(this);
        } else {
            DailyMessageReceiver.cancel(this);
        }

        updateStatus();
    }

    private void updateStatus() {
        if (statusText == null) return;

        boolean enabled = prefs.getBoolean("enabled", false);
        String category = prefs.getString("category", "Random");
        String language = prefs.getString("language", "Burmese + English");

        StringBuilder text = new StringBuilder();
        text.append("Status: ").append(enabled ? "ON ✅" : "OFF ❌");
        text.append("\nCategory: ").append(category);
        text.append("\nLanguage: ").append(language);
        text.append("\nNext scheduled time: ").append(nextTimeText());
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
