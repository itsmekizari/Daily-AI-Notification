package com.kizari.dailynotify;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

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
        ensureDeviceId(); loadPrefs(); buildUi(); requestNotificationPermissionIfNeeded();
    }

    @Override protected void onResume() {
        super.onResume();
        if (prefs != null && prefs.getBoolean("enabled", false)) {
            DailyMessageReceiver.ensureNextMessage(this);
            DailyMessageReceiver.scheduleNext(this);
        }
    }

    private void ensureDeviceId() {
        String id = prefs.getString("device_id", null);
        if (id == null || id.trim().isEmpty()) prefs.edit().putString("device_id", UUID.randomUUID().toString()).apply();
    }

    private void loadPrefs() {
        selectedHour = prefs.getInt("hour", 8); selectedMinute = prefs.getInt("minute", 0);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24), dp(28), dp(24), dp(24));

        TextView languageLabel = label("Language");
        root.addView(languageLabel, marginParams(0,0,0,6));
        languageSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, LANGUAGES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
        selectSpinner(languageSpinner, prefs.getString("language", "Burmese + English"));
        root.addView(languageSpinner, matchWrap());

        root.addView(label("Daily time"), marginParams(0,22,0,6));
        timeText = new TextView(this); timeText.setTextSize(24); updateTimeText();
        root.addView(timeText, marginParams(0,0,0,10));
        Button setTime = button("SET TIME"); setTime.setOnClickListener(v -> showTimePicker());
        root.addView(setTime, matchWrap());

        enabledSwitch = new Switch(this); enabledSwitch.setText("Enable daily notification"); enabledSwitch.setTextSize(17);
        enabledSwitch.setChecked(prefs.getBoolean("enabled", false));
        root.addView(enabledSwitch, marginParams(0,22,0,12));

        Button save = button("SAVE & SCHEDULE"); save.setOnClickListener(v -> saveAndSchedule());
        root.addView(save, matchWrap());
        setContentView(root);
    }

    private TextView label(String text) { TextView v = new TextView(this); v.setText(text); v.setTextSize(18); return v; }
    private Button button(String text) { Button b = new Button(this); b.setText(text); return b; }

    private void showTimePicker() {
        new TimePickerDialog(this, (view,hour,minute) -> { selectedHour=hour; selectedMinute=minute; updateTimeText(); }, selectedHour, selectedMinute, false).show();
    }

    private void updateTimeText() {
        if (timeText == null) return; Calendar c = Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY,selectedHour); c.set(Calendar.MINUTE,selectedMinute);
        timeText.setText(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(c.getTime()));
    }

    private void saveAndSchedule() {
        String language = String.valueOf(languageSpinner.getSelectedItem()); boolean enabled = enabledSwitch.isChecked();
        prefs.edit().putString("language",language).putInt("hour",selectedHour).putInt("minute",selectedMinute).putBoolean("enabled",enabled).remove("next_index").apply();
        if (enabled) { DailyMessageReceiver.ensureNextMessage(this); DailyMessageReceiver.scheduleNext(this); }
        else { DailyMessageReceiver.cancel(this); }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
    }

    private void selectSpinner(Spinner spinner,String value) { ArrayAdapter<?> a=(ArrayAdapter<?>)spinner.getAdapter(); for(int i=0;i<a.getCount();i++) if(value.equals(a.getItem(i))){spinner.setSelection(i);return;} }
    private LinearLayout.LayoutParams matchWrap(){ return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams marginParams(int l,int t,int r,int b){ LinearLayout.LayoutParams p=matchWrap(); p.setMargins(dp(l),dp(t),dp(r),dp(b)); return p; }
    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
}
