package com.kizari.dailynotify;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Daily AI Notification");
        title.setTextSize(24);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        categorySpinner = new Spinner(this);

        categorySpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        categories
                )
        );

        root.addView(categorySpinner);

        timePicker = new TimePicker(this);
        timePicker.setIs24HourView(true);

        root.addView(timePicker);

        enableSwitch = new Switch(this);
        enableSwitch.setText(
                "Enable Daily Notification"
        );

        root.addView(enableSwitch);

        Button save = new Button(this);
        save.setText("Save & Schedule");
        root.addView(save);

        Button test = new Button(this);
        test.setText("Test Notification");
        root.addView(test);

        Button alarmSettings = new Button(this);
        alarmSettings.setText(
                "Open Exact Alarm Settings"
        );
        root.addView(alarmSettings);

        Button notificationSettings = new Button(this);
        notificationSettings.setText(
                "Open Notification Settings"
        );
        root.addView(notificationSettings);

        setContentView(root);

        loadPrefs();

        save.setOnClickListener(
                v -> saveAndSchedule()
        );

        test.setOnClickListener(
                v -> testNotification()
        );

        alarmSettings.setOnClickListener(
                v -> openExactAlarmSettings()
        );

        notificationSettings.setOnClickListener(
                v -> openNotificationSettings()
        );

        requestNotificationPermission();
    }

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= 33) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        1001
                );
            }
        }
    }

    private void loadPrefs() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                );

        enableSwitch.setChecked(
                prefs.getBoolean(
                        KEY_ENABLED,
                        false
                )
        );

        int hour =
                prefs.getInt(
                        KEY_HOUR,
                        8
                );

        int minute =
                prefs.getInt(
                        KEY_MINUTE,
                        0
                );

        timePicker.setHour(hour);
        timePicker.setMinute(minute);

        String savedCategory =
                prefs.getString(
                        KEY_CATEGORY,
                        "Random"
                );

        for (int i = 0;
             i < categories.length;
             i++) {

            if (categories[i].equals(
                    savedCategory
            )) {

                categorySpinner.setSelection(i);
                break;
            }
        }
    }

    private void saveAndSchedule() {

        int hour =
                timePicker.getHour();

        int minute =
                timePicker.getMinute();

        String category =
                String.valueOf(
                        categorySpinner
                                .getSelectedItem()
                );

        boolean enabled =
                enableSwitch.isChecked();

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                );

        prefs.edit()
                .putBoolean(
                        KEY_ENABLED,
                        enabled
                )
                .putInt(
                        KEY_HOUR,
                        hour
                )
                .putInt(
                        KEY_MINUTE,
                        minute
                )
                .putString(
                        KEY_CATEGORY,
                        category
                )
                .apply();

        if (enabled) {

            DailyMessageReceiver.scheduleNext(
                    this
            );

            Toast.makeText(
                    this,
                    "Daily notification scheduled",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            DailyMessageReceiver.cancel(
                    this
            );

            Toast.makeText(
                    this,
                    "Daily notification disabled",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void testNotification() {

        NotificationHelper.show(
                this,
                "🤭 Test",
                "ဒီဟာက test notification ပါ။\n\n"
                        + "This is a test notification."
        );
    }

    private void openExactAlarmSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings
                                    .ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    );

            intent.setData(
                    Uri.parse(
                            "package:" + getPackageName()
                    )
            );

            startActivity(intent);

        } catch (Exception e) {

            startActivity(
                    new Intent(
                            Settings.ACTION_SETTINGS
                    )
            );
        }
    }

    private void openNotificationSettings() {

        Intent intent =
                new Intent(
                        Settings
                                .ACTION_APP_NOTIFICATION_SETTINGS
                );

        intent.putExtra(
                Settings.EXTRA_APP_PACKAGE,
                getPackageName()
        );

        startActivity(intent);
    }
        }
