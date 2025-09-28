package com.example.mohochat;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import androidx.core.app.ActivityCompat;

import com.example.mohochat.models.Contact;
import com.example.mohochat.models.SMSInvite;
import com.example.mohochat.models.ChatFromSMS;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ContactManager {
    private static final String TAG = "ContactManager";
    private Context context;
    private DatabaseReference usersRef;
    private ContactSyncListener listener;

    public interface ContactSyncListener {
        void onContactsLoaded(List<Contact> contacts);
        void onError(String error);
    }

    public ContactManager(Context context) {
        this.context = context;
        this.usersRef = FirebaseDatabase.getInstance().getReference().child("user");
    }

    public void setContactSyncListener(ContactSyncListener listener) {
        this.listener = listener;
    }

    public void syncContacts() {
        Log.d(TAG, "Starting contact sync...");

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Contacts permission not granted");
            if (listener != null) {
                listener.onError("Contacts permission not granted");
            }
            return;
        }

        List<Contact> phoneContacts = getPhoneContacts();
        Log.d(TAG, "Found " + phoneContacts.size() + " phone contacts");
        checkWhichContactsHaveApp(phoneContacts);
    }

    private List<Contact> getPhoneContacts() {
        Map<String, Contact> uniqueContacts = new HashMap<>(); // Phone -> Best Contact
        Log.d(TAG, "Reading phone contacts from device...");

        Cursor cursor = context.getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
            },
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            Log.d(TAG, "Phone cursor returned " + cursor.getCount() + " rows");

            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String phoneNumber = cursor.getString(1);
                int phoneType = cursor.getInt(2);

                if (name != null && phoneNumber != null && !name.trim().isEmpty() && !phoneNumber.trim().isEmpty()) {
                    // Clean and normalize phone number
                    String normalizedPhone = normalizePhoneNumber(phoneNumber);

                    if (normalizedPhone.length() >= 10) {
                        Contact newContact = new Contact(name.trim(), phoneNumber);

                        // Check if we already have this phone number
                        if (uniqueContacts.containsKey(normalizedPhone)) {
                            Contact existingContact = uniqueContacts.get(normalizedPhone);

                            // Prefer mobile numbers and better names
                            boolean shouldReplace = false;

                            // Prefer mobile over other types
                            if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) {
                                shouldReplace = true;
                            }

                            // Prefer names that don't start with numbers/symbols
                            if (!name.matches("^[0-9+\\-()\\s].*") && existingContact.getName().matches("^[0-9+\\-()\\s].*")) {
                                shouldReplace = true;
                            }

                            // Prefer longer, more descriptive names
                            if (name.length() > existingContact.getName().length() + 3) {
                                shouldReplace = true;
                            }

                            if (shouldReplace) {
                                uniqueContacts.put(normalizedPhone, newContact);
                                Log.d(TAG, "Replaced contact: " + existingContact.getName() + " -> " + name + " (" + phoneNumber + ")");
                            } else {
                                Log.d(TAG, "Kept existing contact: " + existingContact.getName() + " over " + name);
                            }
                        } else {
                            uniqueContacts.put(normalizedPhone, newContact);
                            Log.d(TAG, "Added contact: " + name + " - " + phoneNumber);
                        }
                    } else {
                        Log.d(TAG, "Skipped invalid phone: " + name + " - " + phoneNumber);
                    }
                } else {
                    Log.d(TAG, "Skipped contact with null/empty name or phone");
                }
            }
            cursor.close();
        } else {
            Log.w(TAG, "Failed to get phone contacts cursor");
        }

        List<Contact> contacts = new ArrayList<>(uniqueContacts.values());
        Log.d(TAG, "Collected " + contacts.size() + " unique phone contacts (after deduplication)");
        return contacts;
    }

    private void checkWhichContactsHaveApp(List<Contact> phoneContacts) {
        Log.d(TAG, "Checking which contacts have the app...");

        // Get current user info to exclude from contacts
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        Log.d(TAG, "Current user ID: " + currentUserId);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Firebase users data retrieved. User count: " + dataSnapshot.getChildrenCount());

                // Create a map of phone numbers to app users for efficient lookup
                Map<String, Users> appUsersByPhone = new HashMap<>();
                String currentUserPhone = null;

                // Build the phone-to-user mapping
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    Users user = userSnapshot.getValue(Users.class);
                    if (user != null && user.getPhoneNumber() != null) {
                        String normalizedPhone = normalizePhoneNumber(user.getPhoneNumber());

                        // Track current user's phone
                        if (user.getUserId().equals(currentUserId)) {
                            currentUserPhone = normalizedPhone;
                            Log.d(TAG, "Current user phone: " + currentUserPhone);
                        } else {
                            // Add to app users map (excluding current user)
                            appUsersByPhone.put(normalizedPhone, user);
                            Log.d(TAG, "Added app user: " + user.getUserName() + " with phone: " + normalizedPhone);
                        }
                    }
                }

                // Process contacts and check against app users
                List<Contact> finalContacts = new ArrayList<>();
                Set<String> processedPhones = new HashSet<>(); // To prevent duplicates

                for (Contact contact : phoneContacts) {
                    String normalizedContactPhone = normalizePhoneNumber(contact.getPhoneNumber());

                    Log.d(TAG, "Processing contact: " + contact.getName() + " - " + contact.getPhoneNumber() + " -> " + normalizedContactPhone);

                    // Skip current user's own contact
                    if (currentUserPhone != null && phoneNumbersMatch(currentUserPhone, normalizedContactPhone)) {
                        Log.d(TAG, "Skipping current user's own contact");
                        continue;
                    }

                    // Skip duplicates (same normalized phone number)
                    if (processedPhones.contains(normalizedContactPhone)) {
                        Log.d(TAG, "Skipping duplicate contact: " + contact.getName());
                        continue;
                    }
                    processedPhones.add(normalizedContactPhone);

                    // Check if this contact has the app
                    Users appUser = findMatchingAppUser(appUsersByPhone, normalizedContactPhone);

                    if (appUser != null) {
                        // Contact has the app - use app user's data
                        String appUserDisplayName = appUser.getFullName() != null && !appUser.getFullName().isEmpty()
                            ? appUser.getFullName() : appUser.getUserName();

                        Log.d(TAG, "Found app user: " + appUserDisplayName + " for contact: " + contact.getName());

                        Contact appContact = new Contact(
                            appUserDisplayName,  // Use app user's name
                            contact.getPhoneNumber(),  // Keep original phone format
                            true,  // hasApp
                            appUser.getUserId(),
                            appUser.getProfilepic(),
                            appUser.getStatus()
                        );
                        finalContacts.add(appContact);
                    } else {
                        // Contact doesn't have the app - use contact's data
                        Log.d(TAG, "Contact doesn't have app: " + contact.getName());

                        Contact regularContact = new Contact(
                            contact.getName(),  // Use phone contact's name
                            contact.getPhoneNumber(),
                            false,  // hasApp
                            null,
                            null,
                            null
                        );
                        finalContacts.add(regularContact);
                    }
                }

                // Sort contacts: app users first, then others, alphabetically within each group
                Collections.sort(finalContacts, new Comparator<Contact>() {
                    @Override
                    public int compare(Contact c1, Contact c2) {
                        // First sort by hasApp (app users first)
                        if (c1.isHasApp() != c2.isHasApp()) {
                            return c1.isHasApp() ? -1 : 1;
                        }
                        // Then sort alphabetically by name
                        return c1.getName().compareToIgnoreCase(c2.getName());
                    }
                });

                Log.d(TAG, "Final contact list: " + finalContacts.size() + " contacts (after deduplication)");
                for (Contact c : finalContacts) {
                    Log.d(TAG, "- " + c.getName() + " (" + c.getPhoneNumber() + ") hasApp: " + c.isHasApp());
                }

                if (listener != null) {
                    listener.onContactsLoaded(finalContacts);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Firebase error: " + databaseError.getMessage());
                if (listener != null) {
                    listener.onError("Failed to sync contacts: " + databaseError.getMessage());
                }
            }
        });
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";

        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");

        // Return last 11 digits for consistency (handles international codes)
        if (digitsOnly.length() > 11) {
            return digitsOnly.substring(digitsOnly.length() - 11);
        } else if (digitsOnly.length() >= 10) {
            return digitsOnly;
        }

        return digitsOnly;
    }

    private Users findMatchingAppUser(Map<String, Users> appUsersByPhone, String normalizedContactPhone) {
        // Direct match
        if (appUsersByPhone.containsKey(normalizedContactPhone)) {
            return appUsersByPhone.get(normalizedContactPhone);
        }

        // Try different variations for matching
        for (String appUserPhone : appUsersByPhone.keySet()) {
            if (phoneNumbersMatch(appUserPhone, normalizedContactPhone)) {
                return appUsersByPhone.get(appUserPhone);
            }
        }

        return null;
    }

    private boolean phoneNumbersMatch(String phone1, String phone2) {
        // Remove all non-digit characters
        phone1 = phone1.replaceAll("[^\\d]", "");
        phone2 = phone2.replaceAll("[^\\d]", "");

        // Check if they're exactly the same
        if (phone1.equals(phone2)) return true;

        // Check last 11 digits for Bangladesh numbers (like 8801789896378 vs +8801789896378 vs 01789896378)
        if (phone1.length() >= 11 && phone2.length() >= 11) {
            String phone1Last11 = phone1.substring(phone1.length() - 11);
            String phone2Last11 = phone2.substring(phone2.length() - 11);
            if (phone1Last11.equals(phone2Last11)) return true;
        }

        // Also check last 10 digits for shorter numbers
        if (phone1.length() >= 10 && phone2.length() >= 10) {
            String phone1Last10 = phone1.substring(phone1.length() - 10);
            String phone2Last10 = phone2.substring(phone2.length() - 10);
            if (phone1Last10.equals(phone2Last10)) return true;
        }

        // Special case for Bangladesh: remove leading 0 and compare
        // 01789896378 should match 8801789896378
        String phone1WithoutLeadingZero = phone1.startsWith("0") ? "88" + phone1 : phone1;
        String phone2WithoutLeadingZero = phone2.startsWith("0") ? "88" + phone2 : phone2;

        if (phone1WithoutLeadingZero.equals(phone2WithoutLeadingZero)) return true;

        // Compare last 11 digits after processing
        if (phone1WithoutLeadingZero.length() >= 11 && phone2WithoutLeadingZero.length() >= 11) {
            String phone1Last11 = phone1WithoutLeadingZero.substring(phone1WithoutLeadingZero.length() - 11);
            String phone2Last11 = phone2WithoutLeadingZero.substring(phone2WithoutLeadingZero.length() - 11);
            return phone1Last11.equals(phone2Last11);
        }

        return false;
    }

    public void sendSMS(String phoneNumber, String message, String contactName) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "SMS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            // Store SMS in Firebase for tracking
            storeSMSInFirebase(phoneNumber, message, contactName);

            Toast.makeText(context, "SMS sent successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void storeSMSInFirebase(String phoneNumber, String message, String contactName) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String senderId = auth.getCurrentUser().getUid();
        DatabaseReference smsRef = FirebaseDatabase.getInstance().getReference().child("sms_invites");

        String smsId = smsRef.push().getKey();

        SMSInvite smsInvite = new SMSInvite();
        smsInvite.setSmsId(smsId);
        smsInvite.setSenderId(senderId);
        smsInvite.setReceiverPhone(phoneNumber);
        smsInvite.setMessage(message);
        smsInvite.setTimestamp(System.currentTimeMillis());
        smsInvite.setStatus("sent");

        smsRef.child(smsId).setValue(smsInvite);

        // Also create a chat entry for the sender
        createChatFromSMS(senderId, phoneNumber, message, contactName);
    }

    private void createChatFromSMS(String senderId, String receiverPhone, String message, String contactName) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference().child("chats");
        String chatId = chatsRef.push().getKey();

        ChatFromSMS chat = new ChatFromSMS();
        chat.setChatId(chatId);
        chat.setSenderId(senderId);
        chat.setReceiverPhone(receiverPhone);
        chat.setContactName(contactName);
        chat.setLastMessage("Invitation sent via SMS");
        chat.setTimestamp(System.currentTimeMillis());
        chat.setType("sms_invite");

        chatsRef.child(senderId).child(chatId).setValue(chat);
    }

    public boolean addContactToPhone(String name, String phoneNumber) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Write contacts permission not granted", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build());

            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            ContentResolver contentResolver = context.getContentResolver();
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
            return true;
        } catch (Exception e) {
            Toast.makeText(context, "Failed to add contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}