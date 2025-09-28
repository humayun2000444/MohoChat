package com.example.mohochat.models;

import java.util.HashMap;
import java.util.Map;

public class Chat {
    private String chatId;
    private String lastMessage;
    private long lastMessageTime;
    private long timestamp;
    private Map<String, Object> participants;
    private String chatType; // private, group
    private int unreadCount;
    private String receiverName;
    private String receiverPhone;

    public Chat() {
        this.participants = new HashMap<>();
    }

    public Chat(String chatId, String lastMessage, long lastMessageTime, String chatType) {
        this.chatId = chatId;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.chatType = chatType;
        this.participants = new HashMap<>();
        this.unreadCount = 0;
    }

    // Getters and Setters
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public Map<String, Object> getParticipants() { return participants; }
    public void setParticipants(Map<String, Object> participants) { this.participants = participants; }

    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
}