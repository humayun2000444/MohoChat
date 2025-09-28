package com.example.mohochat.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohochat.AddContactActivity;
import com.example.mohochat.R;
import com.example.mohochat.Users;
import com.example.mohochat.adapters.ContactsAdapter;
import com.example.mohochat.models.Contact;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContactsFragment extends Fragment {

    private RecyclerView contactsRecyclerView;
    private ContactsAdapter contactsAdapter;
    private ArrayList<Contact> contactsList;
    private ArrayList<Contact> filteredContactsList;
    private TextView noContactsText;
    private EditText searchEditText;
    private FloatingActionButton fabAddContact;

    private DatabaseReference database;
    private FirebaseAuth auth;

    private static final int CONTACTS_PERMISSION_REQUEST = 100;
    private static final int WRITE_CONTACTS_PERMISSION_REQUEST = 101;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupFab();
        setupSearch();
        checkPermissionsAndLoadContacts();
    }

    private void initViews(View view) {
        contactsRecyclerView = view.findViewById(R.id.contactsRecyclerView);
        noContactsText = view.findViewById(R.id.noContactsText);
        searchEditText = view.findViewById(R.id.searchEditText);
        fabAddContact = view.findViewById(R.id.fabAddContact);

        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        contactsList = new ArrayList<>();
        filteredContactsList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        contactsAdapter = new ContactsAdapter(getContext(), filteredContactsList, this);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactsRecyclerView.setAdapter(contactsAdapter);
    }

    private void setupFab() {
        fabAddContact.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS}, WRITE_CONTACTS_PERMISSION_REQUEST);
            } else {
                Intent intent = new Intent(getContext(), AddContactActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void checkPermissionsAndLoadContacts() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_REQUEST);
        } else {
            loadContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CONTACTS_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                showNoContactsMessage("Contacts permission required\nGrant permission to see your contacts");
            }
        } else if (requestCode == WRITE_CONTACTS_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(getContext(), AddContactActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Write contacts permission required to add contacts", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadContacts() {
        // First load all phone contacts
        List<Contact> phoneContacts = loadPhoneContacts();

        // Then check which ones have the app
        checkAppUsers(phoneContacts);
    }

    private List<Contact> loadPhoneContacts() {
        List<Contact> contacts = new ArrayList<>();
        Set<String> seenNumbers = new HashSet<>();

        if (getContext() == null) return contacts;

        Cursor cursor = getContext().getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            },
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String phone = cursor.getString(1);

                if (name != null && phone != null && !name.trim().isEmpty() && !phone.trim().isEmpty()) {
                    // Clean phone number
                    String cleanPhone = phone.replaceAll("[^\\d+]", "");

                    // Avoid duplicates
                    if (!seenNumbers.contains(cleanPhone)) {
                        seenNumbers.add(cleanPhone);
                        Contact newContact = new Contact(name.trim(), phone.trim());
                        contacts.add(newContact);

                        // Debug log for first few contacts
                        if (contacts.size() <= 5) {
                            android.util.Log.d("ContactsFragment", "Phone contact loaded: " +
                                name.trim() + " - " + phone.trim());
                        }
                    }
                }
            }
            cursor.close();
        }

        return contacts;
    }

    private void checkAppUsers(List<Contact> phoneContacts) {
        if (phoneContacts.isEmpty()) {
            showNoContactsMessage("No contacts found\nAdd contacts to your phone first");
            return;
        }

        database.child("user").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Create map of phone numbers to app users
                Map<String, Users> appUsers = new HashMap<>();
                String currentUserPhone = null;
                String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Users user = userSnapshot.getValue(Users.class);
                    if (user != null && user.getPhoneNumber() != null) {
                        String userPhone = normalizePhone(user.getPhoneNumber());

                        if (user.getUserId().equals(currentUserId)) {
                            currentUserPhone = userPhone;
                        } else {
                            appUsers.put(userPhone, user);
                        }
                    }
                }

                // Process contacts
                List<Contact> finalContacts = new ArrayList<>();
                for (Contact contact : phoneContacts) {
                    String contactPhone = normalizePhone(contact.getPhoneNumber());

                    // Skip current user's own number
                    if (currentUserPhone != null && phoneMatches(currentUserPhone, contactPhone)) {
                        continue;
                    }

                    // Check if contact has app
                    Users appUser = findAppUser(appUsers, contactPhone);
                    if (appUser != null) {
                        // Contact has app - prefer phone contact name, fallback to Firebase name
                        String displayName = contact.getName(); // Use phone contact name first

                        // If phone contact name is empty or generic, use Firebase name
                        if (displayName == null || displayName.trim().isEmpty() ||
                            displayName.matches("^[0-9+\\-()\\s]+$")) { // If name is just numbers/symbols
                            String firebaseName = appUser.getFullName() != null && !appUser.getFullName().isEmpty()
                                ? appUser.getFullName() : appUser.getUserName();
                            if (firebaseName != null && !firebaseName.trim().isEmpty()) {
                                displayName = firebaseName;
                            }
                        }

                        Contact appContact = new Contact(displayName, contact.getPhoneNumber(), true,
                            appUser.getUserId(), appUser.getProfilepic(), appUser.getStatus());
                        finalContacts.add(appContact);

                        // Debug log for app user
                        android.util.Log.d("ContactsFragment", "App user added: " + displayName +
                            " (" + contact.getPhoneNumber() + ") - Original contact name: " + contact.getName());
                    } else {
                        // Regular contact
                        Contact regularContact = new Contact(contact.getName(), contact.getPhoneNumber(),
                            false, null, null, null);
                        finalContacts.add(regularContact);
                    }
                }

                // Sort: app users first, then alphabetically
                Collections.sort(finalContacts, (c1, c2) -> {
                    if (c1.isHasApp() != c2.isHasApp()) {
                        return c1.isHasApp() ? -1 : 1;
                    }
                    return c1.getName().compareToIgnoreCase(c2.getName());
                });

                // Update UI
                contactsList.clear();
                contactsList.addAll(finalContacts);
                filterContacts("");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading contacts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showNoContactsMessage("Error loading contacts\nTap to retry");
                noContactsText.setOnClickListener(v -> loadContacts());
            }
        });
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^\\d]", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    private boolean phoneMatches(String phone1, String phone2) {
        if (phone1 == null || phone2 == null) return false;

        String norm1 = normalizePhone(phone1);
        String norm2 = normalizePhone(phone2);

        return norm1.equals(norm2) ||
               (norm1.length() >= 10 && norm2.length() >= 10 &&
                norm1.substring(norm1.length() - 10).equals(norm2.substring(norm2.length() - 10)));
    }

    private Users findAppUser(Map<String, Users> appUsers, String contactPhone) {
        // Direct match
        if (appUsers.containsKey(contactPhone)) {
            return appUsers.get(contactPhone);
        }

        // Try fuzzy matching
        for (String appPhone : appUsers.keySet()) {
            if (phoneMatches(appPhone, contactPhone)) {
                return appUsers.get(appPhone);
            }
        }

        return null;
    }

    private void filterContacts(String query) {
        filteredContactsList.clear();

        if (query.isEmpty()) {
            filteredContactsList.addAll(contactsList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Contact contact : contactsList) {
                if (contact.getName().toLowerCase().contains(lowerQuery) ||
                    contact.getPhoneNumber().contains(query)) {
                    filteredContactsList.add(contact);
                }
            }
        }

        contactsAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredContactsList.isEmpty()) {
            noContactsText.setVisibility(View.VISIBLE);
            if (contactsList.isEmpty()) {
                noContactsText.setText("No contacts found\nAdd contacts to your phone first");
            } else {
                noContactsText.setText("No contacts match your search");
            }
        } else {
            noContactsText.setVisibility(View.GONE);
        }
    }

    private void showNoContactsMessage(String message) {
        if (noContactsText != null) {
            noContactsText.setVisibility(View.VISIBLE);
            noContactsText.setText(message);
        }
    }
}