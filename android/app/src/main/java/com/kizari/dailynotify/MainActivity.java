package com.kizari.dailynotify;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "daily_ai_prefs";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 5001;
    private static final String[] LANGUAGES = {"Burmese + English", "Burmese only", "English only"};

    private SharedPreferences prefs;
    private Spinner languageSpinner;
    private Switch enabledSwitch;
    private TextView timeText;
    private int selectedHour = 8;
    private int selectedMinute = 0;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadPrefs();
        buildUi();
        requestNotificationPermissionIfNeeded();
    }

    @Override protected void onResume() {
        super.onResume();
        if (prefs != null && prefs.getBoolean("enabled", false)) {
            DailyMessageReceiver.ensureNextMessage(this);
            DailyMessageReceiver.scheduleNext(this);
        }
    }

    private void loadPrefs() {
        selectedHour = prefs.getInt("hour", 8);
        selectedMinute = prefs.getInt("minute", 0);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(18), dp(24), dp(24));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Daily AI Notification");
        title.setTextSize(21);
        title.setTextColor(Color.BLACK);
        topRow.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1f));

        TextView more = new TextView(this);
        more.setText("⋮");
        more.setGravity(Gravity.CENTER);
        more.setTextSize(28);
        more.setTextColor(Color.DKGRAY);
        more.setContentDescription("More options");
        more.setPadding(dp(10), 0, dp(4), 0);
        more.setOnClickListener(this::showMoreMenu);
        topRow.addView(more, new LinearLayout.LayoutParams(dp(48), dp(52)));
        root.addView(topRow);

        root.addView(label("Language"), marginParams(0, 8, 0, 6));
        languageSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, LANGUAGES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
        selectSpinner(languageSpinner, prefs.getString("language", "Burmese + English"));
        root.addView(languageSpinner, matchWrap());

        root.addView(label("Daily time"), marginParams(0, 22, 0, 6));
        timeText = new TextView(this);
        timeText.setTextSize(24);
        updateTimeText();
        root.addView(timeText, marginParams(0, 0, 0, 10));

        Button setTime = button("SET TIME");
        setTime.setOnClickListener(v -> showTimePicker());
        root.addView(setTime, matchWrap());

        enabledSwitch = new Switch(this);
        enabledSwitch.setText("Enable daily notification");
        enabledSwitch.setTextSize(17);
        enabledSwitch.setChecked(prefs.getBoolean("enabled", false));
        root.addView(enabledSwitch, marginParams(0, 22, 0, 12));

        Button save = button("SAVE & SCHEDULE");
        save.setOnClickListener(v -> saveAndSchedule());
        root.addView(save, matchWrap());

        Button test = button("TEST NOTIFICATION");
        test.setOnClickListener(v -> sendTestNotification());
        root.addView(test, marginParams(0, 10, 0, 0));

        setContentView(root);
    }

    private void showMoreMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("About");
        menu.setOnMenuItemClickListener(item -> {
            new AlertDialog.Builder(this)
                    .setTitle("Daily AI Notification")
                    .setMessage("10,000 built-in local notification messages.\n\nNo message editor is required. Messages are selected randomly without repeating until the pool is exhausted.")
                    .setPositiveButton("OK", null)
                    .show();
            return true;
        });
        menu.show();
    }

    private void sendTestNotification() {
        String language = String.valueOf(languageSpinner.getSelectedItem());
        String message = DailyMessageReceiver.peekNextText(this, language);
        if (message == null || message.trim().isEmpty()) {
            Toast.makeText(this, "No message available", Toast.LENGTH_SHORT).show();
            return;
        }
        NotificationHelper.show(this, "Test Notification", message);
    }

    private void updateCustomCountText() {
        if (customCountText != null) {
            customCountText.setText(CustomMessageStore.getCount(this) + " custom message(s) saved");
        }
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(18);
        return v;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            selectedHour = hour;
            selectedMinute = minute;
            updateTimeText();
        }, selectedHour, selectedMinute, false).show();
    }

    private void updateTimeText() {
        if (timeText == null) return;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, selectedHour);
        c.set(Calendar.MINUTE, selectedMinute);
        timeText.setText(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(c.getTime()));
    }

    private void saveAndSchedule() {
        String language = String.valueOf(languageSpinner.getSelectedItem());
        boolean enabled = enabledSwitch.isChecked();
        prefs.edit()
                .putString("language", language)
                .putInt("hour", selectedHour)
                .putInt("minute", selectedMinute)
                .putBoolean("enabled", enabled)
                .remove("next_index")
                .remove("remaining_order")
                .remove("remaining_cursor")
                .apply();
        if (enabled) {
            DailyMessageReceiver.ensureNextMessage(this);
            DailyMessageReceiver.scheduleNext(this);
        } else {
            DailyMessageReceiver.cancel(this);
        }
        Toast.makeText(this, enabled ? "Daily notification scheduled" : "Daily notification disabled", Toast.LENGTH_SHORT).show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void selectSpinner(Spinner spinner, String value) {
        ArrayAdapter<?> a = (ArrayAdapter<?>) spinner.getAdapter();
        for (int i = 0; i < a.getCount(); i++) {
            if (value.equals(a.getItem(i))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapContent() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams marginParams(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = matchWrap();
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
