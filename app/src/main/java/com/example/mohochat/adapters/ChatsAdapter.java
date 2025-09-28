package com.example.mohochat.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohochat.R;
import com.example.mohochat.models.Chat;
import com.example.mohochat.Users;
import com.example.mohochat.utils.ProfileImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private Context context;
    private ArrayList<Chat> chatsList;
    private boolean isDeleteMode = false;

    public ChatsAdapter(Context context, ArrayList<Chat> chatsList) {
        this.context = context;
        this.chatsList = chatsList;
    }

    public void setDeleteMode(boolean deleteMode) {
        this.isDeleteMode = deleteMode;
        notifyDataSetChanged();
    }

    public boolean isDeleteMode() {
        return isDeleteMode;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatsList.get(position);

        // Display chat name (use receiverName for SMS chats, or default for regular chats)
        if (chat.getReceiverName() != null && !chat.getReceiverName().isEmpty()) {
            holder.chatName.setText(chat.getReceiverName());
        } else {
            holder.chatName.setText("Chat " + position); // Temporary for regular chats
        }

        holder.lastMessage.setText(chat.getLastMessage());

        // Format timestamp (use timestamp field if available, otherwise lastMessageTime)
        long timeToFormat = chat.getTimestamp() != 0 ? chat.getTimestamp() : chat.getLastMessageTime();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(new Date(timeToFormat));
        holder.timestamp.setText(formattedTime);

        // Show unread count if > 0
        if (chat.getUnreadCount() > 0) {
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText(String.valueOf(chat.getUnreadCount()));
        } else {
            holder.unreadCount.setVisibility(View.GONE);
        }

        // Load profile picture for regular chats, show SMS icon for SMS chats
        if (chat.getReceiverPhone() != null && !chat.getReceiverPhone().isEmpty()) {
            // This is an SMS chat - show SMS icon
            holder.profileImage.setImageResource(R.drawable.ic_launcher_foreground);
            holder.onlineIndicator.setVisibility(View.GONE); // No online status for SMS
        } else {
            // Regular chat - load profile image from user data and check online status
            loadProfileImage(holder.profileImage, chat.getChatId());
            checkUserOnlineStatus(holder.onlineIndicator, chat.getChatId());
        }

        // Show/hide delete button based on delete mode
        if (isDeleteMode) {
            holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }

        // Delete button click
        holder.deleteButton.setOnClickListener(v -> {
            showDeleteConfirmationDialog(chat, position);
        });

        // Long press to enter delete mode
        holder.itemView.setOnLongClickListener(v -> {
            if (!isDeleteMode) {
                setDeleteMode(true);
                return true;
            }
            return false;
        });

        // Regular click functionality
        holder.itemView.setOnClickListener(v -> {
            if (isDeleteMode) {
                // Exit delete mode when tapping on item
                setDeleteMode(false);
            } else {
                if (chat.getReceiverPhone() != null && !chat.getReceiverPhone().isEmpty()) {
                    // SMS chat - show message that user needs to install app first
                    Toast.makeText(context, chat.getReceiverName() + " hasn't installed MohoChat yet", Toast.LENGTH_SHORT).show();
                } else {
                    // Regular chat - open chat activity
                    Intent intent = new Intent(context, com.example.mohochat.ChatActivity.class);
                    intent.putExtra("receiverId", chat.getChatId()); // chatId is actually receiverId for message chats
                    intent.putExtra("receiverName", chat.getReceiverName());
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatsList.size();
    }

    private void loadProfileImage(CircleImageView profileImageView, String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("user").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Users user = snapshot.getValue(Users.class);
                    if (user != null) {
                        // Get user name for letter avatar fallback
                        String userName = user.getFullName();
                        if (userName == null || userName.isEmpty()) {
                            userName = user.getUserName();
                        }
                        if (userName == null || userName.isEmpty()) {
                            userName = "Unknown User";
                        }

                        // Use letter avatar fallback
                        ProfileImageLoader.loadProfileImageWithLetterFallback(
                            context,
                            profileImageView,
                            user.getProfilepic(),
                            userName
                        );
                    }
                } else {
                    // User not found - create letter avatar with "?"
                    ProfileImageLoader.loadProfileImageWithLetterFallback(
                        context,
                        profileImageView,
                        null,
                        "Unknown User"
                    );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error - show letter avatar with "?"
                ProfileImageLoader.loadProfileImageWithLetterFallback(
                    context,
                    profileImageView,
                    null,
                    "?"
                );
            }
        });
    }

    private void showDeleteConfirmationDialog(Chat chat, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_chat, null);

        TextView deleteMessage = dialogView.findViewById(R.id.deleteMessage);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);

        // Customize message based on chat type
        String chatName = chat.getReceiverName() != null ? chat.getReceiverName() : "this chat";
        deleteMessage.setText("Are you sure you want to delete the chat with " + chatName + "?\n\nAll messages will be permanently removed.");

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            setDeleteMode(false); // Exit delete mode
        });

        btnDelete.setOnClickListener(v -> {
            deleteChat(chat, position);
            dialog.dismiss();
            setDeleteMode(false); // Exit delete mode
        });

        dialog.show();
    }

    private void deleteChat(Chat chat, int position) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        // Show loading message
        Toast.makeText(context, "Deleting chat...", Toast.LENGTH_SHORT).show();

        if (chat.getReceiverPhone() != null && !chat.getReceiverPhone().isEmpty()) {
            // SMS invite chat - delete from Firebase permanently
            String chatId = chat.getChatId();

            // Delete SMS invite from current user's chat list
            database.child("chats").child(currentUserId).child(chatId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Remove from UI
                    chatsList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, chatsList.size());
                    Toast.makeText(context, "SMS invite deleted permanently", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete SMS invite: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } else {
            // Regular chat - complete deletion from Firebase
            String receiverId = chat.getChatId(); // chatId is actually receiverId

            // Generate the same chatId format used in ChatActivity
            String messagesChatId;
            if (currentUserId.compareTo(receiverId) < 0) {
                messagesChatId = currentUserId + "_" + receiverId;
            } else {
                messagesChatId = receiverId + "_" + currentUserId;
            }

            // Step 1: Delete all messages for this chat
            database.child("messages").child(messagesChatId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Step 2: Remove chat from current user's chat list
                    database.child("chats").child(currentUserId).child(receiverId).removeValue()
                        .addOnSuccessListener(aVoid2 -> {
                            // Step 3: Remove chat from receiver's chat list
                            database.child("chats").child(receiverId).child(currentUserId).removeValue()
                                .addOnSuccessListener(aVoid3 -> {
                                    // Step 4: Clean up any notifications related to this chat
                                    cleanupChatNotifications(database, messagesChatId, currentUserId, receiverId);

                                    // Step 5: Remove from UI
                                    chatsList.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, chatsList.size());

                                    Toast.makeText(context, "Chat deleted completely", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to clean receiver's chat list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to clean your chat list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void cleanupChatNotifications(DatabaseReference database, String chatId, String currentUserId, String receiverId) {
        // Query and delete notifications related to this chat
        database.child("notifications").orderByChild("chatId").equalTo(chatId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                        // Delete each notification related to this chat
                        notificationSnapshot.getRef().removeValue();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Non-critical error - notifications cleanup failed but chat is deleted
                    android.util.Log.w("ChatsAdapter", "Failed to cleanup notifications: " + error.getMessage());
                }
            });

        // Also clean up notifications where senderId matches the participants
        database.child("notifications").orderByChild("senderId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                        Object chatIdValue = notificationSnapshot.child("chatId").getValue();
                        if (chatIdValue != null && chatIdValue.toString().equals(chatId)) {
                            notificationSnapshot.getRef().removeValue();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    android.util.Log.w("ChatsAdapter", "Failed to cleanup sender notifications: " + error.getMessage());
                }
            });

        database.child("notifications").orderByChild("senderId").equalTo(receiverId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                        Object chatIdValue = notificationSnapshot.child("chatId").getValue();
                        if (chatIdValue != null && chatIdValue.toString().equals(chatId)) {
                            notificationSnapshot.getRef().removeValue();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    android.util.Log.w("ChatsAdapter", "Failed to cleanup receiver notifications: " + error.getMessage());
                }
            });
    }

    private void checkUserOnlineStatus(View onlineIndicator, String userId) {
        DatabaseReference presenceRef = FirebaseDatabase.getInstance().getReference().child("presence").child(userId);
        presenceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue(String.class);
                    if ("online".equals(status)) {
                        onlineIndicator.setVisibility(View.VISIBLE);
                    } else {
                        onlineIndicator.setVisibility(View.GONE);
                    }
                } else {
                    onlineIndicator.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onlineIndicator.setVisibility(View.GONE);
            }
        });
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView chatName, lastMessage, timestamp, unreadCount;
        View onlineIndicator;
        ImageView deleteButton;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            chatName = itemView.findViewById(R.id.chatName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            timestamp = itemView.findViewById(R.id.timestamp);
            unreadCount = itemView.findViewById(R.id.unreadCount);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}