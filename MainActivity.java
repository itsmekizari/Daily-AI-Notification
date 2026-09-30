package com.kizari.dailynotify;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREFS = "daily_ai_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_CATEGORY = "category";

    private Spinner categorySpinner;
    private TimePicker timePicker;
    private Switch enableSwitch;

    private final String[] categories = {
        "Random", "Teasing", "Funny", "Cute",
        "Motivational", "Good Morning", "Good Night", "Study Reminder"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Daily AI Notification");
        title.setTextSize(24);
        root.addView(title);

        categorySpinner = new Spinner(this);
        categorySpinner.setAdapter(new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, categories
        ));
        root.addView(categorySpinner);

        timePicker = new TimePicker(this);
        timePicker.setIs24HourView(true);
        root.addView(timePicker);

        enableSwitch = new Switch(this);
        enableSwitch.setText("Enable Daily Notification");
        root.addView(enableSwitch);

        Button save = new Button(this);
        save.setText("Save & Schedule");
        root.addView(save);

        Button test = new Button(this);
        test.setText("Test Notification");
        root.addView(test);

        Button alarmSettings = new Button(this);
        alarmSettings.setText("Open Exact Alarm Settings");
        root.addView(alarmSettings);

        Button notificationSettings = new Button(this);
        notificationSettings.setText("Open Notification Settings");
        root.addView(notificationSettings);

        setContentView(root);

        loadPrefs();

        save.setOnClickListener(v -> saveAndSchedule());
        test.setOnClickListener(v -> testNotification());
        alarmSettings.setOnClickListener(v -> openExactAlarmSettings());
        notificationSettings.setOnClickListener(v -> openNotificationSettings());

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001
            );
        }
    }

    private void loadPrefs() {
        android.content.SharedPreferences p =
            getSharedPreferences(PREFS, MODE_PRIVATE);
        enableSwitch.setChecked(p.getBoolean(KEY_ENABLED, false));
        int hour = p.getInt(KEY_HOUR, 8);
        int minute = p.getInt(KEY_MINUTE, 0);
        timePicker.setHour(hour);
        timePicker.setMinute(minute);

        String saved = p.getString(KEY_CATEGORY, "Random");
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(saved)) {
                categorySpinner.setSelection(i);
                break;
            }
        }
    }

    private void saveAndSchedule() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        String category = String.valueOf(categorySpinner.getSelectedItem());
        boolean enabled = enableSwitch.isChecked();

        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .putString(KEY_CATEGORY, category)
            .apply();

        if (enabled) {
            DailyMessageReceiver.scheduleNext(this, hour, minute);
            Toast.makeText(this, "Daily notification scheduled", Toast.LENGTH_SHORT).show();
        } else {
            DailyMessageReceiver.cancel(this);
            Toast.makeText(this, "Daily notification disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void testNotification() {
        NotificationHelper.show(
            this,
            "🤭 Test",
            "ဒီဟာက test notification ပါ။\n\nThis is a test notification."
        );
    }

    private void openExactAlarmSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void openNotificationSettings() {
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(i);
    }
}
