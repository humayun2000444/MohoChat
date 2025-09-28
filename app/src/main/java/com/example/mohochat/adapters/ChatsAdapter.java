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
            updateLastMessageWithStatus(holder, chat.getChatId(), chat.getLastMessage());
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
        if (chat.getReceiverPhone() != null && !chat.getReceiverPhone().isEmpty()) {
            // SMS invite
            deleteMessage.setText("Are you sure you want to delete the SMS invite for " + chatName + "?\n\nThis invitation will be permanently removed.");
        } else {
            // Regular chat
            deleteMessage.setText("Are you sure you want to delete the chat with " + chatName + "?\n\nThis will only remove the chat from your side. " + chatName + " will still be able to see the conversation.");
        }

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
            // Regular chat - only delete from current user's side (WhatsApp-style)
            String receiverId = chat.getChatId(); // chatId is actually receiverId

            // Only remove chat from current user's chat list
            // Messages and other user's chat list remain intact
            database.child("chats").child(currentUserId).child(receiverId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Remove from UI
                    chatsList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, chatsList.size());

                    Toast.makeText(context, "Chat deleted from your side", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }


    private void checkUserOnlineStatus(View onlineIndicator, String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("user").child(userId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Users user = snapshot.getValue(Users.class);
                    if (user != null && user.isOnline()) {
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

    private void updateLastMessageWithStatus(ChatViewHolder holder, String userId, String lastMessage) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("user").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Users user = snapshot.getValue(Users.class);
                    if (user != null && !user.isOnline() && user.getLastSeen() > 0) {
                        long currentTime = System.currentTimeMillis();
                        long timeDiff = currentTime - user.getLastSeen();
                        String lastSeenText = formatLastSeen(timeDiff);

                        // Show last seen instead of last message if user is offline
                        holder.lastMessage.setText(lastSeenText);
                        holder.lastMessage.setTextColor(context.getResources().getColor(R.color.secondary_text));
                    } else {
                        // Show normal last message for online users
                        holder.lastMessage.setText(lastMessage);
                        holder.lastMessage.setTextColor(context.getResources().getColor(R.color.secondary_text));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Fallback to showing last message
                holder.lastMessage.setText(lastMessage);
            }
        });
    }

    private String formatLastSeen(long timeDiffMillis) {
        long seconds = timeDiffMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (minutes < 1) {
            return "Last seen just now";
        } else if (minutes < 60) {
            return "Last seen " + minutes + "m ago";
        } else if (hours < 24) {
            return "Last seen " + hours + "h ago";
        } else if (days < 7) {
            return "Last seen " + days + "d ago";
        } else {
            return "Last seen long ago";
        }
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