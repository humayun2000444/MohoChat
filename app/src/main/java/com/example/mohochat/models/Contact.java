package com.example.mohochat.models;

public class Contact {
    private String contactId;
    private String userId;
    private String contactUserId;
    private String contactName;
    private String contactPhone;
    private String contactProfilePic;
    private long addedTime;
    private boolean isBlocked;

    public Contact() {}

    public Contact(String contactId, String userId, String contactUserId, String contactName,
                   String contactPhone, String contactProfilePic, long addedTime) {
        this.contactId = contactId;
        this.userId = userId;
        this.contactUserId = contactUserId;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactProfilePic = contactProfilePic;
        this.addedTime = addedTime;
        this.isBlocked = false;
    }

    // Getters and Setters
    public String getContactId() { return contactId; }
    public void setContactId(String contactId) { this.contactId = contactId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContactUserId() { return contactUserId; }
    public void setContactUserId(String contactUserId) { this.contactUserId = contactUserId; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactProfilePic() { return contactProfilePic; }
    public void setContactProfilePic(String contactProfilePic) { this.contactProfilePic = contactProfilePic; }

    public long getAddedTime() { return addedTime; }
    public void setAddedTime(long addedTime) { this.addedTime = addedTime; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
}