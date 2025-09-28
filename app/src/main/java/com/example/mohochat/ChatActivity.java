package com.example.mohochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mohochat.adapters.MessagesAdapter;
import com.example.mohochat.models.Message;
import com.example.mohochat.services.PresenceService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.mohochat.utils.ProfileImageLoader;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;
    private ArrayList<Message> messagesList;

    private EditText messageInput;
    private ImageView sendButton, backButton;
    private TextView contactName, contactStatus;
    private CircleImageView contactProfilePic;

    private DatabaseReference database;
    private FirebaseAuth auth;

    private String receiverId, chatId, receiverName;
    private Users receiverUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        getIntentData();
        setupRecyclerView();
        loadReceiverInfo();
        loadMessages();
        setupClickListeners();

        // Start presence service to track user activity
        Intent presenceServiceIntent = new Intent(this, PresenceService.class);
        startService(presenceServiceIntent);
    }

    private void initViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);
        contactName = findViewById(R.id.contactName);
        contactStatus = findViewById(R.id.contactStatus);
        contactProfilePic = findViewById(R.id.contactProfilePic);

        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        messagesList = new ArrayList<>();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        receiverId = intent.getStringExtra("receiverId");
        receiverName = intent.getStringExtra("receiverName");

        // Generate chat ID (combination of both user IDs in alphabetical order)
        String currentUserId = auth.getCurrentUser().getUid();
        if (currentUserId.compareTo(receiverId) < 0) {
            chatId = currentUserId + "_" + receiverId;
        } else {
            chatId = receiverId + "_" + currentUserId;
        }

        // Set receiver name immediately if provided
        if (receiverName != null && !receiverName.isEmpty()) {
            contactName.setText(receiverName);
        }
    }

    private void setupRecyclerView() {
        messagesAdapter = new MessagesAdapter(this, messagesList, auth.getCurrentUser().getUid());
        messagesAdapter.setChatId(chatId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void loadReceiverInfo() {
        database.child("user").child(receiverId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                receiverUser = snapshot.getValue(Users.class);
                if (receiverUser != null) {
                    // Use fullName if available, otherwise userName, otherwise keep existing receiverName
                    String displayName = receiverUser.getUserName();
                    if (receiverName == null || receiverName.isEmpty()) {
                        contactName.setText(displayName != null ? displayName : "Unknown User");
                    }

                    updateContactStatus(receiverUser);

                    // Use letter avatar fallback for profile images
                    String profileDisplayName = receiverUser.getFullName();
                    if (profileDisplayName == null || profileDisplayName.isEmpty()) {
                        profileDisplayName = receiverUser.getUserName();
                    }
                    if (profileDisplayName == null || profileDisplayName.isEmpty()) {
                        profileDisplayName = "Unknown User";
                    }

                    ProfileImageLoader.loadProfileImageWithLetterFallback(
                        ChatActivity.this,
                        contactProfilePic,
                        receiverUser.getProfilepic(),
                        profileDisplayName
                    );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load contact info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        database.child("messages").child(chatId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Message message = dataSnapshot.getValue(Message.class);
                    if (message != null) {
                        messagesList.add(message);
                        // Mark messages from receiver as seen
                        if (!message.getSenderId().equals(auth.getCurrentUser().getUid()) &&
                            !message.isSeen()) {
                            markMessageAsSeen(message.getMessageId());
                        }
                    }
                }
                messagesAdapter.notifyDataSetChanged();
                if (messagesList.size() > 0) {
                    messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        sendButton.setOnClickListener(v -> sendMessage());

        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();
        String messageId = database.child("messages").child(chatId).push().getKey();
        long timestamp = System.currentTimeMillis();

        Message message = new Message(messageId, currentUserId, receiverId,
                messageText, "text", timestamp, chatId);

        // Explicitly set message status to sent
        message.setMessageStatus("sent");

        // Save message
        database.child("messages").child(chatId).child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                    updateLastMessage(messageText, timestamp);
                    // Send notification to receiver
                    sendNotificationToReceiver(messageText);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLastMessage(String lastMessage, long timestamp) {
        // Update chat info for both users
        String currentUserId = auth.getCurrentUser().getUid();

        // Create/update chat for current user
        DatabaseReference currentUserChatRef = database.child("chats").child(currentUserId).child(receiverId);
        currentUserChatRef.child("lastMessage").setValue(lastMessage);
        currentUserChatRef.child("lastMessageTime").setValue(timestamp);
        currentUserChatRef.child("timestamp").setValue(timestamp);
        currentUserChatRef.child("chatId").setValue(receiverId);
        currentUserChatRef.child("receiverName").setValue(receiverName != null ? receiverName : "Unknown User");

        // Create/update chat for receiver
        DatabaseReference receiverChatRef = database.child("chats").child(receiverId).child(currentUserId);
        receiverChatRef.child("lastMessage").setValue(lastMessage);
        receiverChatRef.child("lastMessageTime").setValue(timestamp);
        receiverChatRef.child("timestamp").setValue(timestamp);
        receiverChatRef.child("chatId").setValue(currentUserId);

        // Get current user name for receiver's chat list
        database.child("user").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentUserName = snapshot.child("fullName").getValue(String.class);
                    if (currentUserName == null) {
                        currentUserName = snapshot.child("userName").getValue(String.class);
                    }
                    receiverChatRef.child("receiverName").setValue(currentUserName != null ? currentUserName : "Unknown User");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void sendNotificationToReceiver(String messageText) {
        // Get receiver's FCM token and send notification
        database.child("user").child(receiverId).child("fcmToken")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String fcmToken = snapshot.getValue(String.class);
                        if (fcmToken != null && !fcmToken.isEmpty()) {
                            // Get current user's name
                            String currentUserId = auth.getCurrentUser().getUid();
                            database.child("user").child(currentUserId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            String senderName = snapshot.child("fullName").getValue(String.class);
                                            if (senderName == null) {
                                                senderName = snapshot.child("userName").getValue(String.class);
                                            }
                                            if (senderName == null) senderName = "Unknown User";

                                            // Send notification via Firebase Functions or server
                                            sendFCMNotification(fcmToken, senderName, messageText, currentUserId);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            // Handle error
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private void sendFCMNotification(String fcmToken, String senderName, String message, String senderId) {
        // Create notification payload in Firebase database for cloud function processing
        // This approach works without needing a separate server
        String notificationId = database.child("notifications").push().getKey();

        if (notificationId != null) {
            HashMap<String, Object> notificationData = new HashMap<>();
            notificationData.put("targetToken", fcmToken);
            notificationData.put("title", senderName);
            notificationData.put("body", message);
            notificationData.put("senderId", senderId);
            notificationData.put("senderName", senderName);
            notificationData.put("chatId", chatId);
            notificationData.put("timestamp", System.currentTimeMillis());
            notificationData.put("processed", false);

            database.child("notifications").child(notificationId).setValue(notificationData);
        }
    }

    private void updateContactStatus(Users user) {
        if (user == null) return;

        if (user.isOnline()) {
            contactStatus.setText("Online");
        } else {
            long lastSeenTime = user.getLastSeen();
            if (lastSeenTime > 0) {
                long currentTime = System.currentTimeMillis();
                long timeDiff = currentTime - lastSeenTime;

                String lastSeenText = formatLastSeen(timeDiff);
                contactStatus.setText(lastSeenText);
            } else {
                contactStatus.setText("Last seen recently");
            }
        }
    }

    private String formatLastSeen(long timeDiffMillis) {
        long seconds = timeDiffMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (minutes < 1) {
            return "Last seen just now";
        } else if (minutes < 60) {
            return "Last seen " + minutes + " minutes ago";
        } else if (hours < 24) {
            return "Last seen " + hours + " hours ago";
        } else if (days < 7) {
            return "Last seen " + days + " days ago";
        } else {
            return "Last seen a long time ago";
        }
    }

    private void markMessageAsSeen(String messageId) {
        database.child("messages").child(chatId).child(messageId)
                .child("seen").setValue(true);
        database.child("messages").child(chatId).child(messageId)
                .child("messageStatus").setValue("seen");
    }

    private void markMessageAsDelivered(String messageId) {
        database.child("messages").child(chatId).child(messageId)
                .child("delivered").setValue(true);
        database.child("messages").child(chatId).child(messageId)
                .child("messageStatus").setValue("delivered");
    }
}