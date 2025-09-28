package com.example.mohochat.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohochat.R;
import com.example.mohochat.models.Message;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MESSAGE_SENT = 1;
    private static final int TYPE_MESSAGE_RECEIVED = 2;

    private Context context;
    private ArrayList<Message> messagesList;
    private String currentUserId;
    private String chatId;

    public MessagesAdapter(Context context, ArrayList<Message> messagesList, String currentUserId) {
        this.context = context;
        this.messagesList = messagesList;
        this.currentUserId = currentUserId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messagesList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return TYPE_MESSAGE_SENT;
        } else {
            return TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MESSAGE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messagesList.get(position);

        if (holder.getItemViewType() == TYPE_MESSAGE_SENT) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            sentHolder.messageText.setText(message.getMessageText());
            sentHolder.timestamp.setText(formatTimestamp(message.getTimestamp()));
            updateMessageStatus(sentHolder.messageStatus, message);

            // Set up real-time status updates for sent messages
            if (chatId != null && message.getMessageId() != null) {
                listenForStatusUpdates(sentHolder.messageStatus, message.getMessageId());
            }
        } else {
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            receivedHolder.messageText.setText(message.getMessageText());
            receivedHolder.timestamp.setText(formatTimestamp(message.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void updateMessageStatus(android.widget.ImageView statusIcon, Message message) {
        if (statusIcon == null) {
            android.util.Log.e("MessagesAdapter", "StatusIcon is null!");
            return;
        }

        String status = message.getMessageStatus();
        if (status == null || status.isEmpty()) {
            status = "sent"; // Default to sent if status is null or empty
        }

        android.util.Log.d("MessagesAdapter", "Updating message status: " + status + " for message: " + message.getMessageId());

        // Ensure icon is visible
        statusIcon.setVisibility(android.view.View.VISIBLE);
        statusIcon.clearColorFilter();

        switch (status) {
            case "sent":
                statusIcon.setImageResource(R.drawable.ic_message_sent);
                statusIcon.setAlpha(0.7f);
                android.util.Log.d("MessagesAdapter", "Set SENT icon");
                break;
            case "delivered":
                statusIcon.setImageResource(R.drawable.ic_message_delivered);
                statusIcon.setAlpha(0.9f);
                android.util.Log.d("MessagesAdapter", "Set DELIVERED icon");
                break;
            case "seen":
                statusIcon.setImageResource(R.drawable.ic_message_seen);
                statusIcon.setAlpha(1.0f);
                android.util.Log.d("MessagesAdapter", "Set SEEN icon");
                break;
            default:
                statusIcon.setImageResource(R.drawable.ic_message_sent);
                statusIcon.setAlpha(0.7f);
                android.util.Log.d("MessagesAdapter", "Set DEFAULT icon");
                break;
        }
    }

    private void listenForStatusUpdates(android.widget.ImageView statusIcon, String messageId) {
        if (chatId == null || messageId == null) return;

        DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference()
                .child("messages").child(chatId).child(messageId);

        messageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Message updatedMessage = snapshot.getValue(Message.class);
                    if (updatedMessage != null) {
                        updateMessageStatus(statusIcon, updatedMessage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("MessagesAdapter", "Failed to listen for status updates", error.toException());
            }
        });
    }

    public static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timestamp;
        android.widget.ImageView messageStatus;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestamp = itemView.findViewById(R.id.timestamp);
            messageStatus = itemView.findViewById(R.id.messageStatus);
        }
    }

    public static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timestamp;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestamp = itemView.findViewById(R.id.timestamp);
        }
    }
}