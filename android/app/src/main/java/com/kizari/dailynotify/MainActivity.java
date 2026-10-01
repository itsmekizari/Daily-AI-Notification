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
    private TextView customCountText;

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
        if (customCountText != null) {
            updateCustomCountText();
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

        customCountText = new TextView(this);
        customCountText.setTextSize(12);
        customCountText.setTextColor(Color.GRAY);
        customCountText.setPadding(0, dp(18), 0, 0);
        updateCustomCountText();
        root.addView(customCountText, matchWrap());

        setContentView(root);
    }

    private void showMoreMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Settings");
        menu.setOnMenuItemClickListener(item -> {
            showSettingsDialog();
            return true;
        });
        menu.show();
    }

    /** The option is intentionally tucked into the small ⋮ menu on the top-right. */
    private void showSettingsDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(6), dp(20), dp(4));

        TextView section = label("Other");
        box.addView(section, marginParams(0, 0, 0, 8));

        Button custom = button("Custom Messages");
        custom.setAllCaps(false);
        custom.setOnClickListener(v -> {
            dismissCurrentDialogIfNeeded();
            showCustomMessagesEditor();
        });
        box.addView(custom, matchWrap());

        settingsDialog = new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(box)
                .setNegativeButton("CLOSE", null)
                .create();
        settingsDialog.show();
    }

    private AlertDialog settingsDialog;

    private void dismissCurrentDialogIfNeeded() {
        if (settingsDialog != null && settingsDialog.isShowing()) settingsDialog.dismiss();
    }

    private void showCustomMessagesEditor() {
        final List<String> messages = new ArrayList<>(CustomMessageStore.getAll(this));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(6), dp(18), dp(8));
        scroll.addView(content);

        TextView hint = new TextView(this);
        hint.setText("Messages entered here are mixed into the random notification pool.");
        hint.setTextSize(12);
        hint.setTextColor(Color.GRAY);
        content.addView(hint, marginParams(0, 0, 0, 10));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list, matchWrap());

        final Runnable syncFields = new Runnable() {
            @Override public void run() {
                for (int i = 0; i < list.getChildCount() && i < messages.size(); i++) {
                    View child = list.getChildAt(i);
                    if (child instanceof LinearLayout) {
                        LinearLayout row = (LinearLayout) child;
                        if (row.getChildCount() > 0 && row.getChildAt(0) instanceof EditText) {
                            messages.set(i, ((EditText) row.getChildAt(0)).getText().toString());
                        }
                    }
                }
            }
        };

        final Runnable[] renderRef = new Runnable[1];
        renderRef[0] = new Runnable() {
            @Override public void run() {
                list.removeAllViews();
                for (int i = 0; i < messages.size(); i++) {
                    final int index = i;
                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(0, dp(6), 0, dp(10));

                    EditText edit = new EditText(MainActivity.this);
                    edit.setText(messages.get(i));
                    edit.setTextSize(15);
                    edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    edit.setMinLines(2);
                    edit.setGravity(Gravity.TOP | Gravity.START);
                    edit.setHint("Message " + (i + 1));
                    row.addView(edit, matchWrap());

                    Button remove = button("DELETE");
                    remove.setOnClickListener(v -> {
                        syncFields.run();
                        messages.remove(index);
                        renderRef[0].run();
                    });
                    row.addView(remove, wrapContent());
                    list.addView(row, matchWrap());
                }
            }
        };
        renderRef[0].run();

        Button add = button("+ ADD MESSAGE");
        add.setOnClickListener(v -> {
            syncFields.run();
            messages.add("");
            renderRef[0].run();
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        });
        content.addView(add, marginParams(0, 8, 0, 0));

        new AlertDialog.Builder(this)
                .setTitle("Custom Messages")
                .setView(scroll)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    syncFields.run();
                    ArrayList<String> cleaned = new ArrayList<>();
                    for (String message : messages) {
                        if (message != null && !message.trim().isEmpty()) cleaned.add(message.trim());
                    }
                    CustomMessageStore.saveAll(this, cleaned);
                    updateCustomCountText();
                    if (prefs.getBoolean("enabled", false)) DailyMessageReceiver.ensureNextMessage(this);
                    Toast.makeText(this, "Custom messages saved", Toast.LENGTH_SHORT).show();
                })
                .show();
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
