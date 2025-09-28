package com.example.mohochat.models;

public class ChatFromSMS {
    private String chatId;
    private String senderId;
    private String receiverPhone;
    private String lastMessage;
    private long timestamp;
    private String type;
    private String contactName;

    public ChatFromSMS() {}

    public ChatFromSMS(String chatId, String senderId, String receiverPhone, String lastMessage, long timestamp, String type) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverPhone = receiverPhone;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
}