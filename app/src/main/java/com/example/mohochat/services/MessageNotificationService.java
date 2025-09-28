package com.example.mohochat.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mohochat.utils.LocalNotificationHelper;
import com.example.mohochat.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MessageNotificationService extends Service {

    private static final String TAG = "MessageNotificationService";
    private DatabaseReference database;
    private FirebaseAuth auth;
    private ChildEventListener notificationListener;
    private Map<String, Long> lastMessageTimes = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            startListeningForMessages();
        }
    }

    private void startListeningForMessages() {
        String currentUserId = auth.getCurrentUser().getUid();

        // Listen for new chats involving this user
        database.child("chats").child(currentUserId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        String chatPartnerId = snapshot.getKey();
                        if (chatPartnerId != null) {
                            startListeningForChatMessages(currentUserId, chatPartnerId);
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        // Handle chat changes if needed
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                        // Handle chat removal if needed
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        // Not used
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error listening for chats: " + error.getMessage());
                    }
                });
    }

    private void startListeningForChatMessages(String currentUserId, String chatPartnerId) {
        // Generate chat ID (same logic as ChatActivity)
        String chatId;
        if (currentUserId.compareTo(chatPartnerId) < 0) {
            chatId = currentUserId + "_" + chatPartnerId;
        } else {
            chatId = chatPartnerId + "_" + currentUserId;
        }

        // Listen for new messages in this chat
        database.child("messages").child(chatId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        processNewMessage(snapshot, currentUserId, chatPartnerId);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        // Not used for messages
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                        // Not used for messages
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        // Not used for messages
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error listening for messages: " + error.getMessage());
                    }
                });
    }

    private void processNewMessage(DataSnapshot messageSnapshot, String currentUserId, String chatPartnerId) {
        String senderId = messageSnapshot.child("senderId").getValue(String.class);
        String messageText = messageSnapshot.child("message").getValue(String.class);
        Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);

        // Only show notification if:
        // 1. Message is not from current user
        // 2. Notifications are enabled
        // 3. This is a new message (not from initial load)
        if (senderId != null && !senderId.equals(currentUserId) &&
            NotificationHelper.areNotificationsEnabled(this) &&
            timestamp != null && isNewMessage(chatPartnerId, timestamp)) {

            // Get sender's name and show notification
            database.child("user").child(senderId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String senderName = snapshot.child("fullName").getValue(String.class);
                            if (senderName == null) {
                                senderName = snapshot.child("userName").getValue(String.class);
                            }
                            if (senderName == null) senderName = "Unknown User";

                            // Show notification
                            LocalNotificationHelper.sendMessageNotification(
                                MessageNotificationService.this,
                                senderName,
                                messageText != null ? messageText : "New message"
                            );

                            Log.d(TAG, "Notification sent for message from: " + senderName);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error getting sender info: " + error.getMessage());
                        }
                    });

            // Update last message time for this chat
            lastMessageTimes.put(chatPartnerId, timestamp);
        }
    }

    private boolean isNewMessage(String chatPartnerId, long messageTimestamp) {
        Long lastTime = lastMessageTimes.get(chatPartnerId);
        if (lastTime == null) {
            // First message, consider it "new" only if it's very recent (within last 10 seconds)
            return (System.currentTimeMillis() - messageTimestamp) < 10000;
        }
        return messageTimestamp > lastTime;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Restart service if killed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            // Clean up listeners if needed
        }
    }
}