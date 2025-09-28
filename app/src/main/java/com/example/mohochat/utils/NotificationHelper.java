package com.example.mohochat.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationHelper {

    private static final String PREFS_NAME = "MohoChatSettings";
    private static final String NOTIFICATION_SOUND_KEY = "notification_sound";
    private static final String NOTIFICATION_SOUND_NAME_KEY = "notification_sound_name";
    private static final String NOTIFICATIONS_ENABLED_KEY = "notifications_enabled";
    private static final int NOTIFICATION_PERMISSION_CODE = 103;

    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(NOTIFICATIONS_ENABLED_KEY, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(NOTIFICATIONS_ENABLED_KEY, enabled).apply();
    }

    public static void setNotificationSound(Context context, Uri soundUri, String soundName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(NOTIFICATION_SOUND_KEY, soundUri.toString());
        editor.putString(NOTIFICATION_SOUND_NAME_KEY, soundName);
        editor.apply();
    }

    public static String getNotificationSoundName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(NOTIFICATION_SOUND_NAME_KEY, "Default");
    }

    public static Uri getNotificationSoundUri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String soundUri = prefs.getString(NOTIFICATION_SOUND_KEY, "");

        if (soundUri.isEmpty()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            return Uri.parse(soundUri);
        }
    }

    public static List<NotificationSound> getAvailableNotificationSounds(Context context) {
        List<NotificationSound> sounds = new ArrayList<>();

        // Add default sound
        sounds.add(new NotificationSound("Default",
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)));

        // Get system notification sounds
        RingtoneManager manager = new RingtoneManager(context);
        manager.setType(RingtoneManager.TYPE_NOTIFICATION);
        Cursor cursor = manager.getCursor();

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                Uri uri = manager.getRingtoneUri(cursor.getPosition());
                sounds.add(new NotificationSound(title, uri));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return sounds;
    }

    public static void initializeFCM() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();

                // Save to user profile
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() != null) {
                    String userId = auth.getCurrentUser().getUid();
                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                            .getReference("user")
                            .child(userId);
                    userRef.child("fcmToken").setValue(token);
                }
            });
    }

    public static void sendMessageNotification(String targetUserId, String senderName, String message, String chatId) {
        // This would typically be done through a cloud function or server
        // For demonstration, we're showing the structure
        Map<String, String> data = new HashMap<>();
        data.put("type", "chat_message");
        data.put("senderId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        data.put("senderName", senderName);
        data.put("message", message);
        data.put("chatId", chatId);

        // In a real implementation, you would send this to your server
        // which would then send the FCM message to the target user
    }

    public static class NotificationSound {
        private String name;
        private Uri uri;

        public NotificationSound(String name, Uri uri) {
            this.name = name;
            this.uri = uri;
        }

        public String getName() {
            return name;
        }

        public Uri getUri() {
            return uri;
        }
    }
}