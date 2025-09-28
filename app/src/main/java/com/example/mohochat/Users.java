package com.example.mohochat;

public class Users {
    String profilepic, mail, userName, password, userId, lastMessage, status;
    String phoneNumber;
    String fullName;
    boolean isOnline;
    long lastSeen;
    String about;

    public Users(){}

    public Users(String id, String userName, String email, String phone, String password, String imageUri, String status) {
        this.userId = id;
        this.userName = userName;
        this.mail = email;
        this.phoneNumber = phone;
        this.password= password;
        this.profilepic = imageUri;
        this.status = status;
        this.isOnline = false;
        this.lastSeen = System.currentTimeMillis();
        this.about = "Hey there! I am using MohoChat.";
    }

    public String getProfilepic() { return profilepic;}
    public void setProfilepic(String profilepic) {
        this.profilepic = profilepic;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
