package com.example.mohochat.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mohochat.R;
import com.example.mohochat.Users;
import com.example.mohochat.login;
import com.example.mohochat.utils.ImageUtils;
import com.example.mohochat.utils.ProfileImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private CircleImageView profileImage;
    private TextView userName, userEmail, userAbout, userPhone;
    private TextView editProfile;

    private DatabaseReference database;
    private FirebaseAuth auth;
    private Users currentUser;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        loadUserProfile();
        setupClickListeners();
    }

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);
        userAbout = view.findViewById(R.id.userAbout);
        userPhone = view.findViewById(R.id.userPhone);
        editProfile = view.findViewById(R.id.editProfile);

        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
    }

    private void setupClickListeners() {
        profileImage.setOnClickListener(v -> selectImage());

        editProfile.setOnClickListener(v -> showEditProfileDialog());
    }

    private void loadUserProfile() {
        if (auth.getCurrentUser() == null) {
            // User not authenticated, redirect to login
            Intent intent = new Intent(getContext(), login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();
        database.child("user").child(currentUserId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(Users.class);
                if (currentUser != null) {
                    updateUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                        // Permission denied - user might need to re-authenticate
                        Toast.makeText(getContext(), "Authentication expired. Please login again.", Toast.LENGTH_LONG).show();
                        auth.signOut();
                        Intent intent = new Intent(getContext(), login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getContext(), "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void updateUI() {
        // Show fullName if available, otherwise fall back to userName
        String displayName = currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()
            ? currentUser.getFullName() : currentUser.getUserName();
        userName.setText(displayName);
        userEmail.setText(currentUser.getMail());
        userAbout.setText(currentUser.getAbout());
        userPhone.setText(currentUser.getPhoneNumber());

        // Use letter avatar fallback for profile images
        ProfileImageLoader.loadProfileImageWithLetterFallback(
            getContext(),
            profileImage,
            currentUser.getProfilepic(),
            displayName
        );
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
            && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadProfileImage(imageUri);
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        try {
            String base64Image = ImageUtils.convertImageToBase64(getContext(), imageUri);
            if (base64Image != null) {
                String currentUserId = auth.getCurrentUser().getUid();
                database.child("user").child(currentUserId).child("profilepic").setValue(base64Image)
                        .addOnSuccessListener(aVoid -> {
                            // Load the new profile image
                            String displayName = currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()
                                ? currentUser.getFullName() : currentUser.getUserName();
                            ProfileImageLoader.loadProfileImageWithLetterFallback(
                                getContext(),
                                profileImage,
                                base64Image,
                                displayName
                            );
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Failed to update profile picture", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showEditProfileDialog() {
        if (currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);

        com.google.android.material.textfield.TextInputEditText editName = dialogView.findViewById(R.id.editName);
        com.google.android.material.textfield.TextInputEditText editAbout = dialogView.findViewById(R.id.editAbout);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Set current values
        String displayName = currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()
            ? currentUser.getFullName() : currentUser.getUserName();
        editName.setText(displayName);
        editAbout.setText(currentUser.getAbout() != null ? currentUser.getAbout() : "");

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newAbout = editAbout.getText().toString().trim();

            if (!newName.isEmpty()) {
                updateProfile(newName, newAbout);
                dialog.dismiss();
            } else {
                editName.setError("Name cannot be empty");
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateProfile(String newName, String newAbout) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Update both fullName and userName for consistency
        database.child("user").child(currentUserId).child("fullName").setValue(newName);
        database.child("user").child(currentUserId).child("userName").setValue(newName);
        database.child("user").child(currentUserId).child("about").setValue(newAbout)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    }
                    // Update current user object and UI
                    currentUser.setFullName(newName);
                    currentUser.setUserName(newName);
                    currentUser.setAbout(newAbout);
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}