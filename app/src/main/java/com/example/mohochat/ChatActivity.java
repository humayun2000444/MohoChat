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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

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

    private String receiverId, chatId;
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

        // Generate chat ID (combination of both user IDs in alphabetical order)
        String currentUserId = auth.getCurrentUser().getUid();
        if (currentUserId.compareTo(receiverId) < 0) {
            chatId = currentUserId + "_" + receiverId;
        } else {
            chatId = receiverId + "_" + currentUserId;
        }
    }

    private void setupRecyclerView() {
        messagesAdapter = new MessagesAdapter(this, messagesList, auth.getCurrentUser().getUid());
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
                    contactName.setText(receiverUser.getUserName());

                    if (receiverUser.isOnline()) {
                        contactStatus.setText("Online");
                    } else {
                        contactStatus.setText("Last seen recently");
                    }

                    if (receiverUser.getProfilepic() != null && !receiverUser.getProfilepic().isEmpty()) {
                        Picasso.get().load(receiverUser.getProfilepic()).into(contactProfilePic);
                    }
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

        // Save message
        database.child("messages").child(chatId).child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                    updateLastMessage(messageText, timestamp);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLastMessage(String lastMessage, long timestamp) {
        // Update chat info for both users
        String currentUserId = auth.getCurrentUser().getUid();

        database.child("chats").child(currentUserId).child(chatId).child("lastMessage").setValue(lastMessage);
        database.child("chats").child(currentUserId).child(chatId).child("lastMessageTime").setValue(timestamp);

        database.child("chats").child(receiverId).child(chatId).child("lastMessage").setValue(lastMessage);
        database.child("chats").child(receiverId).child(chatId).child("lastMessageTime").setValue(timestamp);
    }
}