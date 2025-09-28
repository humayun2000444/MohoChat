package com.example.mohochat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.mohochat.utils.ImageUtils;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileSetupActivity extends AppCompatActivity {

    EditText phoneInput, aboutInput;
    Button setupCompleteButton;
    CircleImageView profileImage;
    TextView skipText, fullNameDisplay;

    FirebaseAuth auth;
    FirebaseDatabase database;
    Uri imageUri;
    ProgressDialog progressDialog;
    String currentFullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        initViews();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        fullNameDisplay = findViewById(R.id.fullNameDisplay);
        phoneInput = findViewById(R.id.phoneInput);
        aboutInput = findViewById(R.id.aboutInput);
        setupCompleteButton = findViewById(R.id.setupCompleteButton);
        profileImage = findViewById(R.id.profileImage);
        skipText = findViewById(R.id.skipText);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Setting up profile...");
        progressDialog.setCancelable(false);
    }

    private void loadUserData() {
        String userId = auth.getCurrentUser().getUid();
        DatabaseReference userRef = database.getReference().child("user").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Users user = snapshot.getValue(Users.class);
                    if (user != null && user.getFullName() != null) {
                        currentFullName = user.getFullName();
                        fullNameDisplay.setText("Welcome, " + currentFullName + "!");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileSetupActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        profileImage.setOnClickListener(v -> selectImage());

        setupCompleteButton.setOnClickListener(v -> completeSetup());

        skipText.setOnClickListener(v -> skipSetup());
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), 10);
    }

    private void completeSetup() {
        String phone = phoneInput.getText().toString().trim();
        String about = aboutInput.getText().toString().trim();

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
            return;
        }

        if (phone.length() < 10) {
            phoneInput.setError("Enter valid phone number");
            return;
        }

        if (TextUtils.isEmpty(about)) {
            about = "Hey I'm using MohoChat!";
        }

        if (TextUtils.isEmpty(currentFullName)) {
            Toast.makeText(this, "Error: Full name not found. Please restart registration.", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog.show();
        updateUserProfile(currentFullName, phone, about);
    }

    private void updateUserProfile(String fullName, String phone, String about) {
        String userId = auth.getCurrentUser().getUid();
        DatabaseReference userRef = database.getReference().child("user").child(userId);

        if (imageUri != null) {
            // Convert image to Base64 in background thread
            new Thread(() -> {
                String base64Image = ImageUtils.convertImageToBase64(ProfileSetupActivity.this, imageUri);

                runOnUiThread(() -> {
                    if (base64Image != null) {
                        // Check if image size is reasonable (less than 1MB)
                        long sizeKB = ImageUtils.getBase64SizeKB(base64Image);
                        if (sizeKB > 1024) { // 1MB limit
                            Toast.makeText(ProfileSetupActivity.this, "Image too large (" + sizeKB + "KB). Using default image.", Toast.LENGTH_LONG).show();
                            updateUserData(userRef, fullName, phone, about, "default");
                        } else {
                            updateUserData(userRef, fullName, phone, about, base64Image);
                        }
                    } else {
                        Toast.makeText(ProfileSetupActivity.this, "Failed to process image. Using default image.", Toast.LENGTH_SHORT).show();
                        updateUserData(userRef, fullName, phone, about, "default");
                    }
                });
            }).start();
        } else {
            updateUserData(userRef, fullName, phone, about, "default");
        }
    }

    private void updateUserData(DatabaseReference userRef, String fullName, String phone, String about, String profilePicUrl) {
        userRef.child("fullName").setValue(fullName);
        userRef.child("phoneNumber").setValue(phone);
        userRef.child("about").setValue(about);
        userRef.child("profilepic").setValue(profilePicUrl)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileSetupActivity.this, "Profile setup complete!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            Toast.makeText(ProfileSetupActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void skipSetup() {
        navigateToMain();
    }

    private void navigateToMain() {
        Intent intent = new Intent(ProfileSetupActivity.this, MainActivityNew.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 10 && data != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }
}