package com.kizari.dailynotify;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS = "daily_ai_prefs";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MINUTE = "minute";

    private static final int REQ_POST_NOTIFICATIONS = 2001;

    private Spinner categorySpinner;
    private TimePicker timePicker;
    private Switch enabledSwitch;
    private TextView statusText;

    private static final String[] CATEGORIES = {
            "Random",
            "Teasing",
            "Funny",
            "Cute",
            "Motivational",
            "Good Morning",
            "Good Night",
            "Study Reminder"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildUi();
        loadPrefs();
        requestNotificationPermissionIfNeeded();
    }

    private void buildUi() {
        int padding = dp(20);

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("🤖 Daily AI Notification");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("နေ့တိုင်း AI-generated Burmese + English notification");
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextSize(15);
        subtitle.setPadding(0, 0, 0, dp(22));
        root.addView(subtitle, matchWrap());

        TextView categoryLabel = new TextView(this);
        categoryLabel.setText("Category");
        categoryLabel.setTextSize(17);
        root.addView(categoryLabel, matchWrap());

        categorySpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                CATEGORIES
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        root.addView(categorySpinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        addSpace(root, 16);

        TextView timeLabel = new TextView(this);
        timeLabel.setText("Daily time");
        timeLabel.setTextSize(17);
        root.addView(timeLabel, matchWrap());

        timePicker = new TimePicker(this);
        timePicker.setIs24HourView(android.text.format.DateFormat.is24HourFormat(this));
        root.addView(timePicker, matchWrap());

        enabledSwitch = new Switch(this);
        enabledSwitch.setText("Enable daily notification");
        enabledSwitch.setTextSize(17);
        enabledSwitch.setPadding(0, dp(14), 0, dp(14));
        root.addView(enabledSwitch, matchWrap());

        Button saveButton = new Button(this);
        saveButton.setText("Save & Schedule");
        saveButton.setOnClickListener(v -> saveAndSchedule());
        root.addView(saveButton, matchWrap());

        Button testButton = new Button(this);
        testButton.setText("Test Notification");
        testButton.setOnClickListener(v -> {
            NotificationHelper.show(
                    this,
                    "🤭 Test",
                    "Daily AI Notification is working!\n\nNotification test successful."
            );
        });
        root.addView(testButton, matchWrap());

        Button exactAlarmButton = new Button(this);
        exactAlarmButton.setText("Open Exact Alarm Settings");
        exactAlarmButton.setOnClickListener(v -> openExactAlarmSettings());
        root.addView(exactAlarmButton, matchWrap());

        Button notificationSettingsButton = new Button(this);
        notificationSettingsButton.setText("Open Notification Settings");
        notificationSettingsButton.setOnClickListener(v -> openNotificationSettings());
        root.addView(notificationSettingsButton, matchWrap());

        statusText = new TextView(this);
        statusText.setTextSize(14);
        statusText.setPadding(0, dp(18), 0, 0);
        root.addView(statusText, matchWrap());

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private void loadPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        String savedCategory = prefs.getString(KEY_CATEGORY, "Random");
        int categoryIndex = 0;
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(savedCategory)) {
                categoryIndex = i;
                break;
            }
        }
        categorySpinner.setSelection(categoryIndex);

        int hour = prefs.getInt(KEY_HOUR, 8);
        int minute = prefs.getInt(KEY_MINUTE, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
        } else {
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);
        }

        boolean enabled = prefs.getBoolean(KEY_ENABLED, false);
        enabledSwitch.setChecked(enabled);
        updateStatus(enabled, hour, minute, savedCategory);
    }

    private void saveAndSchedule() {
        int hour;
        int minute;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hour = timePicker.getHour();
            minute = timePicker.getMinute();
        } else {
            hour = timePicker.getCurrentHour();
            minute = timePicker.getCurrentMinute();
        }

        String category = String.valueOf(categorySpinner.getSelectedItem());
        boolean enabled = enabledSwitch.isChecked();

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CATEGORY, category)
                .putBoolean(KEY_ENABLED, enabled)
                .putInt(KEY_HOUR, hour)
                .putInt(KEY_MINUTE, minute)
                .apply();

        if (enabled) {
            DailyMessageReceiver.scheduleNext(this);
            ensureExactAlarmAccess();
            ensureNotificationAccess();
            Toast.makeText(this, "Daily notification enabled ✅", Toast.LENGTH_SHORT).show();
        } else {
            DailyMessageReceiver.cancel(this);
            Toast.makeText(this, "Daily notification disabled", Toast.LENGTH_SHORT).show();
        }

        updateStatus(enabled, hour, minute, category);
    }

    private void updateStatus(boolean enabled, int hour, int minute, String category) {
        if (statusText == null) return;

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        String nextText = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                Locale.getDefault()
        ).format(new Date(next.getTimeInMillis()));

        statusText.setText(
                "Status: " + (enabled ? "ON ✅" : "OFF")
                        + "\nCategory: " + category
                        + "\nNext scheduled time: " + nextText
        );
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_POST_NOTIFICATIONS
            );
        }
    }

    private void ensureNotificationAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded();
        }
    }

    private void ensureExactAlarmAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager =
                    (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                        this,
                        "Exact alarm access is needed for precise daily time.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void openExactAlarmSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent intent = new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
            } else {
                Toast.makeText(this, "Exact alarm setting is not required on this Android version.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not open exact alarm settings.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open notification settings.", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private void addSpace(LinearLayout root, int dp) {
        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(dp)
        ));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
