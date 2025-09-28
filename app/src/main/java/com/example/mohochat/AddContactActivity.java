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

    private EditText contactNameInput, contactPhoneInput;
    private Button addContactButton;
    private ImageView backButton;

    private ContactManager contactManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        contactNameInput = findViewById(R.id.contactNameInput);
        contactPhoneInput = findViewById(R.id.contactPhoneInput);
        addContactButton = findViewById(R.id.addContactButton);
        backButton = findViewById(R.id.backButton);

        contactManager = new ContactManager(this);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        addContactButton.setOnClickListener(v -> addContact());
    }


    private void addContact() {
        String name = contactNameInput.getText().toString().trim();
        String phone = contactPhoneInput.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please enter both name and phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add contact to phone contacts using ContactManager
        boolean success = contactManager.addContactToPhone(name, phone);

        if (success) {
            Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to add contact", Toast.LENGTH_SHORT).show();
        }
    }
}