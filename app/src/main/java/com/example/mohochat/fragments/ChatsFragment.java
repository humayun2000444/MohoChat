package com.example.mohochat.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohochat.R;
import com.example.mohochat.adapters.ChatsAdapter;
import com.example.mohochat.models.Chat;
import com.example.mohochat.models.ChatFromSMS;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ChatsFragment extends Fragment {

    private RecyclerView chatsRecyclerView;
    private ChatsAdapter chatsAdapter;
    private ArrayList<Chat> chatsList;
    private DatabaseReference database;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        loadChats();
    }

    private void initViews(View view) {
        chatsRecyclerView = view.findViewById(R.id.chatsRecyclerView);
        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        chatsList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        chatsAdapter = new ChatsAdapter(getContext(), chatsList);
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatsRecyclerView.setAdapter(chatsAdapter);
    }

    private void loadChats() {
        String currentUserId = auth.getCurrentUser().getUid();

        // Load all chats for current user (both SMS and message chats)
        database.child("chats").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatsList.clear();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    // Try to parse as ChatFromSMS first (for SMS invites)
                    ChatFromSMS smsChat = chatSnapshot.getValue(ChatFromSMS.class);
                    if (smsChat != null && "sms_invite".equals(smsChat.getType())) {
                        // SMS Chat
                        Chat chat = new Chat();
                        chat.setChatId(smsChat.getChatId());
                        chat.setLastMessage(smsChat.getLastMessage());
                        chat.setTimestamp(smsChat.getTimestamp());
                        chat.setReceiverName(smsChat.getContactName());
                        chat.setReceiverPhone(smsChat.getReceiverPhone());
                        chat.setChatType("sms_invite");
                        chatsList.add(chat);
                    } else {
                        // Regular message chat - parse as generic data
                        String receiverId = chatSnapshot.getKey();
                        String lastMessage = chatSnapshot.child("lastMessage").getValue(String.class);
                        String receiverName = chatSnapshot.child("receiverName").getValue(String.class);
                        Long timestamp = chatSnapshot.child("timestamp").getValue(Long.class);
                        Long lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long.class);

                        if (lastMessage != null && (timestamp != null || lastMessageTime != null)) {
                            Chat chat = new Chat();
                            chat.setChatId(receiverId);
                            chat.setLastMessage(lastMessage);
                            chat.setTimestamp(timestamp != null ? timestamp : lastMessageTime);
                            chat.setReceiverName(receiverName != null ? receiverName : "Unknown User");
                            chat.setChatType("message");
                            chatsList.add(chat);
                        }
                    }
                }

                // Sort chats by timestamp (newest first)
                chatsList.sort((c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));

                chatsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void loadReceiverInfo(Chat chat, String receiverId) {
        database.child("user").child(receiverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Try fullName first (from profile setup), then userName (from registration)
                    String receiverName = snapshot.child("fullName").getValue(String.class);
                    if (receiverName == null || receiverName.isEmpty()) {
                        receiverName = snapshot.child("userName").getValue(String.class);
                    }
                    if (receiverName == null || receiverName.isEmpty()) {
                        // Fallback to phone number if no name found
                        String phone = snapshot.child("phoneNumber").getValue(String.class);
                        receiverName = phone != null ? phone : "Unknown User";
                    }
                    chat.setReceiverName(receiverName);
                    chatsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

}