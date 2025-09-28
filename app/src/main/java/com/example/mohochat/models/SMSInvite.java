package com.example.mohochat.models;

public class SMSInvite {
    private String smsId;
    private String senderId;
    private String receiverPhone;
    private String message;
    private long timestamp;
    private String status;

    public SMSInvite() {}

    public SMSInvite(String smsId, String senderId, String receiverPhone, String message, long timestamp, String status) {
        this.smsId = smsId;
        this.senderId = senderId;
        this.receiverPhone = receiverPhone;
        this.message = message;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getSmsId() { return smsId; }
    public void setSmsId(String smsId) { this.smsId = smsId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}