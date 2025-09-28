# MohoChat 📱💬

A modern, feature-rich Android real-time messaging application built with Firebase backend and Material Design components.

## 🌟 Features

### 📞 **Contact Management**
- **Smart Contact Sync** - Automatically syncs with phone contacts
- **App User Detection** - Identifies which contacts have MohoChat installed
- **SMS Invites** - Send app invitations to contacts via SMS
- **Contact Search** - Real-time search through contact list
- **Letter Avatars** - Automatic colorful letter avatars with consistent colors
- **Profile Pictures** - Support for custom profile images with fallback

### 💬 **Real-time Messaging**
- **Instant Messaging** - Real-time chat with Firebase Realtime Database
- **Message Status** - Sent/delivered indicators
- **Online Status** - See when contacts are online
- **Message History** - Persistent chat history
- **Chat Deletion** - Complete chat cleanup (messages + metadata)
- **Text Selection** - Copy messages with long press

### 👤 **User Profile**
- **Profile Setup** - Complete profile management with name, about, email, phone
- **Profile Pictures** - Upload and manage profile images
- **Modern UI** - Collapsing toolbar design with Material Design
- **Edit Profile** - Smooth profile editing with validation

### 🎨 **Modern UI/UX**
- **Material Design 3** - Latest Material Design components
- **Consistent Theme** - Custom color palette with primary accent (#FF9F68)
- **Card-based Layout** - Modern card designs throughout
- **Smooth Animations** - Transition animations and scrolling effects
- **Dark/Light Theme** - Theme-aware components
- **Responsive Design** - Optimized for different screen sizes

### 🔧 **Technical Features**
- **Firebase Authentication** - Secure user authentication
- **Firebase Realtime Database** - Real-time data synchronization
- **Firebase Storage** - Image storage and management
- **Contact Permissions** - Proper permission handling
- **SMS Integration** - Send invitation messages
- **Image Processing** - Base64 image encoding/decoding
- **Offline Support** - Local data caching

## 🏗️ Architecture

### **Backend**
- **Firebase Authentication** - User management and security
- **Firebase Realtime Database** - Real-time data sync
- **Firebase Storage** - File and image storage
- **Firebase Cloud Messaging** - Push notifications

### **Frontend**
- **Native Android** - Java-based Android application
- **Material Design Components** - Modern UI framework
- **RecyclerView** - Efficient list management
- **Fragments** - Modular UI components
- **ViewPager** - Tab-based navigation

### **Database Structure**
```
firebase_root/
├── user/
│   ├── {userId}/
│   │   ├── userName, fullName, email, phoneNumber
│   │   ├── profilepic, about, status
│   │   └── fcmToken, online status
├── messages/
│   └── {chatId}/
│       └── {messageId}/
│           ├── senderId, receiverId, messageText
│           ├── messageType, timestamp
│           └── chatId
├── chats/
│   └── {userId}/
│       └── {receiverId}/
│           ├── lastMessage, lastMessageTime
│           ├── receiverName, chatId
│           └── unreadCount
├── notifications/
│   └── {notificationId}/
│       ├── targetToken, title, body
│       ├── senderId, senderName, chatId
│       └── timestamp, processed
└── sms_invites/
    └── {inviteId}/
        ├── senderId, receiverPhone
        ├── message, timestamp
        └── status
```

## 🚀 Recent Updates

### **v2.0 - Major UI Overhaul**
- ✅ **Redesigned Chat List** - Modern card-based design with proper text wrapping
- ✅ **Enhanced Conversation UI** - New header design with theme colors
- ✅ **Modern Message Bubbles** - Rounded cards with status indicators
- ✅ **Profile Page Redesign** - Collapsing toolbar with Material Design
- ✅ **Contact Management** - Improved contact sync and display
- ✅ **Chat Deletion** - Complete Firebase cleanup on chat deletion
- ✅ **Theme Consistency** - Unified color scheme throughout app

### **v1.5 - Contact System Enhancement**
- ✅ **Smart Contact Detection** - Better app user identification
- ✅ **SMS Invite System** - Send app invitations via SMS
- ✅ **Duplicate Prevention** - Advanced contact deduplication
- ✅ **Letter Avatars** - Consistent color generation based on names

## 📱 Screenshots

### Chat List
- Modern card-based design
- Online status indicators
- Unread message counts
- Long press for chat deletion

### Conversation View
- Real-time messaging
- Message status indicators
- Profile picture integration
- Modern input design

### Profile Management
- Collapsing toolbar design
- Profile picture upload
- Complete profile editing
- Material Design components

## 🛠️ Installation & Setup

### **Prerequisites**
- Android Studio Arctic Fox or later
- Android SDK 21+ (Android 5.0)
- Firebase project setup
- Google Services JSON file

### **Setup Steps**

1. **Clone Repository**
   ```bash
   git clone https://github.com/yourusername/MohoChat.git
   cd MohoChat
   ```

2. **Firebase Configuration**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Enable Authentication, Realtime Database, and Storage
   - Download `google-services.json` and place in `app/` directory
   - Configure Authentication providers (Phone, Email)

3. **Database Rules**
   ```json
   {
     "rules": {
       "user": {
         "$uid": {
           ".read": "auth != null",
           ".write": "$uid === auth.uid"
         }
       },
       "messages": {
         ".read": "auth != null",
         ".write": "auth != null"
       },
       "chats": {
         "$uid": {
           ".read": "$uid === auth.uid",
           ".write": "$uid === auth.uid"
         }
       }
     }
   }
   ```

4. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```

## 🎯 Key Components

### **Core Activities**
- `MainActivity` - Tab-based navigation (Chats, Contacts, Profile)
- `ChatActivity` - Individual conversation interface
- `LoginActivity` - Authentication flow
- `ProfileSetupActivity` - Initial profile configuration

### **Fragments**
- `ChatsFragment` - Chat list management
- `ContactsFragment` - Contact list and SMS invites
- `ProfileFragment` - User profile management

### **Adapters**
- `ChatsAdapter` - Chat list with delete functionality
- `ContactsAdapter` - Contact list with app detection
- `MessagesAdapter` - Message bubbles with status

### **Models**
- `Users` - User profile data
- `Chat` - Chat metadata
- `Message` - Individual message data
- `Contact` - Contact information

### **Utilities**
- `ImageUtils` - Image processing and Base64 conversion
- `ProfileImageLoader` - Profile image loading with fallbacks
- `ContactManager` - Contact sync and SMS functionality

## 🔐 Permissions

### **Required Permissions**
- `READ_CONTACTS` - Contact list access
- `WRITE_CONTACTS` - Adding new contacts
- `SEND_SMS` - SMS invite functionality
- `INTERNET` - Network connectivity
- `ACCESS_NETWORK_STATE` - Network status
- `CAMERA` - Profile picture capture
- `READ_EXTERNAL_STORAGE` - Image selection

## 🎨 Theme & Design

### **Color Palette**
- **Primary Accent**: `#FF9F68` (Orange)
- **Secondary Accent**: `#7F68FF` (Purple)
- **Success Green**: `#68FF9F`
- **Warning Red**: `#FF687F`
- **Online Green**: `#4CAF50`

### **Design Principles**
- Material Design 3 guidelines
- Consistent spacing and typography
- Rounded corners and subtle shadows
- Theme-aware components
- Accessibility considerations

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Your Name** - *Initial work* - [YourGitHub](https://github.com/yourusername)

## 🙏 Acknowledgments

- Firebase team for excellent backend services
- Material Design team for UI/UX guidelines
- Android development community for inspiration and support

---

**MohoChat** - Bringing people together through modern, real-time communication 💬✨
