package com.example.mohochat.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mohochat.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MessageStatusService extends Service {
    private static final String TAG = "MessageStatusService";

    private DatabaseReference database;
    private FirebaseAuth auth;
    private String currentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            startListeningForMessageUpdates();
        }

        Log.d(TAG, "MessageStatusService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Restart if killed by system
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This is an unbound service
    }

    private void startListeningForMessageUpdates() {
        if (currentUserId == null) return;

        // Listen for user online status changes to update message delivery status
        database.child("user").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null && !userId.equals(currentUserId)) {
                        Boolean isOnline = userSnapshot.child("isOnline").getValue(Boolean.class);
                        if (Boolean.TRUE.equals(isOnline)) {
                            markMessagesToUserAsDelivered(userId);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to listen for user status changes", error.toException());
            }
        });
    }

    private void markMessagesToUserAsDelivered(String receiverId) {
        // Generate chat ID same way as in ChatActivity
        String chatId;
        if (currentUserId.compareTo(receiverId) < 0) {
            chatId = currentUserId + "_" + receiverId;
        } else {
            chatId = receiverId + "_" + currentUserId;
        }

        // Find messages sent by current user to this receiver that are not yet delivered
        database.child("messages").child(chatId)
                .orderByChild("senderId").equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> updates = new HashMap<>();

                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            Message message = messageSnapshot.getValue(Message.class);
                            if (message != null && !message.isDelivered() &&
                                message.getSenderId().equals(currentUserId)) {

                                String messageId = messageSnapshot.getKey();
                                updates.put("messages/" + chatId + "/" + messageId + "/delivered", true);
                                updates.put("messages/" + chatId + "/" + messageId + "/messageStatus", "delivered");
                            }
                        }

                        if (!updates.isEmpty()) {
                            database.updateChildren(updates)
                                .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Messages marked as delivered for user: " + receiverId))
                                .addOnFailureListener(e ->
                                    Log.e(TAG, "Failed to mark messages as delivered", e));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to query messages for delivery update", error.toException());
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MessageStatusService destroyed");
    }
}