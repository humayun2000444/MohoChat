package com.example.mohochat.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.mohochat.MainActivityNew;
import com.example.mohochat.R;
import com.example.mohochat.ChatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MohoChatMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "MohoChat_Messages";
    private static final String PREFS_NAME = "MohoChatSettings";
    private static final String NOTIFICATION_SOUND_KEY = "notification_sound";
    private static final String NOTIFICATIONS_ENABLED_KEY = "notifications_enabled";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Send token to server
        sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if notifications are enabled
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(NOTIFICATIONS_ENABLED_KEY, true);

        if (!notificationsEnabled) {
            Log.d(TAG, "Notifications disabled by user");
            return;
        }

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            // Extract additional data
            Map<String, String> data = remoteMessage.getData();
            String senderId = data.get("senderId");
            String senderName = data.get("senderName");
            String chatId = data.get("chatId");

            showNotification(title != null ? title : senderName, body, senderId, chatId);
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        String messageType = data.get("type");
        String senderId = data.get("senderId");
        String senderName = data.get("senderName");
        String message = data.get("message");
        String chatId = data.get("chatId");

        if ("chat_message".equals(messageType)) {
            showNotification(senderName, message, senderId, chatId);
        }
    }

    private void showNotification(String title, String body, String senderId, String chatId) {
        // Don't show notification if sender is current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null && senderId != null &&
            senderId.equals(auth.getCurrentUser().getUid())) {
            return;
        }

        createNotificationChannel();

        Intent intent;
        if (chatId != null) {
            // Open specific chat
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiverId", senderId);
            intent.putExtra("receiverName", title);
            intent.putExtra("chatId", chatId);
        } else {
            // Open main activity
            intent = new Intent(this, MainActivityNew.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Get custom notification sound
        Uri soundUri = getNotificationSound();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setContentIntent(pendingIntent)
                .setColor(getResources().getColor(R.color.primary_accent))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MohoChat Messages";
            String description = "Notifications for new messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Set custom sound
            Uri soundUri = getNotificationSound();
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);

            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Uri getNotificationSound() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String soundUri = prefs.getString(NOTIFICATION_SOUND_KEY, "");

        if (soundUri.isEmpty()) {
            // Return default notification sound
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            return Uri.parse(soundUri);
        }
    }

    private void sendTokenToServer(String token) {
        // Save token to Firebase under user's profile
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("user")
                    .child(userId);

            userRef.child("fcmToken").setValue(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved to server"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save token", e));
        }
    }
}