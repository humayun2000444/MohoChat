package com.example.mohochat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.example.mohochat.utils.NotificationHelper;
import com.example.mohochat.utils.LocalNotificationHelper;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat darkModeSwitch;
    private SwitchCompat notificationsSwitch;
    private ImageView backButton;
    private LinearLayout notificationSoundOption;
    private TextView currentSoundName;
    private TextView testNotificationButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MohoChatSettings";
    private static final String DARK_MODE_KEY = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupSharedPreferences();
        setupDarkModeSwitch();
        setupNotificationSettings();
        setupBackButton();

        // Request notification permission
        NotificationHelper.requestNotificationPermission(this);

        // Initialize FCM
        NotificationHelper.initializeFCM();
    }

    private void initViews() {
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        notificationsSwitch = findViewById(R.id.notificationsSwitch);
        backButton = findViewById(R.id.backButton);
        notificationSoundOption = findViewById(R.id.notificationSoundOption);
        currentSoundName = findViewById(R.id.currentSoundName);
        testNotificationButton = findViewById(R.id.testNotificationButton);
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void setupDarkModeSwitch() {
        // Get current theme preference (default to light mode)
        boolean isDarkMode = sharedPreferences.getBoolean(DARK_MODE_KEY, false);
        darkModeSwitch.setChecked(isDarkMode);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(DARK_MODE_KEY, isChecked);
            editor.apply();

            // Apply theme immediately
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            // Recreate activity to apply theme
            recreate();
        });
    }

    private void setupNotificationSettings() {
        // Setup notifications switch
        boolean notificationsEnabled = NotificationHelper.areNotificationsEnabled(this);
        notificationsSwitch.setChecked(notificationsEnabled);

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            NotificationHelper.setNotificationsEnabled(this, isChecked);
            updateNotificationSoundVisibility(isChecked);
        });

        // Setup notification sound option
        updateCurrentSoundName();
        updateNotificationSoundVisibility(notificationsEnabled);

        notificationSoundOption.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationSoundActivity.class);
            startActivity(intent);
        });

        // Setup test notification button
        testNotificationButton.setOnClickListener(v -> {
            if (NotificationHelper.areNotificationsEnabled(this)) {
                LocalNotificationHelper.sendTestNotification(
                    this,
                    "MohoChat Test",
                    "This is a test notification to check your notification sound!"
                );
            } else {
                // Show message that notifications are disabled
                android.widget.Toast.makeText(this,
                    "Please enable notifications first",
                    android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNotificationSoundVisibility(boolean enabled) {
        notificationSoundOption.setAlpha(enabled ? 1.0f : 0.5f);
        notificationSoundOption.setEnabled(enabled);
        testNotificationButton.setAlpha(enabled ? 1.0f : 0.5f);
        testNotificationButton.setEnabled(enabled);
    }

    private void updateCurrentSoundName() {
        String soundName = NotificationHelper.getNotificationSoundName(this);
        currentSoundName.setText(soundName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCurrentSoundName();
    }

    private void setupBackButton() {
        backButton.setOnClickListener(v -> {
            finish();
        });
    }

    public static void applyTheme(AppCompatActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(DARK_MODE_KEY, false); // Default to light mode

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}