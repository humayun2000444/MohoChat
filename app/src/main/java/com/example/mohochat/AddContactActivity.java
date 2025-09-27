package com.example.mohochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.mohochat.models.Contact;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AddContactActivity extends AppCompatActivity {

    private EditText emailOrPhoneInput;
    private Button searchButton, addContactButton;
    private ImageView backButton;

    private DatabaseReference database;
    private FirebaseAuth auth;
    private Users foundUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        emailOrPhoneInput = findViewById(R.id.emailOrPhoneInput);
        searchButton = findViewById(R.id.searchButton);
        addContactButton = findViewById(R.id.addContactButton);
        backButton = findViewById(R.id.backButton);

        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        addContactButton.setEnabled(false);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        searchButton.setOnClickListener(v -> searchUser());

        addContactButton.setOnClickListener(v -> addContact());
    }

    private void searchUser() {
        String searchQuery = emailOrPhoneInput.getText().toString().trim();
        if (searchQuery.isEmpty()) {
            Toast.makeText(this, "Please enter email or phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search by email first
        database.child("user").orderByChild("mail").equalTo(searchQuery)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                foundUser = userSnapshot.getValue(Users.class);
                                if (foundUser != null && !foundUser.getUserId().equals(auth.getCurrentUser().getUid())) {
                                    showUserFound();
                                    return;
                                }
                            }
                            searchByPhone(searchQuery);
                        } else {
                            searchByPhone(searchQuery);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddContactActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void searchByPhone(String phoneNumber) {
        database.child("user").orderByChild("phoneNumber").equalTo(phoneNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                foundUser = userSnapshot.getValue(Users.class);
                                if (foundUser != null && !foundUser.getUserId().equals(auth.getCurrentUser().getUid())) {
                                    showUserFound();
                                    return;
                                }
                            }
                        }
                        Toast.makeText(AddContactActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                        foundUser = null;
                        addContactButton.setEnabled(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddContactActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showUserFound() {
        Toast.makeText(this, "User found: " + foundUser.getUserName(), Toast.LENGTH_SHORT).show();
        addContactButton.setEnabled(true);

        // Check if already a contact
        String currentUserId = auth.getCurrentUser().getUid();
        database.child("contacts").child(currentUserId).child(foundUser.getUserId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            addContactButton.setText("Already a Contact");
                            addContactButton.setEnabled(false);
                        } else {
                            addContactButton.setText("Add Contact");
                            addContactButton.setEnabled(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void addContact() {
        if (foundUser == null) {
            Toast.makeText(this, "No user selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();
        String contactId = database.child("contacts").child(currentUserId).push().getKey();

        Contact contact = new Contact(
                contactId,
                currentUserId,
                foundUser.getUserId(),
                foundUser.getUserName(),
                foundUser.getPhoneNumber(),
                foundUser.getProfilepic(),
                System.currentTimeMillis()
        );

        database.child("contacts").child(currentUserId).child(foundUser.getUserId())
                .setValue(contact)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddContactActivity.this, "Contact added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddContactActivity.this, "Failed to add contact", Toast.LENGTH_SHORT).show();
                });
    }
}